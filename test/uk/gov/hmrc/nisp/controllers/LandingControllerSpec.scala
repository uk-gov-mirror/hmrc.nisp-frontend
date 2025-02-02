/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.nisp.controllers

import java.util.{Locale, UUID}

import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{any => mockAny, eq => mockEQ}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Lang, MessagesApi, MessagesImpl}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers, Injecting}
import uk.gov.hmrc.auth.core.{AuthConnector, MissingBearerToken}
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.connectors.{IdentityVerificationConnector, IdentityVerificationSuccessResponse}
import uk.gov.hmrc.nisp.controllers.auth.VerifyAuthActionImpl
import uk.gov.hmrc.nisp.helpers.{FakeTemplateRenderer, _}
import uk.gov.hmrc.play.partials.{CachedStaticHtmlPartialRetriever, FormPartialRetriever}
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.time.DateTimeUtils._

import scala.concurrent.Future

class LandingControllerSpec extends PlaySpec with MockitoSugar with BeforeAndAfterEach with GuiceOneAppPerSuite with Injecting {

  implicit val fakeRequest = FakeRequest("GET", "/")
  val fakeRequestWelsh = FakeRequest("GET", "/cymraeg")
  val mockApplicationConfig: ApplicationConfig = mock[ApplicationConfig]
  val mockIVConnector: IdentityVerificationConnector = mock[IdentityVerificationConnector]

  implicit val cachedRetriever: CachedStaticHtmlPartialRetriever = FakeCachedStaticHtmlPartialRetriever
  implicit val formPartialRetriever: FormPartialRetriever = FakePartialRetriever
  implicit val templateRenderer: TemplateRenderer = FakeTemplateRenderer

  val injector = GuiceApplicationBuilder().
    overrides(
      bind[IdentityVerificationConnector].toInstance(mockIVConnector),
      bind[ApplicationConfig].toInstance(mockApplicationConfig),
      bind[CachedStaticHtmlPartialRetriever].toInstance(cachedRetriever),
      bind[FormPartialRetriever].toInstance(formPartialRetriever),
      bind[TemplateRenderer].toInstance(templateRenderer),
      bind[VerifyAuthActionImpl].to[FakeVerifyAuthAction]
    ).injector()

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockApplicationConfig, mockIVConnector)
  }

  val verifyLandingController = injector.instanceOf[LandingController]


  implicit val messages: MessagesImpl = MessagesImpl(Lang(Locale.getDefault), inject[MessagesApi])

  "GET /" should {
    "return 200" in {
      val result = verifyLandingController.show(fakeRequest)
      status(result) mustBe OK
    }

    "return HTML" in {
      val result = verifyLandingController.show(fakeRequest)
      Helpers.contentType(result) mustBe Some("text/html")
      charset(result) mustBe Some("utf-8")
    }

    "load the landing page" in {
      val result = verifyLandingController.show(fakeRequest)
      contentAsString(result) must include("Your State Pension forecast is provided for your information only and the " +
        "service does not offer financial advice. When planning for your retirement, you should seek professional advice.")
    }

    "have a start button" in {
      val result = verifyLandingController.show(fakeRequest)
      contentAsString(result) must include("Continue")
    }

    "return IVLanding page" in {
      when(mockApplicationConfig.identityVerification).thenReturn(true)

      val result = verifyLandingController.show(fakeRequest)
      val doc = Jsoup.parse( contentAsString(result))
      doc.getElementById("landing-signin-heading").text mustBe messages("nisp.landing.signin.heading")
    }

    "return non-IV landing page when switched on" in {
      when(mockApplicationConfig.identityVerification).thenReturn(false)

      val result = verifyLandingController.show(fakeRequest)
      val doc = Jsoup.parse( contentAsString(result))
      doc.getElementById("eligibility-heading").text mustBe messages("nisp.landing.eligibility.heading")
    }
  }

  "GET /signin/verify" must {
    "redirect to verify" in {
      val mockAuthConnector = mock[AuthConnector]

      val verifyAuthBasedInjector = GuiceApplicationBuilder().
        overrides(
          bind[IdentityVerificationConnector].toInstance(mockIVConnector),
          bind[CachedStaticHtmlPartialRetriever].toInstance(cachedRetriever),
          bind[FormPartialRetriever].toInstance(formPartialRetriever),
          bind[AuthConnector].toInstance(mockAuthConnector),
          bind[TemplateRenderer].toInstance(templateRenderer)
        ).injector()

      when(mockAuthConnector.authorise(mockAny(), mockAny())(mockAny(), mockAny()))
        .thenReturn(Future.failed(MissingBearerToken("Missing Bearer Token!")))

      val verifyLandingController = verifyAuthBasedInjector.instanceOf[LandingController]
      val result = verifyLandingController.verifySignIn(fakeRequest)
      redirectLocation(result) mustBe Some("http://localhost:9949/auth-login-stub/verify-sign-in?continue=http%3A%2F%2Flocalhost%3A9234%2Fcheck-your-state-pension%2Faccount")
    }

    "redirect to account page when signed in" in {
      val result = verifyLandingController.verifySignIn(FakeRequest().withSession(
        SessionKeys.sessionId -> s"session-${UUID.randomUUID()}",
        SessionKeys.lastRequestTimestamp -> now.getMillis.toString
      ))

      redirectLocation(result) mustBe Some("/check-your-state-pension/account")
    }
  }

  "GET /not-authorised" must {
    "show not authorised page" in {
      val result = verifyLandingController.showNotAuthorised(None)(fakeRequest)
      contentAsString(result) must include("We cannot confirm your identity")
    }

    "show generic not_authorised template for FailedMatching journey" in {
      val journeyId = "failed-matching-journey-id"

      when(mockIVConnector.identityVerificationResponse(mockEQ(journeyId))(mockAny())).thenReturn(
        Future.successful(IdentityVerificationSuccessResponse("FailedMatching"))
      )

      val result = verifyLandingController.showNotAuthorised(Some(journeyId))(fakeRequest)
      contentAsString(result) must include("We cannot confirm your identity")
    }

    "show generic not_authorised template for InsufficientEvidence journey" in {
      val journeyId = "insufficient-evidence-journey-id"

      when(mockIVConnector.identityVerificationResponse(mockEQ(journeyId))(mockAny())).thenReturn(
        Future.successful(IdentityVerificationSuccessResponse("InsufficientEvidence"))
      )

      val result = verifyLandingController.showNotAuthorised(Some(journeyId))(fakeRequest)
      contentAsString(result) must include("We cannot confirm your identity")
    }

    "show generic not_authorised template for Incomplete journey" in {
      val journeyId = "incomplete-journey-id"

      when(mockIVConnector.identityVerificationResponse(mockEQ(journeyId))(mockAny())).thenReturn(
        Future.successful(IdentityVerificationSuccessResponse("Incomplete"))
      )

      val result = verifyLandingController.showNotAuthorised(Some(journeyId))(fakeRequest)
      contentAsString(result) must include("We cannot confirm your identity")
    }

    "show generic not_authorised template for PreconditionFailed journey" in {
      val journeyId = "precondition-failed-journey-id"

      when(mockIVConnector.identityVerificationResponse(mockEQ(journeyId))(mockAny())).thenReturn(
        Future.successful(IdentityVerificationSuccessResponse("PreconditionFailed"))
      )

      val result = verifyLandingController.showNotAuthorised(Some(journeyId))(fakeRequest)
      contentAsString(result) must include("We cannot confirm your identity")
    }

    "show generic not_authorised template for UserAborted journey" in {
      val journeyId = "user-aborted-journey-id"

      when(mockIVConnector.identityVerificationResponse(mockEQ(journeyId))(mockAny())).thenReturn(
        Future.successful(IdentityVerificationSuccessResponse("UserAborted"))
      )

      val result = verifyLandingController.showNotAuthorised(Some(journeyId))(fakeRequest)
      contentAsString(result) must include("We cannot confirm your identity")
    }

    "show technical_issue template for TechnicalIssue journey" in {
      val journeyId = "technical-issue-journey-id"

      when(mockIVConnector.identityVerificationResponse(mockEQ(journeyId))(mockAny())).thenReturn(
        Future.successful(IdentityVerificationSuccessResponse("TechnicalIssue"))
      )

      val result = verifyLandingController.showNotAuthorised(Some(journeyId))(fakeRequest)
      contentAsString(result) must include("This online service is experiencing technical difficulties.")
    }

    "show locked_out template for LockedOut journey" in {
      val journeyId = "locked-out-journey-id"

      when(mockIVConnector.identityVerificationResponse(mockEQ(journeyId))(mockAny())).thenReturn(
        Future.successful(IdentityVerificationSuccessResponse("LockedOut"))
      )

      val result = verifyLandingController.showNotAuthorised(Some(journeyId))(fakeRequest)
      contentAsString(result) must include("You have reached the maximum number of attempts to confirm your identity.")
    }

    "show timeout template for Timeout journey" in {
      val journeyId = "timeout-journey-id"

      when(mockIVConnector.identityVerificationResponse(mockEQ(journeyId))(mockAny())).thenReturn(
        Future.successful(IdentityVerificationSuccessResponse("Timeout"))
      )

      val result = verifyLandingController.showNotAuthorised(Some(journeyId))(fakeRequest)
      contentAsString(result) must include("Your session has ended because you have not done anything for 15 minutes.")
    }

    "show 2FA failure page when no journey ID specified" in {
      val result = verifyLandingController.showNotAuthorised(None)(fakeRequest)
      contentAsString(result) must include("We cannot confirm your identity")
      contentAsString(result) must not include "If you cannot confirm your identity and you have a query you can"
    }
  }

  "GET /cymraeg" must {
    implicit val lang = Lang("cy")
    "return 200" in {
      val result = verifyLandingController.show(fakeRequestWelsh)
      status(result) mustBe OK
    }

    "return HTML" in {
      val result = verifyLandingController.show(fakeRequestWelsh)
      Helpers.contentType(result) mustBe Some("text/html")
      charset(result) mustBe Some("utf-8")
    }
    "load the landing page in welsh" in {
      val result = verifyLandingController.show(fakeRequestWelsh)
      contentAsString(result) must include("data-journey-click=\"checkmystatepension:language: cy\"")
    }
  }
}
