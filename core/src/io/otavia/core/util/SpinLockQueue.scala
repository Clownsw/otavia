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

package io.otavia.core.util

import java.util.concurrent.atomic.AtomicInteger
import scala.collection.mutable
import scala.language.unsafeNulls

class SpinLockQueue[T <: Nextable] {

    private val readLock  = new SpinLock()
    private val writeLock = new SpinLock()
    private val size      = new AtomicInteger(0)

    @volatile private var head: T | Null = _
    @volatile private var tail: T | Null = _

    def isEmpty: Boolean = size.get() == 0

    def nonEmpty: Boolean = size.get() > 0

    def length: Int = size.get()

    def enqueue(instance: T): Unit = {
        writeLock.lock()
        if (size.get() == 0) {
            readLock.lock()
            head = instance
            tail = instance
            size.incrementAndGet()
            writeLock.unlock()
            readLock.unlock()
        } else {
            tail.next = instance
            tail = instance
            size.incrementAndGet()
            writeLock.unlock()
        }
    }

    final def dequeue(): T | Null = {
        if (size.get() == 0) null
        else dequeue0()
    }

    final private def dequeue0(): T | Null = {
        if (size.get() == 0) {
            null
        } else if (size.get() == 1) {
            writeLock.lock()
            readLock.lock()
            if (size.get() == 1) {
                val value = head
                head = null
                tail = null
                size.decrementAndGet()
                writeLock.unlock()
                readLock.unlock()
                value
            } else { // size.get() > 1
                writeLock.unlock()
                dequeue00()
            }
        } else {
            readLock.lock()
            dequeue00()
        }
    }

    final private inline def dequeue00(): T = {
        val value = head
        head = value.next.asInstanceOf[T]
        size.decrementAndGet()
        readLock.unlock()
        value.dechain()
        value.asInstanceOf[T]
    }

}