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

package uk.gov.hmrc.nisp.helpers

import play.api.i18n.Lang
import play.api.mvc.Cookie

object LanguageToggle {
  val welshUnitTest = false

  def getLanguageCode: Lang = {
    if (welshUnitTest) Lang("cy")
    else Lang("en")
  }

  def getLanguageCookie: Cookie = {
    val language = if (welshUnitTest) "cy-GB" else "en-GB"
    Cookie("PLAY_LANG", language, None, "/", None, false, true)
  }
}