/*
 * Copyright 2018 HM Revenue & Customs
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

package sdil.uniform

import cats.implicits._
import play.api.libs.json._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.ShortLivedHttpCaching

import scala.collection.mutable.{Map => MMap}
import scala.concurrent._
import scala.pickling._
import scala.pickling.json._

/** WebMonads read in all their data at the start of the interaction with the
  * user and write it all out again at the end.
  * A major limitation at the moment is that this has to be Json values. This
  * was a bad design decision as if I'd used plain serialisation or pickling
  * then the programmer building the journey wouldn't have needed to worry about
  * writing Format converters.
  * The store should be tolerant of failure - the structure of objects is likely
  * to change during development and it's usually fine to just ask the user again.
  */
trait Persistence {
  lazy val log = play.api.Logger(this.getClass)
  def dataGet(session: String): Future[Map[String, Pickle]]
  def dataPut(session: String, dataIn: Map[String, Pickle]): Future[Unit]
}

/** A non-presistent persistence engine - fast to develop in (store gets purged on
  * recompile), but guaranteed to bring shame and ruin should you actually dare
  * to use this in production. Basically a thin wrapper around a mutable map.
  */
class JunkPersistence(implicit val ec: ExecutionContext) extends Persistence {

  private val data = MMap.empty[String,Map[String,Pickle]]

  def dataGet(session: String): Future[Map[String, Pickle]] = {

    data.getOrElse(session, Map.empty[String,Pickle])
  }.pure[Future]

  def dataPut(session: String, dataIn: Map[String, Pickle]): Future[Unit] =
  {data(session) = dataIn}.pure[Future]
}

case class SessionCachePersistence(
  journeyName: String,
  keystore: uk.gov.hmrc.http.cache.client.SessionCache
)(implicit
    ec: ExecutionContext,
  hc: HeaderCarrier
) extends Persistence {
  def dataGet(session: String): Future[Map[String, Pickle]] = {
    val decoded = keystore.fetchAndGetEntry[Map[String, String]](journeyName).map{
      _.getOrElse(Map.empty).mapValues{JSONPickle}
    }
    log.info(s"Decoded: $decoded")
    decoded
  }

  def dataPut(session: String, dataIn: Map[String, Pickle]): Future[Unit] = {
    val encoded = dataIn.mapValues { _.value.toString }
    keystore.cache(journeyName, encoded).map{_ =>
      log.info(s"Encoded: $encoded")
      ()
    }
  }
}

case class SaveForLaterPersistence(
  journeyName: String,
  userId: String,
  shortLiveCache: ShortLivedHttpCaching
)(implicit
    ec: ExecutionContext,
  hc: HeaderCarrier
) extends Persistence {

  def dataGet(session: String): Future[Map[String, Pickle]] = {
    val decoded = shortLiveCache.fetchAndGetEntry[Map[String, String]](userId, journeyName).map{
      _.getOrElse(Map.empty).mapValues{JSONPickle}
    }
    log.info(s"Decoded: $decoded")
    decoded
  }

  def dataPut(session: String, dataIn: Map[String, Pickle]): Future[Unit] = {
    val encoded = dataIn.mapValues { _.value.toString }
    shortLiveCache.cache(userId, journeyName, encoded).map{_ =>
      log.info(s"Encoded: $encoded")
      ()
    }
  }


}
