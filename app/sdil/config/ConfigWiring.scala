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

package sdil.config

import com.softwaremill.macwire.wire
import uk.gov.hmrc.crypto.{ApplicationCryptoDI, CompositeSymmetricCrypto}
import uk.gov.hmrc.http.cache.client.ShortLivedHttpCaching
import uk.gov.hmrc.play.bootstrap.http.{FrontendErrorHandler, HttpClient}

trait ConfigWiring extends CommonWiring {
  implicit lazy val appConfig: AppConfig = wire[FrontendAppConfig]
  private lazy val applicationCrypto: ApplicationCryptoDI = wire[ApplicationCryptoDI]
  private implicit lazy val crypto: CompositeSymmetricCrypto = applicationCrypto.JsonCrypto
  lazy val cache: RegistrationFormDataCache = wire[RegistrationFormDataCache]
  lazy val errorHandler: FrontendErrorHandler = wire[SDILErrorHandler]
  lazy val keystore: SDILSessionCache = wire[SDILSessionCache]
}
