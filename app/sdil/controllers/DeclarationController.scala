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

import java.time.{LocalDateTime, ZoneId}

import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import sdil.actions.FormAction
import sdil.config.{AppConfig, RegistrationFormDataCache}
import sdil.connectors.SoftDrinksIndustryLevyConnector
import sdil.models.backend.Subscription
import sdil.models.{DeclarationPage, Journey, SubmissionData}
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import views.html.softdrinksindustrylevy.register

class DeclarationController(val messagesApi: MessagesApi,
                            cache: RegistrationFormDataCache,
                            formAction: FormAction,
                            sdilConnector: SoftDrinksIndustryLevyConnector,
                            keystore: SessionCache)
                           (implicit config: AppConfig)
  extends FrontendController with I18nSupport {

  def show: Action[AnyContent] = formAction.async { implicit request =>
    Journey.expectedPage(DeclarationPage) match {
      case DeclarationPage => Ok(register.declaration(request.formData))
      case other => Redirect(other.show)
    }
  }

  def submit(): Action[AnyContent] = formAction.async { implicit request =>
    Subscription.fromFormData(request.formData) match {
      case Some(s) => for {
        _ <- sdilConnector.submit(s, request.formData.rosmData.safeId)
        _ <- keystore.cache(
          "submissionData",
          SubmissionData(s.contact.email, LocalDateTime.now(ZoneId.of("Europe/London")), request.formData.isVoluntary)
        )
        _ <- cache.clear(request.internalId)
      } yield {
        Redirect(routes.CompleteController.show())
      }
      case None => Redirect(Journey.expectedPage(DeclarationPage).show)
    }
  }

}
