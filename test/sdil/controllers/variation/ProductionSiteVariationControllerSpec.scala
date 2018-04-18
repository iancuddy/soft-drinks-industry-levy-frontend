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
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, status, _}
import sdil.controllers.ControllerSpec
import sdil.models.Address
import sdil.models.backend.Site
import sdil.models.variations.VariationData
import uk.gov.hmrc.auth.core.retrieve.Retrievals.allEnrolments
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, Enrolments}

import scala.collection.JavaConverters._
import scala.concurrent.Future

class ProductionSiteVariationControllerSpec  extends ControllerSpec with BeforeAndAfterAll {

  "GET /production-sites" should {
    "return 200 Ok and the packaging-sites page" in {
      val res = testController.show()(FakeRequest())
      status(res) mustBe OK

      val html = Jsoup.parse(contentAsString(res))
      html.select("h1").text mustBe Messages("sdil.productionSite.heading")
      html.select("a.link-back").attr("href") mustBe routes.VariationsController.show().url
    }

    "return a page including all production sites added so far" in {
      val sites = Seq(
        Address("3 The Place", "Another place", "", "", "AA11 1AA"),
        Address("4 The Place", "Another place", "", "", "AA12 2AA"),
        Address("5 The Place", "Another place", "", "", "AA13 3AA")
      )

      val data = VariationData(
        subscription.copy(productionSites = sites.map { x => Site.fromAddress(x) }.toList))

      when(mockKeystore.fetchAndGetEntry[VariationData](matching("variationData"))(any(), any(), any()))
      .thenReturn(Future.successful(Some(data)))

      val res = testController.show()(FakeRequest())
      status(res) mustBe OK

      val html = Jsoup.parse(contentAsString(res))
      val checkboxLabels = html.select(""".multiple-choice input[type="checkbox"]""")
        .asScala.filter(_.id.startsWith("additionalSites")).map(_.nextElementSibling().text)

      checkboxLabels must contain theSameElementsAs sites.map(_.nonEmptyLines.mkString(", "))
    }
  }

  "POST /production-site" should {
    "return 400 Bad Request and the production sites page if the user says they need to add another address, " +
      "but do not fill in the address form" in {

      val res = testController.submit()(FakeRequest().withFormUrlEncodedBody("addAddress" -> "true"))
      status(res) mustBe BAD_REQUEST
    }

    "return 400 Bad Request when the user selects no production sites and does not add a new site" in {
      val res = testController.submit()(FakeRequest())
      status(res) mustBe BAD_REQUEST
    }

    "redirect to the production sites page if another site has been added and the form data is valid" in {
      val request = FakeRequest().withFormUrlEncodedBody(
        "addAddress" -> "true",
        "additionalAddress.line1" -> "line 1",
        "additionalAddress.line2" -> "line 2",
        "additionalAddress.line3" -> "",
        "additionalAddress.line4" -> "",
        "additionalAddress.postcode" -> "AA11 1AA"
      )

      val res = testController.submit()(request)
      status(res) mustBe SEE_OTHER
      redirectLocation(res) mustBe Some(routes.ProductionSiteVariationController.show().url)
    }

    "redirect to the warehouse page if another site has not been added and the form data is valid" in {
      val res = testController.submit()(FakeRequest().withFormUrlEncodedBody("bprAddress" -> "true"))
      status(res) mustBe SEE_OTHER
      redirectLocation(res) mustBe Some(routes.VariationsController.show().url)
    }

    "store the new address in keystore if another site has been added and the form data is valid" in {
      val data = VariationData(subscription.copy(productionSites = Nil))

      when(mockKeystore.fetchAndGetEntry[VariationData](matching("variationData"))(any(), any(), any()))
        .thenReturn(Future.successful(Some(data)))

      val request = FakeRequest().withFormUrlEncodedBody(
        "addAddress" -> "true",
        "additionalAddress.line1" -> "line 2",
        "additionalAddress.line2" -> "line 3",
        "additionalAddress.line3" -> "",
        "additionalAddress.line4" -> "",
        "additionalAddress.postcode" -> "AA12 2AA"
      )

      val res = testController.submit()(request)
      status(res) mustBe SEE_OTHER

      verify(mockKeystore, times(1)).cache(
        matching("variationData"),
        matching((VariationData(subscription)).copy(updatedProductionSites= Seq(Address("line 2", "line 3", "", "", "AA12 2AA"))))
      )(any(), any(), any())
    }

  }

  lazy val testController: ProductionSiteVariationController = wire[ProductionSiteVariationController]

  override protected def beforeAll(): Unit = {
    val sdilEnrolment = EnrolmentIdentifier("EtmpRegistrationNumber", "XZSDIL000100107")

    when(mockAuthConnector.authorise[Enrolments](any(), matching(allEnrolments))(any(), any())).thenReturn {
      Future.successful(Enrolments(Set(Enrolment("HMRC-OBTDS-ORG", Seq(sdilEnrolment), "Active"))))
    }

    when(mockKeystore.fetchAndGetEntry[VariationData](matching("variationData"))(any(), any(), any()))
      .thenReturn(Future.successful(Some(VariationData(subscription))))
  }

}