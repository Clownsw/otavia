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

import mill._, scalalib._, publish._
import $ivy.`io.github.otavia-projects::mill-rust_mill$MILL_BIN_PLATFORM:0.1.1-SNAPSHOT`
import $ivy.`com.lihaoyi::mill-contrib-buildinfo:$MILL_VERSION`
import mill.contrib.buildinfo.BuildInfo
import io.github.otavia.jni.plugin.RustJniModule

object ProjectInfo {

    def description: String     = "A super fast IO & Actor programming model!"
    def organization: String    = "io.github.otavia-projects"
    def organizationUrl: String = "https://github.com/otavia-projects"
    def github                  = VersionControl.github("otavia-projects", "otavia")
    def repository              = github.browsableRepository.get
    def licenses                = Seq(License.`Apache-2.0`)
    def author                  = Seq("Yan Kun <yan_kun_1992@foxmail.com>")

}

trait OtaviaModule extends ScalaModule with PublishModule {

    override def scalaVersion = "3.2.2"

    override def publishVersion: T[String] = "0.1.0-SNAPSHOT"

    override def pomSettings: T[PomSettings] = PomSettings(
      description = ProjectInfo.description,
      organization = ProjectInfo.organization,
      url = ProjectInfo.repository,
      licenses = ProjectInfo.licenses,
      versionControl = ProjectInfo.github,
      developers = Seq(
        Developer(
          "yankun1992",
          "Yan Kun",
          "https://github.com/yankun1992",
          Some("otavia-projects"),
          Some("https://github.com/otavia-projects")
        )
      )
    )

    override def scalacOptions = T { scala.Seq("-Yexplicit-nulls") }

}

object core extends OtaviaModule with BuildInfo {

    override def ivyDeps = Agg(
      ivy"io.netty:netty5-buffer:5.0.0.Alpha5",
      ivy"org.log4s::log4s::1.10.0"
    )

    override def buildInfoMembers = Map(
      "scalaVersion"    -> scalaVersion(),
      "publishVersion"  -> publishVersion(),
      "name"            -> "otavia",
      "description"     -> ProjectInfo.description,
      "organization"    -> ProjectInfo.organization,
      "organizationUrl" -> ProjectInfo.organizationUrl,
      "github"          -> ProjectInfo.repository,
      "licenses" -> {
          if (ProjectInfo.licenses.length == 1) ProjectInfo.licenses.head.name
          else ProjectInfo.licenses.map(_.name).mkString("[", ", ", "]")
      },
      "author" -> {
          if (ProjectInfo.author.length == 1) ProjectInfo.author.head else ProjectInfo.author.mkString("[", ", ", "]")
      }
    )

    override def buildInfoPackageName: Option[String] = Some("io.otavia")

}

object handler extends OtaviaModule {
    override def moduleDeps: Seq[PublishModule] = scala.Seq(core, codec)
}

object codec extends OtaviaModule {
    override def moduleDeps: Seq[PublishModule] = scala.Seq(core)
}

object http extends OtaviaModule {
    override def moduleDeps: Seq[PublishModule] = scala.Seq(core)

}

object adbc extends OtaviaModule {
    override def moduleDeps: Seq[PublishModule] = scala.Seq(core)
}

object redis extends OtaviaModule {

    override def artifactName: T[String]        = "redis-client"
    override def moduleDeps: Seq[PublishModule] = scala.Seq(core)

}

object mio extends OtaviaModule {
    override def moduleDeps: Seq[PublishModule] = scala.Seq(core)
}

object mionative extends RustJniModule {
    override def release: Boolean = false
}

object web extends OtaviaModule {
    override def moduleDeps: Seq[PublishModule] = scala.Seq(core, http, adbc, redis)
}

object examples extends OtaviaModule {
    override def moduleDeps: Seq[PublishModule] = scala.Seq(web)
}
