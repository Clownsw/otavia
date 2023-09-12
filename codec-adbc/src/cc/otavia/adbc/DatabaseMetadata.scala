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

package cc.otavia.adbc

/** Contains static metadata about the backend database server */
trait DatabaseMetadata {

    /** @return The product name of the backend database server */
    def productName: String

    /** @return
     *    The full version string for the backend database server. This may be useful for for parsing more subtle
     *    aspects of the version string. For simple information like database major and minor version, use
     *    [[majorVersion]] and [[minorVersion]] instead.
     */
    def fullVersion: String

    /** @return The major version of the backend database server */
    def majorVersion: Int

    /** @return The minor version of the backend database server */
    def minorVersion: Int

}
