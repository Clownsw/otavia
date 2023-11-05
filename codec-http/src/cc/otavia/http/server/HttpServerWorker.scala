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

package cc.otavia.http.server

import cc.otavia.core.actor.AcceptedWorkerActor
import cc.otavia.core.actor.AcceptorActor.AcceptedChannel
import cc.otavia.core.cache.ActorThreadLocal
import cc.otavia.core.channel.{Channel, ChannelAddress}
import cc.otavia.core.stack.{AskStack, StackState}
import cc.otavia.handler.http.ServerCodec

class HttpServerWorker(routerMatcher: RouterMatcher, dates: ActorThreadLocal[Array[Byte]])
    extends AcceptedWorkerActor[Nothing] {

    final override protected def initChannel(channel: Channel): Unit =
        channel.pipeline.addLast(new ServerCodec(routerMatcher, dates))

    override def continueAsk(stack: AskStack[AcceptedChannel]): Option[StackState] = handleAccepted(stack)

    override protected def afterAccepted(channel: ChannelAddress): Unit = logger.debug("Accepted {}", channel)

}