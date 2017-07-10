/*
 * Copyright 2017 Carsten Hartenfels
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
import scala.util.Properties.envOrElse


object Main {
  def main(args: Array[String]): Unit = {
    val DefaultPort = 53115

    if (!args.isEmpty) {
      System.err.println(
        s"""
           |I don't take any arguments.
           |
           |Set the SEMSERV_PORT environment variable
           |if you want to use a different port than
           |the default port $DefaultPort.
           |
           |Set the SEMSERV_CACHE environment variable
           |to set the cache database file. This may be
           |a relative or absolute path, or ":memory:"
           |for an in-memory database.
           |""".stripMargin)
      System.exit(2)
    }

    semserv.Server(envOrElse("SEMSERV_PORT", s"$DefaultPort").toInt)
  }
}
