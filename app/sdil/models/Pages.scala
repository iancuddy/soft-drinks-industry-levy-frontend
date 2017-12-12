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

package sdil.models

import play.api.mvc.Call
import sdil.controllers.routes

sealed trait Page {
  def isComplete(formData: RegistrationFormData): Boolean

  def expectedPage(formData: RegistrationFormData): Page = this

  def show: Call
}

sealed trait PageWithPreviousPage extends Page {
  def previousPage(formData: RegistrationFormData): Page

  override def expectedPage(formData: RegistrationFormData): Page = {
    previousPage(formData) match {
      case page if page.isComplete(formData) => this
      case page => page.expectedPage(formData)
    }
  }
}

sealed trait PageWithNextPage extends Page {
  def nextPage(formData: RegistrationFormData): Page
}

sealed trait MidJourneyPage extends PageWithPreviousPage with PageWithNextPage

case object IdentifyPage extends PageWithNextPage {
  override def nextPage(formData: RegistrationFormData): Page = VerifyPage
  override def isComplete(formData: RegistrationFormData): Boolean = true

  override def show: Call = routes.IdentifyController.identify()
}

case object VerifyPage extends MidJourneyPage {
  override def nextPage(formData: RegistrationFormData): Page = formData.verify match {
    case Some(DetailsCorrect.No) => IdentifyPage
    case Some(_) => PackagePage
    case None => VerifyPage
  }
  override def previousPage(formData: RegistrationFormData): Page = IdentifyPage
  override def isComplete(formData: RegistrationFormData): Boolean = formData.verify.isDefined

  override def show: Call = routes.VerifyController.verify()
}

case object PackagePage extends MidJourneyPage {
  override def nextPage(formData: RegistrationFormData): Page = formData.packaging match {
    case Some(p) if p.isLiable && p.ownBrands => PackageOwnPage
    case Some(p) if p.isLiable && p.customers => PackageCopackPage
    case Some(p) => CopackedPage
    case None => PackagePage
  }

  override def previousPage(formData: RegistrationFormData): Page = VerifyPage

  override def isComplete(formData: RegistrationFormData): Boolean = formData.packaging.isDefined

  override def show: Call = routes.PackageController.displayPackage()
}

case object PackageOwnPage extends MidJourneyPage {
  override def nextPage(formData: RegistrationFormData): Page = formData.packaging match {
    case Some(p) if p.isLiable && p.customers => CopackedVolumePage
    case Some(p) => CopackedPage
    case None => PackagePage
  }

  override def previousPage(formData: RegistrationFormData): Page = PackagePage

  override def isComplete(formData: RegistrationFormData): Boolean = formData.packageOwn.isDefined

  override def show: Call = routes.LitreageController.show("packageOwn")
}

case object PackageCopackPage extends MidJourneyPage {
  override def nextPage(formData: RegistrationFormData): Page = PackageCopackSmallPage

  override def previousPage(formData: RegistrationFormData): Page = formData.packaging match {
    case Some(p) if p.isLiable && p.ownBrands => PackageOwnPage
    case _ => PackagePage
  }

  override def isComplete(formData: RegistrationFormData): Boolean = formData.packageCopack.isDefined

  override def show: Call = routes.LitreageController.show("packageCopack")
}

case object PackageCopackSmallPage extends MidJourneyPage {
  override def nextPage(formData: RegistrationFormData): Page = formData.packageCopackSmall match {
    case Some(true) => PackageCopackSmallVolPage
    case Some(false) => CopackedPage
    case None => PackageCopackPage
  }

  override def previousPage(formData: RegistrationFormData): Page = formData.packaging match {
    case Some(p) if p.isLiable && p.customers => PackageCopackPage
    case Some(p) if p.isLiable && p.ownBrands => PackageOwnPage
    case _ => PackagePage
  }

  override def isComplete(formData: RegistrationFormData): Boolean = {
    formData.packaging match {
      case Some(p) if (!p.customers && !p.ownBrands) || formData.packageCopackSmall.isDefined  => true
      case _ =>
    }

    formData.packageCopackSmall.isDefined
  }

  override def show: Call = routes.RadioFormController.display("package-copack-small")
}

case object PackageCopackSmallVolPage extends MidJourneyPage {
  override def nextPage(formData: RegistrationFormData): Page = CopackedPage

  override def previousPage(formData: RegistrationFormData): Page = PackageCopackSmallPage

  override def isComplete(formData: RegistrationFormData): Boolean = formData.packageCopackSmallVol.isDefined

  override def show: Call = routes.LitreageController.show("packageCopackSmallVol")
}

case object CopackedPage extends MidJourneyPage {
  override def nextPage(formData: RegistrationFormData): Page = formData.copacked match {
    case Some(true) => CopackedVolumePage
    case Some(false) => ImportPage
    case None => CopackedPage
  }

  override def previousPage(formData: RegistrationFormData): Page = formData.packaging match {
    case Some(p) if p.customers && formData.packageCopackSmall.contains(true) =>  PackageCopackSmallVolPage
    case Some(p) if p.customers && formData.packageCopackSmall.contains(false) =>  PackageCopackSmallPage
    case Some(p) if p.ownBrands && !p.customers => PackageOwnPage
    case _ => PackagePage
  }

  override def isComplete(formData: RegistrationFormData): Boolean = formData.copacked.isDefined

  override def show: Call = routes.RadioFormController.display("copacked")
}

case object CopackedVolumePage extends MidJourneyPage {
  override def nextPage(formData: RegistrationFormData): Page = ImportPage

  override def previousPage(formData: RegistrationFormData): Page = CopackedPage

  override def isComplete(formData: RegistrationFormData): Boolean = formData.copackedVolume.isDefined

  override def show: Call = routes.LitreageController.show("copackedVolume")
}

case object ImportPage extends MidJourneyPage {
  override def nextPage(formData: RegistrationFormData): Page = formData.imports match {
    case Some(true) => ImportVolumePage
    case Some(false) => StartDatePage
    case None => ImportPage
  }

  override def previousPage(formData: RegistrationFormData): Page = formData.copacked match {
    case Some(true) => CopackedVolumePage
    case _ => CopackedPage
  }

  override def isComplete(formData: RegistrationFormData): Boolean = formData.imports.isDefined

  override def show: Call = routes.RadioFormController.display("import")
}

case object ImportVolumePage extends MidJourneyPage {
  override def nextPage(formData: RegistrationFormData): Page = StartDatePage

  override def previousPage(formData: RegistrationFormData): Page = ImportPage

  override def isComplete(formData: RegistrationFormData): Boolean = formData.importVolume.isDefined

  override def show: Call = routes.LitreageController.show("importVolume")
}

case object StartDatePage extends MidJourneyPage {
  override def nextPage(formData: RegistrationFormData): Page = formData.packaging match {
    case Some(p) if p.isLiable => ProductionSitesPage
    case Some(p) => WarehouseSitesPage
    case None => PackagePage
  }

  override def previousPage(formData: RegistrationFormData): Page = formData.imports match {
    case Some(true) => ImportVolumePage
    case _ => ImportPage
  }

  override def isComplete(formData: RegistrationFormData): Boolean = formData.startDate.isDefined

  override def show: Call = routes.StartDateController.displayStartDate()
}

case object ProductionSitesPage extends MidJourneyPage {
  override def nextPage(formData: RegistrationFormData): Page = WarehouseSitesPage

  override def previousPage(formData: RegistrationFormData): Page = StartDatePage

  override def isComplete(formData: RegistrationFormData): Boolean = formData.productionSites.nonEmpty

  override def show: Call = routes.ProductionSiteController.addSite()
}

case object WarehouseSitesPage extends MidJourneyPage {
  override def nextPage(formData: RegistrationFormData): Page = ContactDetailsPage

  override def previousPage(formData: RegistrationFormData): Page = formData.packaging match {
    case Some(p) if p.isLiable => ProductionSitesPage
    case Some(p) => StartDatePage
    case None => PackagePage
  }

  override def isComplete(formData: RegistrationFormData): Boolean = true

  override def show: Call = routes.WarehouseController.show()
}

case object ContactDetailsPage extends PageWithPreviousPage {
  override def previousPage(formData: RegistrationFormData): Page = WarehouseSitesPage

  override def isComplete(formData: RegistrationFormData): Boolean = formData.contactDetails.isDefined

  override def show: Call = routes.ContactDetailsController.displayContactDetails()
}