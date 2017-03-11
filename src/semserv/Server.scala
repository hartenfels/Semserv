package semserv

import java.io.{BufferedReader, InputStreamReader, PrintWriter}
import java.net.{Socket, ServerSocket}


object Server {
  def apply(port: Int): Unit = {
    new Server(port).listen()
  }
}


class Server(port: Int) {
  private val server = new ServerSocket(port)

  def listen(): Unit = {
    println("# listen " + port)
    while (true) {
      val socket = server.accept()
      println("# connect " + socket.getInetAddress)
      new ServerThread(socket).start()
    }
  }
}


class ServerThread(private val socket: Socket) extends Thread {
  private val output = new PrintWriter(socket.getOutputStream)
  private val input  =
      new BufferedReader(new InputStreamReader(socket.getInputStream))

  override def run(): Unit =
    try read() finally {
      socket.close()
      println("# disconnect " + socket.getInetAddress)
    }

  private def read(): Unit = {
    var line = ""
    while ({ line = input.readLine(); line != null }) {
      output.print(interpret(line))
      output.flush()
    }
  }
}
