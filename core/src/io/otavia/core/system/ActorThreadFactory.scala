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

package io.otavia.core.system

import java.util.concurrent.ThreadFactory

abstract class ActorThreadFactory(val system: ActorSystem) {
    def newThread(): ActorThread

}

object ActorThreadFactory {
    class DefaultActorThreadFactory(system: ActorSystem) extends ActorThreadFactory(system) {

        override def newThread(): ActorThread = {
            val thread = new ActorThread(system)
            if (thread.isDaemon) thread.setDaemon(false)
            if (thread.getPriority != Thread.NORM_PRIORITY) thread.setPriority(Thread.NORM_PRIORITY)
            thread
        }

    }
}
