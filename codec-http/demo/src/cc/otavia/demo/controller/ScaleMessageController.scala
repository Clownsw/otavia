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

package cc.otavia.demo.controller

import cc.otavia.buffer.Buffer
import cc.otavia.core.actor.StateActor
import cc.otavia.core.message.{Ask, Reply}
import cc.otavia.core.stack.helper.{ChannelFutureState, FutureState, StartState}
import cc.otavia.core.stack.{AskStack, StackState}
import cc.otavia.demo.controller.ScaleMessageController.*
import cc.otavia.http.*
import cc.otavia.http.server.{HttpRequest, HttpRequestFactory, HttpResponseSerde}
import cc.otavia.json.{JsonConstants, JsonSerde}

import java.nio.charset.StandardCharsets
import scala.collection.mutable

class ScaleMessageController extends StateActor[ScaleMessageRequest] {

    override protected def resumeAsk(stack: AskStack[ScaleMessageRequest]): Option[StackState] = {
        val request      = stack.ask
        val scaleLength  = request.params.get("length").map(_.toInt).getOrElse(1)
        val scaleMessage = request.content.get
        if (scaleLength == 1) stack.`return`(scaleMessage)
        else stack.`return`(ScaleMessage(scaleMessage.name * scaleLength, scaleMessage.age * scaleLength))
    }

}

object ScaleMessageController {

    case class ScaleMessage(name: String, age: Int) extends Reply
    class ScaleMessageRequest                       extends HttpRequest[ScaleMessage, ScaleMessage]

    //// All below codes can be auto generated by macro

    private val messageSerde = new JsonSerde[ScaleMessage] {

        private val name: Array[Byte] = "\"name\"".getBytes(StandardCharsets.US_ASCII)
        private val age: Array[Byte]  = "\"age\"".getBytes(StandardCharsets.US_ASCII)

        override def serialize(value: ScaleMessage, out: Buffer): Unit = {
            serializeObjectStart(out)
            serializeKey("name", out)
            serializeString(value.name, out)
            out.writeByte(JsonConstants.TOKEN_COMMA)
            serializeKey("age", out)
            serializeInt(value.age, out)
            serializeObjectEnd(out)
        }
        override def deserialize(in: Buffer): ScaleMessage = {
            var n: String = null
            var a: Int    = 0
            skipObjectStart(in)
            skipBlanks(in)
            while (!in.nextIs(JsonConstants.TOKEN_OBJECT_END)) {
                if (in.skipIfNextAre(name)) {
                    skipBlanks(in)
                    in.skipIfNextIs(JsonConstants.TOKEN_COLON)
                    n = deserializeString(in)
                } else if (in.skipIfNextAre(age)) {
                    skipBlanks(in)
                    in.skipIfNextIs(JsonConstants.TOKEN_COLON)
                    a = deserializeInt(in)
                }
                skipBlanks(in)
                in.skipIfNextIs(JsonConstants.TOKEN_COMMA)
            }
            skipObjectEnd(in)

            ScaleMessage(n, a)
        }

    }

    val messageRequestFactory: HttpRequestFactory = new HttpRequestFactory(Some(messageSerde)) {
        override def createHttpRequest(): HttpRequest[?, ?] = new ScaleMessageRequest()
    }

    val messageResponseSerde = new HttpResponseSerde[ScaleMessage](messageSerde, MediaType.APP_JSON)

}
