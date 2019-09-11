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

package uk.gov.hmrc.nisp.fixtures

import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.auth.core.retrieve.{ItmpAddress, Name}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.nisp.controllers.auth.NispAuthedUser
import uk.gov.hmrc.nisp.helpers.TestAccountBuilder
import uk.gov.hmrc.nisp.models.UserName
import uk.gov.hmrc.nisp.models.citizen.CitizenDetailsResponse

object NispAuthedUserFixture {

  def user(nino: Nino): NispAuthedUser = {

    val citizenDetailsResponse: CitizenDetailsResponse = TestAccountBuilder.directJsonResponse(nino, "citizen-details")

    NispAuthedUser(nino,
      confidenceLevel =  ConfidenceLevel.L200,
      dateOfBirth =  citizenDetailsResponse.person.dateOfBirth,
      name = UserName(Name(citizenDetailsResponse.person.firstName,
        citizenDetailsResponse.person.lastName)),
      address = citizenDetailsResponse.address)
  }

}
