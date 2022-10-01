/*
 * Copyright 2022 yankun
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


package io.otavia.core.message

import io.otavia.core.Address

class MessageIdDistributor() {
  private var address: Address[?] = _
  private var aid: Long = _
  private var currentId: Long = Long.MinValue

  private[core] def setActorId(id: Long): Unit = aid = id

  private[core] def setActorAddress(address: Address[?]): Unit = this.address = address

  def sender: Address[?] = address

  def actorId: Long = aid

  def generate: Long = {
    val id = currentId
    currentId += 1
    id
  }
}
