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

import cc.otavia.core.actor.{AbstractActor, Actor}
import cc.otavia.core.cache.{AbstractThreadIsolatedObjectPool, ActorThreadLocal, Poolable, ThreadLocalTimer}
import cc.otavia.core.message.*
import cc.otavia.core.timer.Timer

import java.util.concurrent.TimeUnit
import scala.language.unsafeNulls

final class AskStack[A <: Ask[? <: Reply]] private () extends Stack {

    private var call: Call   = _
    private var reply: Reply = _

    private[core] def setAsk(c: Call): Unit = call = c

    def ask: A = call.asInstanceOf[A]

    def `return`(ret: ReplyOf[A]): None.type = {
        ret.setReplyId(ask.askId)
        reply = ret
        ask.sender.reply(reply, runtimeActor)
        None
    }

    def `throw`(cause: ExceptionMessage): None.type = {
        cause.setReplyId(ask.askId)
        reply = cause
        this.setFailed()
        ask.sender.reply(reply, runtimeActor)
        None
    }

    def `throw`(cause: Throwable): None.type = this.`throw`(ExceptionMessage(cause))

    def isDone: Boolean = reply != null

    override def recycle(): Unit = AskStack.stackPool.recycle(this)

    override protected def cleanInstance(): Unit = {
        call = null
        reply = null
        super.cleanInstance()
    }

    override def toString: String =
        s"AskStack(ask = $ask, state = $state, uncompleted = $uncompletedPromiseCount, " +
            s"completed = $completedPromiseCount, reply = ${Option(reply)})"

}

object AskStack {

    private val stackPool = new StackObjectPool[AskStack[? <: Ask[?]]] {
        override protected def newObject(): AskStack[? <: Ask[?]] = new AskStack[Nothing]()
    }

    private[core] def apply[A <: Ask[? <: Reply]](actor: AbstractActor[?]): AskStack[A] = {
        val stack = stackPool.get().asInstanceOf[AskStack[A]]
        stack.setRuntimeActor(actor)
        stack
    }

}
