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

package sdil.controllers.variation

import com.softwaremill.macwire.wire
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{any, eq => matching}
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.BeforeAndAfterAll
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, status, _}
import sdil.controllers.ControllerSpec
import sdil.models.variations._
import uk.gov.hmrc.auth.core.retrieve.Retrievals.allEnrolments
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, Enrolments}

import scala.concurrent.Future

class PacakgeOwnControllerSpec extends ControllerSpec with BeforeAndAfterAll {

  override protected def beforeAll(): Unit = {
    val sdilEnrolment = EnrolmentIdentifier("EtmpRegistrationNumber", "XZSDIL000100107")

    when(mockAuthConnector.authorise[Enrolments](any(), matching(allEnrolments))(any(), any())).thenReturn {
      Future.successful(Enrolments(Set(Enrolment("HMRC-OBTDS-ORG", Seq(sdilEnrolment), "Active"))))
    }

    when(mockKeystore.fetchAndGetEntry[VariationData](matching("variationData"))(any(), any(), any()))
      .thenReturn(Future.successful(Some(VariationData(subscription))))
  }

  "GET /variations/package-own" should {
    "return 200 and the package own page" in {
      val res = testController.show()(FakeRequest())
      status(res) mustBe OK

      contentAsString(res) must include(messagesApi("sdil.packageOwnUk.heading"))
    }

    "return a page with a link back to the uses copacker page" in {
      val data = VariationData(subscription)
        .copy(previousPages = Seq(
          routes.VariationsController.show,
          routes.ProducerVariationsController.show,
          routes.UsesCopackerController.show)
        )

      when(mockKeystore.fetchAndGetEntry[VariationData](matching("variationData"))(any(), any(), any()))
        .thenReturn(Future.successful(Some(data)))

      val res = testController.show()(FakeRequest())
      status(res) mustBe OK

      val html = Jsoup.parse(contentAsString(res))
      html.select("a.link-back").attr("href") mustBe routes.UsesCopackerController.show().url
    }

    "return a page with a link back to the producer-variations page" in {
      val data = VariationData(subscription)
        .copy(previousPages = Seq(
          routes.VariationsController.show,
          routes.ProducerVariationsController.show)
        )

      when(mockKeystore.fetchAndGetEntry[VariationData](matching("variationData"))(any(), any(), any()))
        .thenReturn(Future.successful(Some(data)))

      val res = testController.show()(FakeRequest())
      status(res) mustBe OK

      val html = Jsoup.parse(contentAsString(res))
      html.select("a.link-back").attr("href") mustBe routes.ProducerVariationsController.show().url
    }
  }

  "POST /variations/package-own" should {
    "return 400 Bad Request if the form data is invalid" in {
      val res = testController.submit()(FakeRequest().withFormUrlEncodedBody())
      status(res) mustBe BAD_REQUEST
    }

    "return 303 See Other and redirect to the package own vol page with valid form data" in {
      val result = testController.submit()(FakeRequest().withFormUrlEncodedBody(
        "yesOrNo" -> "true"
      ))

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe routes.PackageOwnVolController.show().url
    }

    "return 303 See Other and redirect to the variations summary page with valid form data" in {
      val result = testController.submit()(FakeRequest().withFormUrlEncodedBody(
        "yesOrNo" -> "false"
      ))

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe routes.VariationsController.show().url
    }

    "update the cached form data when the form data is valid" in {
      val data = VariationData(subscription.copy(utr = "9998887776"))

      when(mockKeystore.fetchAndGetEntry[VariationData](matching("variationData"))(any(), any(), any()))
        .thenReturn(Future.successful(Some(data)))

      val request = FakeRequest().withFormUrlEncodedBody(
        "yesOrNo" -> "true"
      )

      val res = testController.submit()(request)
      status(res) mustBe SEE_OTHER

      verify(mockKeystore, times(1))
        .cache(
          matching("variationData"),
          matching(data.copy(packageOwn = Option[Boolean](
            true
          )))
        )(any(), any(), any())
    }
  }
  lazy val testController = wire[PackageOwnController]
}
