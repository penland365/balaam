package codes.penland365
package balaam.db

import codes.penland365.balaam.Main
import com.twitter.finagle.Service
import com.twitter.util.Future
import roc.postgresql.{Request, Result}

private[db] object Postgresql {

  lazy val client = roc.Postgresql.client
    .withUserAndPasswd(Main.DatabaseUser(), Main.DatabasePasswd())
    .withDatabase(Main.Database())
    .newRichClient(s"${Main.DatabaseHost()}${Main.DatabasePort()}")

  trait Decoder[A] {
    def decode(result: Result, exceptionMessage: String): Future[A]
  }

  class Select[A : Decoder](private var errMsg: String = "")(implicit d: Decoder[A])
    extends Service[Request, A] {
    override def apply(request: Request): Future[A]  = for {
      result <- client.query(request)
      a      <- d.decode(result, errMsg)
    } yield a

    def errorMessage_=(newMessage: String): Unit = errMsg = newMessage
  }
}

