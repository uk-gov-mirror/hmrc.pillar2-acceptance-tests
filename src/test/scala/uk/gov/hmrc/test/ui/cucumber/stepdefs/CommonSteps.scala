/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.test.ui.cucumber.stepdefs

import io.cucumber.scala.{EN, ScalaDsl}
import org.openqa.selenium.By
import org.scalatest.matchers.should.Matchers
import uk.gov.hmrc.test.ui.cucumber.Input.{clickSubmit, getTextOf}
import uk.gov.hmrc.test.ui.cucumber.Nav.{isVisible, navigateTo}
import uk.gov.hmrc.test.ui.cucumber._
import uk.gov.hmrc.test.ui.driver.BrowserDriver
import uk.gov.hmrc.test.ui.pages._

class CommonSteps extends EN with ScalaDsl with BrowserDriver with Matchers {

  Given("""^(.*) logs in to register for Pillar2$""") { name: String =>
    name match {
      case "Organisation User" => AuthLoginPage.loginWithUser()
      case "Individual User"   => AuthLoginPage.loginAsInd()
      case "Agent User"        => AuthLoginPage.loginAsAgent()
      case "Assistant User"    => AuthLoginPage.loginAssistant()
    }
  }

  Given("""^(.*) logs in to register for Pillar2 Agent service$""") { name: String =>
    name match {
      case "Organisation User" => AuthLoginPage.loginAsOrgToASA()
      case "Individual User"   => AuthLoginPage.loginAsIndToASA()
      case "Assistant User"    => AuthLoginPage.loginAsAssistantToASA()

    }
  }

  Given("""^(.*) logs in to subscribe for Pillar2$""") { name: String =>
    name match {
      case "Organisation User" => AuthLoginPage.loginToSubscribe()
      case _                   => AuthLoginPage.loginToSubscribe()
    }
  }

  Given("""^(.*) logs in with BTA for Pillar2$""") { name: String =>
    name match {
      case "Organisation User" => AuthLoginPage.loginUsingBta()
      case _                   => AuthLoginPage.loginToSubscribe()
    }
  }

  Given("""^.* logs in without Pillar2 enrolment$""") {
    AuthLoginPage.loginToUPE()
  }

  Given("""^.* logs in and navigates to RFM start page without Pillar2 enrolment with groupId (.*)$""") { groupId: String =>
    AuthLoginPage.loginToRFMWithGroupId(groupId)
  }

  Given("""^(.*) logs in as upe with credId (.*) for Pillar2$""") { (name: String, credId: String) =>
    name match {
      case "Organisation User" => AuthLoginPage.loginToUPEWithCredID(credId)
      case _                   => AuthLoginPage.loginToUPEWithCredID(credId)
    }
  }

  Given("""^(.*) logs in with credId (.*) for Pillar2$""") { (name: String, credId: String) =>
    name match {
      case "Organisation User" => AuthLoginPage.loginAsUserWithCredId(credId)
      case _                   => AuthLoginPage.loginAsUserWithCredId(credId)
    }
  }

  Given("""^(.*) logs in to upe org page with CredID (.*) for Pillar2$""") { (name: String, credId: String) =>
    name match {
      case "Organisation User" => AuthLoginPage.loginToOrgWithCredID(credId)
      case _                   => AuthLoginPage.loginToOrgWithCredID(credId)
    }
  }

  Given("""^(.*) logs in to nfm org page with CredID (.*) for Pillar2$""") { (name: String, credId: String) =>
    name match {
      case "Organisation User" => AuthLoginPage.loginToNfmOrgWithCredID(credId)
      case _                   => AuthLoginPage.loginToNfmOrgWithCredID(credId)
    }

  }

  Given("""^(.*) logs in to upe registered in UK page with CredID (.*) for Pillar2$""") { (name: String, credId: String) =>
    name match {
      case "Organisation User" => AuthLoginPage.loginToRegWithCredID(credId)
      case _                   => AuthLoginPage.loginToRegWithCredID(credId)
    }
  }

  Given("""^(.*) logs in to upe name page with CredID (.*) for Pillar2$""") { (name: String, credId: String) =>
    name match {
      case "Organisation User" => AuthLoginPage.loginToUPEName(credId)
      case _                   => AuthLoginPage.loginToUPEName(credId)
    }
  }

  Given("""^(.*) logs in to nfm name page with CredID (.*) for Pillar2$""") { (name: String, credId: String) =>
    name match {
      case "Organisation User" => AuthLoginPage.loginToNFMNameWithCredID(credId)
      case _                   => AuthLoginPage.loginToNFMNameWithCredID(credId)
    }
  }

  Given("""^Organisation User navigates to (.*) check your answer page with credId (.*)$""") { (name: String, credId: String) =>
    name match {
      case "UPE"                      => AuthLoginPage.loginToCA(credId)
      case "NFM"                      => AuthLoginPage.loginToNFMCA(credId)
      case "FD"                       => AuthLoginPage.loginToFDCA(credId)
      case "Contact Details"          => AuthLoginPage.loginToCDCA(credId)
      case "Final Check Your Answers" => AuthLoginPage.loginToFinalCA(credId)
    }
  }

  Then("""^I clear the cache$""") {
    Nav.navigateTo("http://localhost:10050/report-pillar2-top-up-taxes/test-only/eligibility/clear-session")
  }

  Then("""^The Heading should be (.*)$""") { header: String =>
    Check.checkH1(header)
  }

  Then("""^The Body content should be (.*)$""") { text: String =>
    Check.checkBodyText(text)
  }

  When("""^I click (.*)$""") { button: String =>
    button match {
      case "Continue button" => clickSubmit()
      case "on Continue button" => InitialGuidancePage.clickContinue()
    }
  }

  And("""^I click radio button for (.*)$""") { accountingPeriod: String =>
    BtnMultipleAccountingPage.selectAccountingPeriod(accountingPeriod)
  }

  When("""^I click on Country selected""") { () =>
    UPEAddressPage.clickCountrySelected()
  }

  Given("""^I am on (.*) Page$""") { page: String =>
    page match {
      case "UPE EQ" =>
        navigateTo(UPEEQPage.url)
        Wait.waitForElementToPresentByCssSelector(UPEEQPage.eqForm)
        isVisible(By.cssSelector(UPEEQPage.eq)) shouldBe true
      case "Business activity EQ" =>
        navigateTo(BusinessActivityEQPage.url)
        Wait.waitForElementToPresentByCssSelector(BusinessActivityEQPage.eqForm)
        isVisible(By.cssSelector(BusinessActivityEQPage.eq)) shouldBe true
      case "Global gross revenue" =>
        navigateTo(GlobalGrossRevenueEQPage.url)
        Wait.waitForElementToPresentByCssSelector(GlobalGrossRevenueEQPage.eqForm)
        isVisible(By.cssSelector(GlobalGrossRevenueEQPage.eq)) shouldBe true
      case "NFM registration failed error" =>
        navigateTo(NFMGRSRegistrationFailedErrorPage.url)
        Wait.waitForElementToPresentByCssSelector(NFMGRSRegistrationFailedErrorPage.content)
        isVisible(By.cssSelector(NFMGRSRegistrationFailedErrorPage.header)) shouldBe true
      case "NFM registration not called error" =>
        navigateTo(NFMGRSRegistrationNotCalledErrorPage.url)
        Wait.waitForElementToPresentByCssSelector(NFMGRSRegistrationNotCalledErrorPage.content)
        isVisible(By.cssSelector(NFMGRSRegistrationNotCalledErrorPage.header)) shouldBe true
      case "UPE registration failed error" =>
        navigateTo(UPEGRSRegistrationFailedErrorPage.url)
        Wait.waitForElementToPresentByCssSelector(UPEGRSRegistrationFailedErrorPage.content)
        isVisible(By.cssSelector(UPEGRSRegistrationFailedErrorPage.header)) shouldBe true
      case "UPE registration not called error" =>
        navigateTo(UPEGRSRegistrationNotCalledErrorPage.url)
        Wait.waitForElementToPresentByCssSelector(UPEGRSRegistrationNotCalledErrorPage.content)
        isVisible(By.cssSelector(UPEGRSRegistrationNotCalledErrorPage.header)) shouldBe true
    }
  }

  And("""^I should see error message (.*) on the Contact details display Page$""") { (error: String) =>
    Wait.waitForTagNameToBeRefreshed("h1")
    Wait.waitForElementToPresentByCssSelector(ContactDetailsDisplayPage.errorSummary)

    Wait.waitForElementToPresentByCssSelector(ContactDetailsDisplayPage.errorLink)
    getTextOf(By cssSelector ContactDetailsDisplayPage.errorLink) should be(error)

    Wait.waitForElementToPresentByCssSelector(ContactDetailsDisplayPage.errorMessage)
    getTextOf(By cssSelector ContactDetailsDisplayPage.errorMessage) should include(error)
  }

  Then("""^The caption must be (.*)$""") { caption: String =>
    Wait.waitForElementToPresentByCssSelector(InitialGuidancePage.caption)
    assert(getTextOf(By.cssSelector(InitialGuidancePage.caption)).equals(caption))
  }

  And("""^I click (.*) link$""") { (linkText: String) =>
    Input.clickByLinkText(linkText)
  }

  And("""^I select option (.*) and continue to next$""") { (option: String) =>
    option match {
      case "Yes" => Input.clickById("value_0")
      case "No"  => Input.clickById("value_1")
    }
    InitialGuidancePage.clickContinue()
  }

  And("""^I select (.*) option and continue to next$""") { (option: String) =>
    option match {
      case "Yes" => Input.clickById("nominateFilingMember_0")
      case "No"  => Input.clickById("nominateFilingMember_1")
    }
    InitialGuidancePage.clickContinue()
  }

  And("""^I click the browser back button$""") { () =>
    Nav.browserBack()
  }

  Then("""^I should be navigated to (.*) page$""") { (text: String) =>
    Wait.waitForTagNameToBeRefreshed("h1")
    assert(driver.findElement(By.cssSelector(UPEPage.sendYourFeedback)).getText.contains(text))
  }

  When("""^(.*) User logs in with existing entity group (.*), (.*) and (.*) for Pillar2 service$""") {
    (userType: String, enrolmentKey: String, identifierName: String, identifierValue: String) =>
      userType match {
        case "Organisation" => AuthLoginPage.loginWithExistingEntity(enrolmentKey, identifierName, identifierValue)
        case "Agent"        => AuthLoginPage.agentLoginWithExistingEntity(enrolmentKey, identifierName, identifierValue)
      }
  }

  When("""^I add delegated enrolment with (.*), (.*), (.*) and (.*) for Pillar2 service$""") {
    (enrolmentKey: String, identifierName: String, identifierValue: String, authRule: String) =>
      AuthLoginPage.addDelegatedEnrolment(enrolmentKey, identifierName, identifierValue, authRule)
  }

  When("""^I refresh the page$""") { () =>
    driver.navigate().refresh()
  }

  When("""^I refresh the registration in progress page$""") { () =>
    val count = 5
    var i     = 0
    while (i < count) {
      driver.navigate().refresh()
      i += 1
    }

  }

  Given("""^I access random page$""") { () =>
    Nav.navigateTo(AuthLoginPage.incorrectUrl)
  }

  Then("""^I can see (.*) link$""") { (linkText: String) =>
    linkText match {
      case "Print this page" =>
        Wait.waitForElementToPresentByCssSelector(RegistrationConfirmationPage.printThisPage)
        assert(driver.findElement(By.cssSelector(RegistrationConfirmationPage.printThisPage)).getText.contains(linkText))
      case "Agent Services Account" =>
        Wait.waitForElementToPresentByCssSelector(DashboardPage.ASALink)
        assert(driver.findElement(By.cssSelector(DashboardPage.ASALink)).getText.contains(linkText))
      case "Sign out" =>
        Wait.waitForElementToPresentByCssSelector(RepaymentConfirmationPage.signOut)
        assert(driver.findElement(By.cssSelector(RepaymentConfirmationPage.signOut)).getText.contains(linkText))
    }
  }

  And("""^I should see (.*) link on (.*)$""") { (linkText: String, page: String) =>
    page match {
      case "Review answers page" =>
        Wait.waitForTagNameToBeRefreshed("h1")
        Wait.waitForElementToPresentByCssSelector(ReviewAnswersPage.printThisPage)
        assert(driver.findElement(By.cssSelector(ReviewAnswersPage.printThisPage)).getText.contains(linkText))
    }
  }

  Given("""^I access the (.*) page$""") { (page: String) =>
    page match {
      case "contact details summary" =>
        Nav.navigateTo(ContactDetailsSummaryPage.url)
      case "account summary" =>
        Nav.navigateTo(AccountsSummaryPage.url)
      case "MakePayment" =>
        Nav.navigateTo(MakePaymentPage.url)
      case "repayment guidance" =>
        Nav.navigateTo(RepaymentGuidancePage.url)
      case "repayment amount" =>
        Nav.navigateTo(RepaymentAmountPage.url)
      case "repayment reason" =>
        Nav.navigateTo(RepaymentReasonPage.url)
      case "repayment method" =>
        Nav.navigateTo(RepaymentMethodPage.url)
      case "uk bank account" =>
        Nav.navigateTo(UKBankAccountPaymentPage.url)
      case "non-uk bank account" =>
        Nav.navigateTo(NonUKBankAccountPaymentPage.url)
      case "repayment contact name" =>
        Nav.navigateTo(RepaymentContactPage.url)
      case "repayment contact email" =>
        Nav.navigateTo(RepaymentContactEmailPage.url)
      case "repayment phone" =>
        Nav.navigateTo(RepaymentPhonePage.url)
      case "repayment phone input" =>
        Nav.navigateTo(RepaymentPhoneInputPage.url)
      case "repayment CYA" =>
        Nav.navigateTo(RepaymentCYAPage.url)
      case "manage contact name" =>
        Nav.navigateTo(ManageContactNamePage.url)
      case "manage second contact name" =>
        Nav.navigateTo(ManageSecondContactNamePage.url)
      case "manage contact address" =>
        Nav.navigateTo(ManageContactAddressPage.url)
      case "manage group status" =>
        Nav.navigateTo(ManageGroupStatusPage.url)
      case "manage accounting period" =>
        Nav.navigateTo(ManageAccountPeriodPage.url)
      case "repayment change amount" =>
        Nav.navigateTo(RepaymentChangeAmountPage.url)
      case "repayment change method" =>
        Nav.navigateTo(RepaymentChangeMethodPage.url)
      case "repayment change name" =>
        Nav.navigateTo(RepaymentChangeNamePage.url)
      case "transaction history" =>
        Nav.navigateTo(TransactionHistoryPage.url)

    }
  }

  Then("""^I should see (.*) CTA$""") { (pageNumber: String) =>
    pageNumber match {
      case "Next" =>
        assert(driver.findElement(By.cssSelector(TransactionHistoryPage.nextPageCTA)).isDisplayed)
      case "Previous" =>
        assert(driver.findElement(By.cssSelector(TransactionHistorySecondPage.previousPageCTA)).isDisplayed)
    }
  }

  When("""^I click (.*) CTA$""") { (pageNumber: String) =>
    pageNumber match {
      case "Next" =>
        TransactionHistoryPage.clickNext()
      case "Previous" =>
        TransactionHistorySecondPage.clickPrevious()
    }
  }
}
