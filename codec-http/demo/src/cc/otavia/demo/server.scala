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

package cc.otavia.demo

import cc.otavia.buffer.Buffer
import cc.otavia.core.actor.ChannelsActor.{Bind, BindReply}
import cc.otavia.core.actor.MainActor
import cc.otavia.core.slf4a.LoggerFactory
import cc.otavia.core.stack.helper.FutureState
import cc.otavia.core.stack.{NoticeStack, StackState}
import cc.otavia.core.system.ActorSystem
import cc.otavia.demo.controller.ScaleMessageController
import cc.otavia.demo.controller.ScaleMessageController.*
import cc.otavia.http.HttpMethod.*
import cc.otavia.http.MediaType.*
import cc.otavia.http.server.Router.*
import cc.otavia.http.server.{HttpServer, Router}
import cc.otavia.json.JsonSerde
import cc.otavia.serde.helper.BytesSerde

import java.nio.charset.StandardCharsets.*
import java.nio.file.Path

private class ServerMain(val port: Int = 80) extends MainActor(Array.empty) {

    import ServerMain.*

    override def main0(stack: NoticeStack[MainActor.Args]): Option[StackState] = stack.state match
        case StackState.start =>
            val controller = system.buildActor(() => ScaleMessageController(), system.actorWorkerSize)
            val routers = Seq(
              constant[Array[Byte]](GET, "/plaintext", "Hello, World!".getBytes(UTF_8), BytesSerde, TEXT_PLAIN_UTF8),
              constant[HelloMessage](GET, "/json", HelloMessage("Hello, World!"), helloSerde, APP_JSON),
              get("/scale_message/{length}", controller, messageRequestSerde, messageResponseSerde)
            )
            val server = system.buildActor(() => new HttpServer(system.actorWorkerSize, routers))
            val state  = FutureState[BindReply]()
            server.ask(Bind(port), state.future)
            state.suspend()
        case state: FutureState[BindReply] =>
            if (state.future.isFailed) state.future.causeUnsafe.printStackTrace()
            logger.info(s"http server bind port $port success")
            stack.`return`()

}

object ServerMain {

    private case class HelloMessage(message: String)

    private val helloSerde = new JsonSerde[HelloMessage] { // this object can be derived by macro

        override def deserialize(in: Buffer): HelloMessage = throw new UnsupportedOperationException("no need")
        override def serialize(value: HelloMessage, out: Buffer): Unit = {
            this.serializeObjectStart(out)
            this.serializeKey("message", out)
            this.serializeString(value.message, out)
            this.serializeObjectEnd(out)
        }

    }

}

@main def server(): Unit =
    val system = ActorSystem()
    val logger = LoggerFactory.getLogger("server", system)
    logger.info("starting http server")
    system.buildActor(() => new ServerMain())
