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

package sdil.config

import java.time.LocalDate
import javax.inject.Inject

import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.config.ServicesConfig

trait AppConfig {
  val analyticsToken: String
  val analyticsHost: String
  val reportAProblemPartialUrl: String
  val reportAProblemNonJSUrl: String
  val betaFeedbackUrlAuth: String
  def taxStartDate: LocalDate
  val ggLoginUrl: String
  val sdilHomePage: String
  val appName: String
}

class FrontendAppConfig @Inject()(val runModeConfiguration: Configuration, environment: Environment) extends AppConfig with ServicesConfig {
  override protected def mode = environment.mode

  private def loadConfig(key: String) = runModeConfiguration.getString(key).getOrElse(throw new Exception(s"Missing configuration key: $key"))

  private val contactHost = runModeConfiguration.getString(s"contact-frontend.host").getOrElse("")
  private val contactFormServiceIdentifier = runModeConfiguration.getString("appName").get

  override lazy val analyticsToken = loadConfig(s"google-analytics.token")
  override lazy val analyticsHost = loadConfig(s"google-analytics.host")
  override lazy val reportAProblemPartialUrl = s"$contactHost/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
  override lazy val reportAProblemNonJSUrl = s"$contactHost/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"
  override lazy val betaFeedbackUrlAuth = s"$contactHost/contact/beta-feedback?service=$contactFormServiceIdentifier"

  //Auth related config
  val appName: String = loadConfig("appName")
  private lazy val companyAuthFrontend = getConfString("company-auth.url", "")
  private lazy val companyAuthSignInPath = getConfString("company-auth.sign-in-path", "")
  private lazy val companyAuthSignOutPath = getConfString("company-auth.sign-out-path", "")
  val ggLoginUrl: String = s"$companyAuthFrontend$companyAuthSignInPath"
  val sdilHomePage: String = loadConfig("sdil-home-page-url")
  val taxStartDate: LocalDate = LocalDate.parse(loadConfig("tax-start-date"))
}