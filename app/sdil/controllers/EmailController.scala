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

package sdil.controllers

import play.api.data.Forms._
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import sdil.actions.AuthorisedAction
import sdil.config.AppConfig
import sdil.connectors.{EmailVerificationConnector, VerificationResult}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import views.html.softdrinksindustrylevy.register

class EmailController(val messagesApi: MessagesApi,
                      authorisedAction: AuthorisedAction,
                      emailVerificationConnector: EmailVerificationConnector)
                     (implicit config: AppConfig)
  extends FrontendController with I18nSupport {

  def show = authorisedAction { implicit request =>
    Ok(register.email.email(emailForm))
  }

  def validate = authorisedAction.async { implicit request =>
    emailForm.bindFromRequest().fold(
      errors => BadRequest(register.email.email(errors)),
      email => emailVerificationConnector.sendVerificationEmail(email, routes.EmailController.verified) map {
        case VerificationResult.EmailSent => Redirect(routes.EmailController.verify)
        case VerificationResult.EmailVerified => Redirect(routes.EmailController.verified)
      }
    )
  }

  def verify = authorisedAction { implicit request =>
    Ok(register.email.verification_required())
  }

  def verified = authorisedAction { implicit request =>
    Ok(register.email.email_verified())
  }

  lazy val emailForm = Form(single("email" -> email))
}
