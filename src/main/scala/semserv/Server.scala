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
package semserv

import java.io.{BufferedReader, BufferedWriter, InputStreamReader, OutputStreamWriter}
import java.net.{Socket, ServerSocket}
import java.nio.charset.StandardCharsets.UTF_8;


object Server {
  def apply(port: Int): Unit = {
    new Server(port).listen()
  }
}


class Server(port: Int) {
  private val server = new ServerSocket(port)

  def listen(): Unit = {
    println("# listen " + server.getLocalPort)
    while (true) {
      val socket = server.accept()
      println("# connect " + socket.getInetAddress)
      new ServerThread(socket).start()
    }
  }
}


class ServerThread(private val socket: Socket) extends Thread {
  private val output = new BufferedWriter(
      new OutputStreamWriter(socket.getOutputStream, UTF_8))

  private val input = new BufferedReader(
      new InputStreamReader(socket.getInputStream, UTF_8))

  override def run(): Unit =
    try read() finally {
      socket.close()
      println("# disconnect " + socket.getInetAddress)
    }

  private def read(): Unit = {
    var line = ""
    while ({ line = input.readLine(); line != null }) {
      output.write(interpret(line))
      output.flush()
    }
  }
}
