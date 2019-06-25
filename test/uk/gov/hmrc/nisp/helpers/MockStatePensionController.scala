/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.nisp.helpers

import uk.gov.hmrc.nisp.controllers.StatePensionController
import uk.gov.hmrc.nisp.fixtures.MockApplicationConfig
import uk.gov.hmrc.nisp.utils.MockTemplateRenderer
import uk.gov.hmrc.play.partials.{CachedStaticHtmlPartialRetriever, FormPartialRetriever}
import uk.gov.hmrc.renderer.TemplateRenderer

  object MockStatePensionController extends StatePensionController(MockSessionCache,
    MockCustomAuditConnector,
    MockApplicationConfig,
    MockCitizenDetailsService,
    MockMetricsService.metrics,
    MockStatePensionService,
    MockStatePensionConnection,
    MockNationalInsuranceServiceViaNationalInsurance
  )(
  MockCachedStaticHtmlPartialRetriever,
  MockFormPartialRetriever,
MockTemplateRenderer
) {
      override implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever
  override implicit val formPartialRetriever: FormPartialRetriever = MockFormPartialRetriever
  override implicit val templateRenderer: TemplateRenderer = MockTemplateRenderer
  }
