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

package sdil.controllers

import com.softwaremill.macwire._
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{eq => matching, _}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers._
import sdil.models.backend.{UkAddress, Site}
import sdil.models.{Address, Producer}

class WarehouseControllerSpec extends ControllerSpec with BeforeAndAfterEach {

  "GET /secondary-warehouses" should {
    "return 200 Ok and the secondary warehouse page if no secondary warehouses have been added" in {
      stubFormPage(secondaryWarehouses = Some(Nil))

      val res = testController.show()(FakeRequest())
      status(res) mustBe OK
      contentAsString(res) must include(Messages("sdil.warehouse.heading"))
    }

    "return 200 Ok and the add secondary warehouse page if other secondary warehouses have been added" in {
      stubFormPage(secondaryWarehouses = Some(Seq(
        Site(
          UkAddress.fromAddress(Address("1 Warehouse Site St", "Warehouse Site Town", "", "", "AA11 1AA")),
          None,
          None,
          None)
      )))

      val res = testController.show()(FakeRequest())
      status(res) mustBe OK
      contentAsString(res) must include(Messages("sdil.warehouse.add.heading"))
    }

    "return a page with a link back to the production sites page if the user packages liable drinks" in {
      val res = testController.show()(FakeRequest())
      status(res) mustBe OK

      val html = Jsoup.parse(contentAsString(res))
      html.select("a.link-back").attr("href") mustBe routes.ProductionSiteController.show().url
    }

    "return a page with a link back to the start date page if the user does not package liable drinks " +
      "and the date is after the tax start date" in {

      stubFormPage(
        producer = Some(Producer(isProducer = false, isLarge = None)),
        packagesForOthers = Some(false)
      )

      val res = testController.show()(FakeRequest())
      status(res) mustBe OK

      val html = Jsoup.parse(contentAsString(res))
      html.select("a.link-back").attr("href") mustBe routes.StartDateController.show().url
    }
  }

  "POST /secondary-warehouses" should {
    "return 400 Bad Request if the no radio button is selected" in {
      stubFormPage(secondaryWarehouses = None)

      val res = testController.addSingleSite()(FakeRequest())
      status(res) mustBe BAD_REQUEST
      contentAsString(res) must include(Messages("sdil.warehouse.heading"))
    }

    "return 400 Bad Request if the user adds a warehouse, but does not supply the address" in {
      stubFormPage(secondaryWarehouses = None)

      val res = testController.addSingleSite()(FakeRequest().withFormUrlEncodedBody("addWarehouse" -> "true"))
      status(res) mustBe BAD_REQUEST
    }

    "redirect to the add secondary warehouse page if the user adds a warehouse" in {
      stubFormPage(secondaryWarehouses = None)

      val res = testController.addSingleSite()(FakeRequest().withFormUrlEncodedBody(
        "addAddress" -> "true",
        "tradingName" -> "name trade",
        "additionalAddress.line1" -> "line 3",
        "additionalAddress.line2" -> "line 4",
        "additionalAddress.line3" -> "line 5",
        "additionalAddress.line4" -> "line 6",
        "additionalAddress.postcode" -> "AA11 1AA"
      ))

      status(res) mustBe SEE_OTHER
      redirectLocation(res).value mustBe routes.WarehouseController.show().url
    }

    "redirect to the contact details page if the user has no warehouses" in {
      stubFormPage(secondaryWarehouses = None)

      val res = testController.addSingleSite()(FakeRequest().withFormUrlEncodedBody("addAddress" -> "false"))

      status(res) mustBe SEE_OTHER
      redirectLocation(res).value mustBe routes.ContactDetailsController.show().url
    }
  }

  "POST /secondary-warehouses/select-sites" should {
    "return 400 Bad Request if the user says they have a warehouse and does not fill in the address form" in {
      stubFormPage(secondaryWarehouses = Some(Seq(
        Site(
          UkAddress.fromAddress(Address("1 Warehouse Site St", "Warehouse Site Town", "", "", "AA11 1AA")),
          None,
          None,
          None)
      )))

      val res = testController.addMultipleSites()(FakeRequest().withFormUrlEncodedBody("addAddress" -> "true"))
      status(res) mustBe BAD_REQUEST
      contentAsString(res) must include(Messages("sdil.warehouse.add.heading"))
    }

    "redirect to the add secondary warehouse page if a warehouse has been added" in {
      val request = FakeRequest().withFormUrlEncodedBody(
        "addAddress" -> "true",
        "tradingName" -> "name trade",
        "additionalAddress.line1" -> "line 1",
        "additionalAddress.line2" -> "line 2",
        "additionalAddress.line3" -> "line 3",
        "additionalAddress.line4" -> "line 4",
        "additionalAddress.postcode" -> "AA11 1AA"
      )

      val res = testController.addMultipleSites()(request)
      status(res) mustBe SEE_OTHER
      redirectLocation(res) mustBe Some(routes.WarehouseController.show().url)
    }

    "redirect to the contact details page if a warehouse is not added" in {
      val res = testController.addMultipleSites()(FakeRequest().withFormUrlEncodedBody("addAddress" -> "false"))
      status(res) mustBe SEE_OTHER
      redirectLocation(res) mustBe Some(routes.ContactDetailsController.show().url)
    }

    "store the new address in keystore if a warehouse is added" in {
      stubFormPage(secondaryWarehouses = Some(Nil))

      val request = FakeRequest().withFormUrlEncodedBody(
        "addAddress" -> "true",
        "tradingName" -> "name trade",
        "additionalAddress.line1" -> "1 Warehouse Site St",
        "additionalAddress.line2" -> "Warehouse Site Town",
        "additionalAddress.line3" -> "",
        "additionalAddress.line4" -> "",
        "additionalAddress.postcode" -> "AA11 1AA"
      )

      val res = testController.addMultipleSites()(request)
      status(res) mustBe SEE_OTHER

      verify(mockCache, times(1))
        .cache(
          matching("internal id"),
          matching(defaultFormData.copy(secondaryWarehouses = Some(Seq(
            Site(
              UkAddress.fromAddress(Address("1 Warehouse Site St", "Warehouse Site Town", "", "", "AA11 1AA")),
              None,
              Some("name trade"),
              None)
          )))))(any())
    }
  }

  lazy val testController = wire[WarehouseController]

  override protected def beforeEach(): Unit = stubFilledInForm
}
