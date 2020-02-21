/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.nisp.views

import org.scalatest.mock.MockitoSugar
import play.api.mvc.{AnyContent, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, _}
import uk.gov.hmrc.http.HttpPost
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.config.wiring.{NispFormPartialRetriever, WSHttp}
import uk.gov.hmrc.nisp.controllers.FeedbackController
import uk.gov.hmrc.nisp.controllers.auth.NispAuthedUser
import uk.gov.hmrc.nisp.fixtures.NispAuthedUserFixture
import uk.gov.hmrc.nisp.helpers._
import uk.gov.hmrc.nisp.utils.{Constants, MockTemplateRenderer}
import uk.gov.hmrc.nisp.views.html.landing
import uk.gov.hmrc.play.partials.{CachedStaticHtmlPartialRetriever, FormPartialRetriever}
import uk.gov.hmrc.renderer.TemplateRenderer

class LandingViewSpec extends HtmlSpec with MockitoSugar {

  val fakeRequest = FakeRequest("GET", "/")

  val mockHttp = mock[WSHttp]

  val testFeedbackController = new FeedbackController {
    override implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever
    override implicit val formPartialRetriever: FormPartialRetriever = MockFormPartialRetriever

    override implicit val templateRenderer: TemplateRenderer = MockTemplateRenderer

    override def httpPost: HttpPost = mockHttp

    override def localSubmitUrl(implicit request: Request[AnyContent]): String = ""

    override def contactFormReferer(implicit request: Request[AnyContent]): String = request.headers.get(REFERER).getOrElse("")

    override val applicationConfig: ApplicationConfig = new ApplicationConfig {
      override val ggSignInUrl: String = ""
      override val verifySignIn: String = ""
      override val verifySignInContinue: Boolean = false
      override val assetsPrefix: String = ""
      override val reportAProblemNonJSUrl: String = ""
      override val ssoUrl: Option[String] = None
      override val identityVerification: Boolean = false
      override val betaFeedbackUnauthenticatedUrl: String = ""
      override val notAuthorisedRedirectUrl: String = ""
      override val contactFrontendPartialBaseUrl: String = ""
      override val govUkFinishedPageUrl: String = ""
      override val showGovUkDonePage: Boolean = false
      override val analyticsHost: String = ""
      override val betaFeedbackUrl: String = ""
      override val analyticsToken: Option[String] = None
      override val reportAProblemPartialUrl: String = ""
      override val contactFormServiceIdentifier: String = "NISP"
      override val postSignInRedirectUrl: String = ""
      override val ivUpliftUrl: String = ""
      override val pertaxFrontendUrl: String = ""
      override val breadcrumbPartialUrl: String = ""
      override lazy val showFullNI: Boolean = false
      override val futureProofPersonalMax: Boolean = false
      override val isWelshEnabled = false
      override val frontendTemplatePath: String = "microservice.services.frontend-template-provider.path"
      override val feedbackFrontendUrl: String = "/foo"
    }
  }

  implicit val cachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever
  implicit val formPartialRetriever: uk.gov.hmrc.play.partials.FormPartialRetriever = NispFormPartialRetriever
  implicit val templateRenderer: TemplateRenderer = MockTemplateRenderer
  implicit val user: NispAuthedUser = NispAuthedUserFixture.user(TestAccountBuilder.regularNino)

  val feedbackFrontendUrl: String = "/foo"

  "return correct page title on landing page" in {
    val html = landing()
    val current: String = contentAsString(html)
    val document = asDocument(current)
    val title = document.title()
    val expected = messages("nisp.landing.title") + Constants.titleSplitter +
                   messages("nisp.title.extension") + Constants.titleSplitter + messages("nisp.gov-uk")
    title must include(expected)
  }

  "return correct title on the landing page" in {
    val html = landing()
    val current: String = contentAsString(html)
    val document = asDocument(current)
    val title = document.title()
    val expected = messages("nisp.landing.title")
    title must include(expected)
  }
}