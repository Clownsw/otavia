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

package cc.otavia.core.stack

import cc.otavia.core.message.Reply
import cc.otavia.core.message.{ExceptionMessage, Reply, UnitReply}
import cc.otavia.core.timer.Timer

import scala.language.unsafeNulls

/** User interface for class [[ReplyPromise]]
 *  @tparam R
 *    type of [[Reply]]
 */
sealed trait ReplyFuture[+R <: Reply] extends Future[R] {
    private[core] override def promise: ReplyPromise[Reply] = this.asInstanceOf[ReplyPromise[Reply]]

}

object ReplyFuture {
    def apply[R <: Reply](): ReplyFuture[R] = ReplyPromise()
}

private[core] class ReplyPromise[R <: Reply] private () extends TimeoutablePromise[Reply] with ReplyFuture[R] {

    private var reply: Reply         = _
    private var throwable: Throwable = _

    override def future: ReplyFuture[R] = this

    override def isSuccess: Boolean = if (reply != null) true else false

    override def isFailed: Boolean = if (throwable != null) true else false

    override def isDone: Boolean = (reply != null) || (throwable != null)

    override def getNow: R = if (reply == null && throwable == null)
        throw new IllegalStateException("not completed yet")
    else if (throwable != null) throw throwable
    else reply.asInstanceOf[R]

    override def cause: Option[Throwable] = if (reply == null && throwable == null)
        throw new IllegalStateException("not completed yet")
    else Option(throwable)

    override def causeUnsafe: Throwable = if (reply == null && throwable == null)
        throw new IllegalStateException("not completed yet")
    else if (reply != null) throw new IllegalStateException("the future is success")
    else throwable

    override def recycle(): Unit = ReplyPromise.promiseObjectPool.recycle(this)

    override protected def cleanInstance(): Unit = {
        reply = null
        throwable = null
        super.cleanInstance()
    }

    override def setSuccess(result: Reply): Promise[Reply] = {
        reply = result
        this
    }

    override def setFailure(cause: Throwable): Promise[Reply] = {
        throwable = cause
        this
    }

}

object ReplyPromise {

    private val promiseObjectPool = new PromiseObjectPool[ReplyPromise[?]] {
        override protected def newObject(): ReplyPromise[?] = new ReplyPromise[Nothing]()
    }

    def apply[R <: Reply](): ReplyPromise[R] = promiseObjectPool.get().asInstanceOf[ReplyPromise[R]]

}
