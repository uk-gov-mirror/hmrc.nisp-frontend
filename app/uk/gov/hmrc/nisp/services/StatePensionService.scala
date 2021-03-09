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

package uk.gov.hmrc.nisp.services

import com.google.inject.Inject
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import org.joda.time.{DateTime, LocalDate}
import play.api.http.Status._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, Upstream4xxResponse}
import uk.gov.hmrc.nisp.connectors.StatePensionConnector
import uk.gov.hmrc.nisp.models.{Exclusion, _}
import uk.gov.hmrc.time.CurrentTaxYear

import scala.util.matching.Regex
import scala.concurrent.{ExecutionContext, Future}

class StatePensionService @Inject()(statePensionConnector: StatePensionConnector)
                                   (implicit executor: ExecutionContext) extends CurrentTaxYear {

  val exclusionCodeDead: String = "EXCLUSION_DEAD"
  val exclusionCodeManualCorrespondence: String = "EXCLUSION_MANUAL_CORRESPONDENCE"
  val exclusionCodeCopeProcessing: String = "EXCLUSION_COPE_PROCESSING"
  val exclusionCodeCopeProcessingFailed: String = "EXCLUSION_COPE_PROCESSING_FAILED"

  override def now: () => DateTime = () => DateTime.now(ukTime)

  def getSummary(nino: Nino)(implicit hc: HeaderCarrier): Future[Either[StatePensionExclusionFiltered, StatePension]] = {
    statePensionConnector.getStatePension(nino)
      .map {
        case Right(statePension) => Right(statePension)

        case Left(spExclusion) => Left(StatePensionExclusionFiltered(
          filterExclusions(spExclusion.exclusionReasons),
          spExclusion.pensionAge,
          spExclusion.pensionDate,
          spExclusion.statePensionAgeUnderConsideration
        ))
      }
      .recover {
        case ex: Upstream4xxResponse if ex.upstreamResponseCode == FORBIDDEN && ex.message.contains(exclusionCodeDead) =>
          Left(StatePensionExclusionFiltered(Exclusion.Dead))
        case ex: Upstream4xxResponse if ex.upstreamResponseCode == FORBIDDEN && ex.message.contains(exclusionCodeManualCorrespondence) =>
          Left(StatePensionExclusionFiltered(Exclusion.ManualCorrespondenceIndicator))
        case ex: Upstream4xxResponse if ex.upstreamResponseCode == FORBIDDEN && ex.message.contains(exclusionCodeCopeProcessingFailed) =>
          Left(StatePensionExclusionFiltered(Exclusion.CopeProcessingFailed))
        case ex: Upstream4xxResponse if ex.upstreamResponseCode == FORBIDDEN && ex.message.contains(exclusionCodeCopeProcessing) =>
          Left(StatePensionExclusionFiltered(exclusion = Exclusion.CopeProcessing, copeDataAvailableDate = getDateWithRegex(ex.message)))
        // Case match not exhaustive
      }
  }

  def yearsToContributeUntilPensionAge(earningsIncludedUpTo: LocalDate, finalRelevantYearStart: Int): Int = {
    finalRelevantYearStart - taxYearFor(earningsIncludedUpTo).startYear
  }

  private[services] def filterExclusions(exclusions: List[Exclusion]): Exclusion = {
    if (exclusions.contains(Exclusion.Dead)) {
      Exclusion.Dead
    } else if (exclusions.contains(Exclusion.ManualCorrespondenceIndicator)) {
      Exclusion.ManualCorrespondenceIndicator
    } else if (exclusions.contains(Exclusion.PostStatePensionAge)) {
      Exclusion.PostStatePensionAge
    } else if (exclusions.contains(Exclusion.AmountDissonance)) {
      Exclusion.AmountDissonance
    } else if (exclusions.contains(Exclusion.IsleOfMan)) {
      Exclusion.IsleOfMan
    } else if (exclusions.contains(Exclusion.MarriedWomenReducedRateElection)) {
      Exclusion.MarriedWomenReducedRateElection
    } else {
      throw new RuntimeException(s"Un-accounted for exclusion in NispConnectionNI: $exclusions")
    }
  }

  private def getDateWithRegex(copeResponse: String): Option[LocalDate] = {
    val upstream4xxResponseDateCapturingRegex: Regex = """(?:.*)(?:'\{"errorCode":"EXCLUSION_COPE_PROCESSING","copeDataAvailableDate":\")(\d{4}-\d{2}-\d{2})(?:\"}')""".r
    val dateFormatter: DateTimeFormatter = DateTimeFormat.forPattern("d MMMM y")

    copeResponse match {
      case upstream4xxResponseDateCapturingRegex(copeResponseDate) => {
        println(new LocalDate(copeResponseDate).toString(dateFormatter))

        // WIP
        val dateAsString: String = new LocalDate(copeResponseDate).toString(dateFormatter)
        val dateFromString = new LocalDate(dateAsString)
        Some(dateFromString)
      }
      case _ => None
    }
  }
}