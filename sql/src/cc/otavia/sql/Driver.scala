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

package cc.otavia.sql

import cc.otavia.buffer.Buffer
import cc.otavia.core.actor.ChannelsActor
import cc.otavia.core.channel.ChannelHandlerContext
import cc.otavia.core.channel.handler.{Byte2MessageDecoder, Message2ByteEncoder}
import cc.otavia.core.message.{Ask, Reply}
import cc.otavia.core.stack.ChannelFuture

import java.net.SocketAddress

abstract class Driver(val options: ConnectOptions) extends Byte2MessageDecoder with Message2ByteEncoder {

    protected def checkDecodePacket(buffer: Buffer): Boolean

}
