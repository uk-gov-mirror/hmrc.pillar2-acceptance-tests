package uk.gov.hmrc.selenium.webdriver

import java.io.File
import org.openqa.selenium.WebDriver
import org.openqa.selenium.edge.{EdgeDriver, EdgeDriverService, EdgeOptions}
import org.openqa.selenium.chrome.{ChromeDriver, ChromeOptions}
import org.openqa.selenium.firefox.{FirefoxDriver, FirefoxOptions, GeckoDriverService}

object BrowserDriver {

  lazy val instance: WebDriver = {
    sys.props.getOrElse("browser", throw new IllegalArgumentException("'browser' system property must be set")) match {

      case "edge" =>
        // Get binary and driver paths from system properties
        val edgeBinary = sys.props.get("edge.binary")
        val driverPath = sys.props.getOrElse("webdriver.edge.driver",
          throw new IllegalArgumentException("System property 'webdriver.edge.driver' must be set"))

        val edgeOptions = new EdgeOptions()
        edgeOptions.addArguments("--headless=new") // optional headless
        edgeBinary.foreach(edgeOptions.setBinary)

        val service = new EdgeDriverService.Builder()
          .usingDriverExecutable(new File(driverPath))
          .build()

        new EdgeDriver(service, edgeOptions)

      case "chrome" =>
        val chromeOptions = new ChromeOptions()
        chromeOptions.addArguments("--headless=new")
        new ChromeDriver(chromeOptions)

      case "firefox" =>
        val firefoxOptions = new FirefoxOptions()
        firefoxOptions.addArguments("--headless=new")
        new FirefoxDriver(firefoxOptions)

      case other =>
        throw new IllegalArgumentException(s"Unsupported browser: $other")
    }
  }
}
