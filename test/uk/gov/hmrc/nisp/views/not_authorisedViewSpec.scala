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

import org.scalatest.mockito.MockitoSugar
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
import uk.gov.hmrc.nisp.utils.{Constants, FakeApplicationConfig, MockTemplateRenderer}
import uk.gov.hmrc.play.partials.{CachedStaticHtmlPartialRetriever, FormPartialRetriever}
import uk.gov.hmrc.renderer.TemplateRenderer

class not_authorisedViewSpec extends HtmlSpec with MockitoSugar {

  val fakeRequest = FakeRequest("GET", "/")

  val mockHttp = mock[WSHttp]

  val testFeedbackController = new FeedbackController {
    override implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever
    override implicit val formPartialRetriever: FormPartialRetriever = MockFormPartialRetriever

    override implicit val templateRenderer: TemplateRenderer = MockTemplateRenderer

    override def httpPost: HttpPost = mockHttp

    override def localSubmitUrl(implicit request: Request[AnyContent]): String = ""

    override def contactFormReferer(implicit request: Request[AnyContent]): String = request.headers.get(REFERER).getOrElse("")

    override val applicationConfig: ApplicationConfig = new FakeApplicationConfig {}

  }

  implicit val cachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever
  implicit val formPartialRetriever: uk.gov.hmrc.play.partials.FormPartialRetriever = NispFormPartialRetriever
  implicit val templateRenderer: TemplateRenderer = MockTemplateRenderer
  implicit val user: NispAuthedUser = NispAuthedUserFixture.user(TestAccountBuilder.regularNino)

  val feedbackFrontendUrl: String = "/foo"
  lazy val html = uk.gov.hmrc.nisp.views.html.iv.failurepages.not_authorised()
  lazy val source = asDocument(contentAsString(html))

  "NotAuthorised View" should {

    "assert correct page title" in {
      val title = source.title()
      val expected = messages("nisp.iv.failure.general.title") + Constants.titleSplitter +
                     messages("nisp.title.extension") + Constants.titleSplitter + messages("nisp.gov-uk")
      title must include(expected)
    }

    "assert correct paragraph text on page" in {
      val title = source.getElementsByTag("p").get(1).toString
      val messageKey = "nisp.iv.failure.general.youcan"
      val expected = ">" + messages(messageKey) + "<"
      title must include(expected)
    }

    "assert correct href for redirect link one on page" in {
      val redirect = source.getElementsByTag("a").get(1).attr("href")
      val expected = "/check-your-state-pension/account"
      redirect must include(expected)
    }

    "assert correct href for redirect link two on page" in {
      val redirect = source.getElementsByTag("a").get(2).attr("href")
      val expected = "/contact-hmrc"
      redirect must include(expected)
    }

    "assert correct href for redirect link three on page" in {
      val redirect = source.getElementsByTag("a").get(3).attr("href")
      val expected = "/future-pension-centre"
      redirect must include(expected)
    }
  }
}