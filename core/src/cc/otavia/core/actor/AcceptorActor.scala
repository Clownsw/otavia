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

package cc.otavia.core.actor

import cc.otavia.core.actor.AcceptorActor.*
import cc.otavia.core.actor.ChannelsActor.{Bind, BindReply, RegisterWaitState}
import cc.otavia.core.address.Address
import cc.otavia.core.channel.*
import cc.otavia.core.message.ReactorEvent
import cc.otavia.core.stack.*
//import cc.otavia.core.channel.impl.NioServerSocketChannel
import cc.otavia.core.message.*
import cc.otavia.core.stack.{ChannelFrame, StackState}

import java.net.{InetAddress, InetSocketAddress, SocketAddress}
import scala.runtime.Nothing$

abstract class AcceptorActor[W <: AcceptedWorkerActor[? <: Call]] extends ChannelsActor[Bind] {

    private var workers: Address[MessageOf[W]] = _

    /** Number of worker. */
    protected def workerNumber: Int = 1

    protected def workerFactory: WorkerFactory[W]

    override def afterMount(): Unit = {
        workers = system.buildActor(workerFactory, workerNumber)
    }

    override def init(channel: Channel): Unit = {
        if (handler.nonEmpty) {
            channel.pipeline.addLast(handler.get)
        }
    }

    final override protected def newChannel(): Channel = system.channelFactory.openServerSocketChannel(family)

    final protected def bind(stack: AskStack[Bind]): Option[StackState] = {
        stack.stackState match
            case StackState.start =>
                val channel = newChannelAndInit()
                val state   = new BindState()
                channel.bind(stack.ask.local, state.bindFuture)
                state.suspend()
            case bindState: BindState =>
                if (bindState.bindFuture.isSuccess) {
                    val channel = bindState.bindFuture.channel
                    channels.put(channel.id, channel)
                    afterBind(bindState.bindFuture.channel)
                    stack.`return`(BindReply(channel.id))
                } else {
                    stack.`throw`(ExceptionMessage(bindState.bindFuture.causeUnsafe))
                }
    }

    protected def afterBind(channel: ChannelAddress): Unit = {
        // default do nothing
    }

    override def continueAsk(stack: AskStack[Bind]): Option[StackState] = bind(stack)

    override def continueChannel(stack: ChannelStack[AnyRef]): Option[StackState] = {
        stack match
            case _: ChannelStack[?] if stack.message.isInstanceOf[Channel] =>
                handleAcceptedStack(stack.asInstanceOf[ChannelStack[Channel]])
    }

    private def handleAcceptedStack(stack: ChannelStack[Channel]): Option[StackState] = {
        stack.stackState match
            case StackState.start =>
                val state = new DispatchState()
                workers.ask(AcceptedChannel(stack.message), state.dispatchFuture)
                state.suspend()
            case state: DispatchState =>
                if (state.dispatchFuture.isSuccess) stack.`return`(None)
                else
                    stack.`return`(state.dispatchFuture.causeUnsafe)
    }

}

object AcceptorActor {

    trait WorkerFactory[W <: AcceptedWorkerActor[? <: Call]] extends ActorFactory[W] {
        override def newActor(): W
    }

    final case class AcceptedChannel(channel: ChannelAddress) extends Ask[UnitReply]

    private final class DispatchState extends StackState {

        val dispatchFuture: ReplyFuture[UnitReply] = ReplyFuture[UnitReply]()

        override def resumable(): Boolean = dispatchFuture.isDone

    }

    private final class BindState extends StackState {
        val bindFuture: ChannelFuture = ChannelFuture()
    }

}