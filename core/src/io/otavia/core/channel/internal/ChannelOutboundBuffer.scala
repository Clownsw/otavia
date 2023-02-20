/*
 * Copyright 2022 Yan Kun <yan_kun_1992@foxmail.com>
 *
 * This file fork from netty.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.otavia.core.channel.internal

import io.netty5.util.concurrent.{EventExecutor, Promise}
import io.netty5.util.internal.logging.{InternalLogger, InternalLoggerFactory}
import io.netty5.util.internal.{ObjectPool, PromiseNotificationUtil, SilentDispose, SystemPropertyUtil}
import io.otavia.core.actor.ChannelsActor
import io.otavia.core.cache.{PerActorThreadObjectPool, Poolable}
import io.otavia.core.channel.ChannelOutboundBuffer
import io.otavia.core.channel.internal.ChannelOutboundBuffer.MessageEntry

import java.util.Objects.requireNonNull
import java.util.function.Predicate

object ChannelOutboundBuffer {

    // Assuming a 64-bit JVM:
    //  - 16 bytes object header
    //  - 6 reference fields
    //  - 2 long fields
    //  - 2 int fields
    //  - 1 boolean field
    //  - padding
    private[channel] val CHANNEL_OUTBOUND_BUFFER_ENTRY_OVERHEAD =
        SystemPropertyUtil.getInt("io.netty5.transport.outboundBufferEntrySizeOverhead", 96)
    private val logger = InternalLoggerFactory.getInstance(classOf[ChannelOutboundBuffer])

    private def safeSuccess(promise: Promise[Void]): Unit = {
        PromiseNotificationUtil.trySuccess(promise, null, logger)
    }

    private def safeFail(promise: Promise[Void], cause: Throwable): Unit = {
        PromiseNotificationUtil.tryFailure(promise, cause, logger)
    }

    private object MessageEntry {

        private val recycler = new PerActorThreadObjectPool[MessageEntry] {
            override protected def newObject(): MessageEntry = new MessageEntry()
        }

        def apply(message: AnyRef | Null, pendingSize: Int, cancelled: Boolean = false): MessageEntry = {
            val entry = recycler.get()
            entry.message = message
            entry.pendingSize = pendingSize + CHANNEL_OUTBOUND_BUFFER_ENTRY_OVERHEAD
            entry.cancelled = cancelled
            entry
        }

    }

    private final class MessageEntry(
        var message: AnyRef | Null = null,
        var pendingSize: Int = 0,
        var cancelled: Boolean = false
    ) extends Poolable {

        def cancel(): Int = if (!cancelled) {
            cancelled = true
            val size = pendingSize
            SilentDispose.dispose(message, logger)
            message = null
            pendingSize = 0
            size
        } else 0

        override protected def cleanInstance(): Unit = {
            message = null
            pendingSize = 0
            cancelled = false
        }

        override def recycle(): Unit = this.recycle(MessageEntry.recycler)

        def recycleAndGetNext(): MessageEntry | Null = {
            val n = this.next
            recycle(MessageEntry.recycler)
            n
        }

    }

}

@SuppressWarnings(Array("UnusedDeclaration"))
private final class ChannelOutboundBuffer() {

    // MessageEntry(flushedEntry) --> ... MessageEntry(unflushedEntry) --> ... MessageEntry(tailEntry)
    //
    // The MessageEntry that is the first in the linked-list structure that was flushed
    private var flushedEntry: MessageEntry | Null = null
    // The MessageEntry which is the first unflushed in the linked-list structure
    private var unflushedEntry: MessageEntry | Null = null
    // The MessageEntry which represents the tail of the buffer
    private var tailEntry: MessageEntry | Null = null
    // The number of flushed entries that are not written yet
    private var flushed = 0
    private var inFail  = false
    private var closed  = false
    // Its single-writer, single-reader
    private var totalPendingSize = 0L

    private def incrementPendingOutboundBytes(size: Long): Unit = totalPendingSize += size

    private def decrementPendingOutboundBytes(size: Long): Unit = totalPendingSize -= size

    /** Add given message to this[[ChannelOutboundBuffer]]. */
    private[channel] def addMessage(msg: AnyRef, size: Int): Unit = {
        if (closed) throw new IllegalStateException("ChannelOutboundBuffer has been closed!")

        val entry = MessageEntry(msg, size)
        tailEntry match
            case null: Null         => flushedEntry = null
            case tail: MessageEntry => tail.next = entry
        tailEntry = entry

        if (unflushedEntry == null) unflushedEntry = entry
        // increment pending bytes after adding message to the unflushed arrays.
        // See https://github.com/netty/netty/issues/1619
        incrementPendingOutboundBytes(entry.pendingSize)
    }

    /** Add a flush to this [[ChannelOutboundBuffer]]. This means all previous added messages are marked as flushed and
     *  so you will be able to handle them.
     */
    private[channel] def addFlush(): Unit = {

        // There is no need to process all entries if there was already a flush before and no new messages
        // where added in the meantime.
        //
        // See https://github.com/netty/netty/issues/2577
        unflushedEntry match
            case null: Null =>
            case entry: MessageEntry =>
                if (flushedEntry == null) {
                    // there is no flushedEntry yet, so start with the entry
                    flushedEntry = entry
                }
                var prev: MessageEntry | Null = null
                while {
                    // Was cancelled so make sure we free up memory, unlink and notify about the freed bytes
                    val pending = entry.cancel()
                    prev match
                        case null: Null =>
                            // It's the first entry, drop it
                            flushedEntry = entry.next
                        case prevEntry: MessageEntry =>
                            // Remove te entry from the linked list.
                            prevEntry.next = entry.next

                    val next = entry.next
                    entry.recycle()

                } do ()

        var entry = unflushedEntry
        if (entry != null) {
            if (flushedEntry == null) {
                // there is no flushedEntry yet, so start with the entry
                flushedEntry = entry
            }
            var prev: MessageEntry = null
            while {
                // Was cancelled so make sure we free up memory, unlink and notify about the freed bytes
                val pending = entry.cancel
                if (prev == null) {
                    // It's the first entry, drop it
                    flushedEntry = entry.next
                } else {
                    // Remove te entry from the linked list.
                    prev.next = entry.next
                }
                val next = entry.next
                entry.recycle()
                entry = next
                decrementPendingOutboundBytes(pending)
                entry != null
            } do ()
            // All flushed so reset unflushedEntry
            unflushedEntry = null
        }
    }

    /** Return the current message to write or null if nothing was flushed before and so is ready to be written. */
    private[channel] def current: AnyRef = {

        val entry = flushedEntry
        if (entry == null) return null
        entry.msg
    }

    /** Will remove the current message, mark its {@link Promise} as success and return {@code true}. If no flushed
     *  message exists at the time this method is called it will return {@code false} to signal that no more messages
     *  are ready to be handled.
     */
    private[channel] def remove = remove0(null)

    /** Will remove the current message, mark its {@link Promise} as failure using the given {@link Throwable} and
     *  return {@code true}. If no flushed message exists at the time this method is called it will return {@code false}
     *  to signal that no more messages are ready to be handled.
     */
    private[channel] def remove(cause: Throwable) = remove0(requireNonNull(cause, "cause").nn)

    private def remove0(cause: Throwable | Null): Boolean = {

        val e = flushedEntry
        if (e == null) return false
        val msg = e.msg
//    val promise = e.promise
        val size = e.pendingSize
        removeEntry(e)
        if (!e.cancelled) {
            // only release message, fail and decrement if it was not canceled before.
            SilentDispose.trySilentDispose(msg, ChannelOutboundBuffer.logger)
//      if (cause == null) ChannelOutboundBuffer.safeSuccess(promise)
//      else ChannelOutboundBuffer.safeFail(promise, cause)
            decrementPendingOutboundBytes(size)
        }
        // recycle the entry
        e.recycle()
        true
    }

    private def removeEntry(e: ChannelOutboundBuffer.Entry): Unit = {

        if ({
            flushed -= 1; flushed
        } == 0) {
            // processed everything
            flushedEntry = null
            if (e eq tailEntry) {
                tailEntry = null
                unflushedEntry = null
            }
        } else flushedEntry = e.next
    }

    /** Returns the number of flushed messages in this {@link ChannelOutboundBuffer}. */
    private[channel] def size = {

        flushed
    }

    /** Returns {@code true} if there are flushed messages in this {@link ChannelOutboundBuffer} or {@code false}
     *  otherwise.
     */
    private[channel] def isEmpty = {

        flushed == 0
    }

    private[channel] def failFlushedAndClose(failCause: Throwable, closeCause: Throwable): Unit = {

        failFlushed(failCause)
        close(closeCause)
    }

    private[channel] def failFlushed(cause: Throwable): Unit = {

        // Make sure that this method does not reenter.  A listener added to the current promise can be notified by the
        // current thread in the tryFailure() call of the loop below, and the listener can trigger another fail() call
        // indirectly (usually by closing the channel.)
        //
        // See https://github.com/netty/netty/issues/1501
        if (inFail) return try {
            inFail = true
            while (!isEmpty) remove(cause)
        } finally inFail = false
    }

    private def close(cause: Throwable): Unit = {

        if (inFail) {
//      executor.execute(() => close(cause))
            return
        }
        inFail = true
        if (!isEmpty) throw new IllegalStateException("close() must be invoked after all flushed writes are handled.")
        // Release all unflushed messages.
        try {
            var e = unflushedEntry
            while (e != null) {
                val size = e.pendingSize
                decrementPendingOutboundBytes(size)
                if (!e.cancelled) {
                    SilentDispose.dispose(e.msg, ChannelOutboundBuffer.logger)
//          ChannelOutboundBuffer.safeFail(e.promise, cause)
                }
                e = e.recycleAndGetNext
            }
        } finally {
            closed = true
            inFail = false
        }
    }

    private[channel] def totalPendingWriteBytes = totalPendingSize

    /** Call {@link Function# apply ( Object )} for each flushed message in this {@link ChannelOutboundBuffer} until
     *  {@link Function# apply ( Object )} returns {@link Boolean# FALSE} or there are no more flushed messages to
     *  process.
     */
    private[channel] def forEachFlushedMessage(processor: AnyRef => Boolean): Unit = {

        var entry = flushedEntry
        if (flushedEntry != null) {
            ???
        }
    }

    private def isFlushedEntry(e: ChannelOutboundBuffer.MessageEntry) = e != null && (e ne unflushedEntry)

}