/*
 * Copyright 2022 Yan Kun <yan_kun_1992@foxmail.com>
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

package io.otavia.core.cache

import io.otavia.core.system.ActorThread

/** A special variant of [[ThreadLocal]] that yields higher access performance when accessed from a [[ActorThread]].
 *
 *  Internally, a [[ThreadLocal]] uses a constant index in an array, instead of using hash code and hash table, to look
 *  for a variable. Although seemingly very subtle, it yields slight performance advantage over using a hash table, and
 *  it is useful when accessed frequently.
 *
 *  To take advantage of this thread-local variable, your thread must be a [[ActorThread]]. By default, all actors and
 *  channel are running by [[ActorThread]].
 *  @see
 *    [[java.lang.ThreadLocal]]
 *  @tparam V
 *    the type of the thread-local variable
 */
abstract class ThreadLocal[V] {

    @volatile private var inited: Boolean = false

    private final def initIfNotInited(len: Int): Unit =
        if (inited) {} else syncInit(len) // Reducing cpu branch prediction errors.

    private[cache] final def threadIndex(): Int = {
        val thread = ActorThread.currentThread()
        initIfNotInited(thread.parent.size)
        thread.index
    }

    private def syncInit(len: Int): Unit = this.synchronized {
        if (!inited) {
            doInit(len)
            inited = true
        }
    }

    private[cache] def doInit(len: Int): Unit

    /** Returns true if the thread local variable has been inited, otherwise false. */
    final def isInited: Boolean = inited

    /** Returns the initial value for this thread-local variable. */
    protected def initialValue(): V

    /** Returns the current value for the current thread */
    def get(): V

    /** Returns the current value for the current thread if it exists, null otherwise. */
    def getIfExists: V | Null

    /** Set the value for the current thread. */
    def set(v: V): Unit

    /** Returns true if and only if this thread-local variable is set. */
    def isSet: Boolean

    /** Sets the value to uninitialized for the specified thread local map. After this, any subsequent call to get()
     *  will trigger a new call to initialValue().
     */
    def remove(): Unit

    /** Invoked when this thread local variable is removed by remove(). Be aware that remove() is not guaranteed to be
     *  called when the `Thread` completes which means you can not depend on this for cleanup of the resources in the
     *  case of `Thread` completion.
     */
    protected def onRemoval(value: V): Unit = {}

}

object ThreadLocal {

    val EMPTY: Array[AnyRef] = Array.empty

}