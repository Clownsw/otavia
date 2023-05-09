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

package io.otavia.core.cache

import io.otavia.core.message.TimeoutEvent
import io.otavia.core.timer.TimeoutTrigger

private[core] trait TimeoutResource {

    /** Initial [[TimeoutTrigger]] when initial a time-out resource like [[ThreadLocal]] */
    protected def initialTimeoutTrigger: Option[TimeoutTrigger] = None

    /** Handle [[TimeoutEvent]] for this [[TimeoutResource]]
     *
     *  @param registerId
     *    timer task register id in [[Timer]].
     *  @param threadLocalTimer
     *    current time-out [[ResourceTimer]]
     */
    def handleTimeout(registerId: Long, resourceTimer: ResourceTimer): Unit = {
        // default do nothing.
    }

}
