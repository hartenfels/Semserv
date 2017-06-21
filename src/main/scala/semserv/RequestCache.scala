package semserv

import java.sql.{Connection, DriverManager, PreparedStatement}
import scala.util.Properties.envOrElse


object RequestCache {
  def apply(db: Option[String] = None):RequestCache =
    new RequestCache(db.getOrElse(envOrElse("SEMSERV_CACHE", "cache.db")))
}

class RequestCache(db: String) {
  val conn = DriverManager.getConnection(s"jdbc:sqlite:$db")

  conn.createStatement().execute("""
      CREATE TABLE IF NOT EXISTS cache (
        req TEXT PRIMARY KEY NOT NULL,
        res TEXT NOT NULL
      )
    """)


  def get(req: String): Option[String] = {
    val stmt = conn.prepareStatement("SELECT res FROM cache WHERE req = ?")
    stmt.setString(1, req)
    val rs = stmt.executeQuery()
    if (rs.next()) Some(rs.getString("res")) else None
  }


  def set(req: String, res: String): String = {
    val stmt = conn.prepareStatement(
      "INSERT OR REPLACE INTO cache (req, res) VALUES (?, ?)")
    stmt.setString(1, req)
    stmt.setString(2, res)
    stmt.executeUpdate()
    res
  }
}
