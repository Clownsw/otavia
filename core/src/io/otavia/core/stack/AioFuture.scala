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

package io.otavia.core.stack

trait AioFuture[V] extends Future[V] {

    override private[core] def promise: AioPromise[V] = this.asInstanceOf[AioPromise[V]]

    def onCompleted(exe: AioFuture[V] => Unit): Unit

}

object AioFuture {}

class AioPromise[V] extends Promise[V] with AioFuture[V] with Runnable {

    override def setSuccess(result: V): Promise[V] = ???

    override def setFailure(cause: Throwable): Promise[V] = ???

    override def future: AioFuture[V] = ???

    override def canTimeout: Boolean = ???

    override def setStack(s: Stack): Unit = ???

    override def actorStack: Stack = ???

    override def run(): Unit = ???

    override def recycle(): Unit = ???

    override protected def cleanInstance(): Unit = ???

    override def isSuccess: Boolean = ???

    override def isFailed: Boolean = ???

    override def isDone: Boolean = ???

    override def getNow: V = ???

    override def cause: Option[Throwable] = ???

    override def causeUnsafe: Throwable = ???

    override def onCompleted(exe: AioFuture[V] => Unit): Unit = ???

}