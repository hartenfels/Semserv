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
