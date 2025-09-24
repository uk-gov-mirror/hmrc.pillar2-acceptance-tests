package uk.gov.hmrc.test.ui.driver

import java.io.File
import org.openqa.selenium.WebDriver
import org.openqa.selenium.edge.{EdgeDriver, EdgeDriverService, EdgeOptions}
import org.openqa.selenium.chrome.{ChromeDriver, ChromeOptions}
import org.openqa.selenium.firefox.{FirefoxDriver, FirefoxOptions}
import java.nio.file.Files


object DriverManager {

  lazy val instance: WebDriver = {
    sys.props.getOrElse("browser", throw new IllegalArgumentException("'browser' system property must be set")) match {

      case "edge" =>
        val edgeBinary = sys.props.get("edge.binary")
        val driverPath = sys.props.getOrElse(
          "webdriver.edge.driver",
          throw new IllegalArgumentException("System property 'webdriver.edge.driver' must be set")
        )

        val edgeOptions = new EdgeOptions()

        // Ensure headless mode can be overridden via system property
        if (sys.props.getOrElse("headless", "true").toBoolean) {
          edgeOptions.addArguments("--headless=new")
        }

        // Add unique user data dir to avoid session clashes
        val tempProfileDir = Files.createTempDirectory("edge-profile-").toAbsolutePath.toString
        edgeOptions.addArguments(s"--user-data-dir=$tempProfileDir")

        edgeBinary.foreach(edgeOptions.setBinary)

        val service = new EdgeDriverService.Builder()
          .usingDriverExecutable(new File(driverPath))
          .build()

        new EdgeDriver(service, edgeOptions)

      case "chrome" =>
        val chromeOptions = new ChromeOptions()
        if (sys.props.get("headless").exists(_.toBoolean)) {
          chromeOptions.addArguments("--headless=new")
        }
        new ChromeDriver(chromeOptions)

      case "firefox" =>
        val firefoxOptions = new FirefoxOptions()
        if (sys.props.get("headless").exists(_.toBoolean)) {
          firefoxOptions.addArguments("--headless=new")
        }
        new FirefoxDriver(firefoxOptions)

      case other =>
        throw new IllegalArgumentException(s"Unsupported browser: $other")
    }
  }
}
