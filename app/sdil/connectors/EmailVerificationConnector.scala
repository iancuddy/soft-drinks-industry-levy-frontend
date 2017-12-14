/*
 * Copyright 2017 HM Revenue & Customs
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

package sdil.connectors

import play.api.Mode.Mode
import play.api.{Configuration, Environment}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Call, Request}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, Upstream4xxResponse}
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}

class EmailVerificationConnector(http: HttpClient,
                                 environment: Environment,
                                 val runModeConfiguration: Configuration)
                                (implicit ec: ExecutionContext) extends ServicesConfig {

  private implicit def hc(implicit request: Request[_]): HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

  override protected def mode: Mode = environment.mode

  def sendVerificationEmail(email: String, redirectTo: Call)(implicit request: Request[_]): Future[VerificationResult] = {
    val payload = Json.obj(
      "email" -> email,
      "templateId" -> "verifyEmailAddress",
      "linkExpiryDuration" -> "P1D",
      "continueUrl" -> redirectTo.absoluteURL()
    )
    val verificationUrl = s"${baseUrl("email-verification")}/email-verification/verification-requests"

    http.POST[JsValue, HttpResponse](verificationUrl, payload) map {
      _ => VerificationResult.EmailSent
    } recover {
      case Upstream4xxResponse(_, 409, _, _) => VerificationResult.EmailVerified
    }
  }

}

sealed trait VerificationResult

object VerificationResult {
  case object EmailSent extends VerificationResult
  case object EmailVerified extends VerificationResult
}
