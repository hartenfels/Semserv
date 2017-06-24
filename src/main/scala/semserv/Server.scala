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
