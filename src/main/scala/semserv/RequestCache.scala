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
