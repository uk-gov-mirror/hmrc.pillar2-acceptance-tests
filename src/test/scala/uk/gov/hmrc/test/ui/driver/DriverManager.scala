package uk.gov.hmrc.test.ui.driver

import java.io.File
import java.nio.file.Files
import java.util.UUID
import org.openqa.selenium.WebDriver
import org.openqa.selenium.edge.{EdgeDriver, EdgeDriverService, EdgeOptions}
import org.openqa.selenium.chrome.{ChromeDriver, ChromeOptions}
import org.openqa.selenium.firefox.{FirefoxDriver, FirefoxOptions}

object DriverManager {

  lazy val instance: WebDriver = {
    val browser = sys.props.getOrElse(
      "browser",
      throw new IllegalArgumentException("'browser' system property must be set")
    )

    browser match {

      case "edge" =>
        val edgeBinary = sys.props.get("edge.binary")
        val driverPath = sys.props.getOrElse(
          "webdriver.edge.driver",
          throw new IllegalArgumentException("System property 'webdriver.edge.driver' must be set")
        )

        val edgeOptions = new EdgeOptions()

        // Ensure headless mode can be overridden via system property (default: true)
        if (sys.props.getOrElse("headless", "true").toBoolean) {
          edgeOptions.addArguments("--headless=new")
        }

        // Generate a unique directory per run to prevent session clashes
        val buildId = sys.env.getOrElse("BUILD_ID", "local")
        val uniqueProfileDir = s"/tmp/edge-profile-$buildId-${System.currentTimeMillis}-${UUID.randomUUID().toString}"
        new File(uniqueProfileDir).mkdirs()

        println(s"[DriverManager] Launching Edge with unique user-data-dir: $uniqueProfileDir")
        edgeOptions.addArguments(s"--user-data-dir=$uniqueProfileDir")

        // Use provided binary if available
        edgeBinary.foreach(edgeOptions.setBinary)

        val service = new EdgeDriverService.Builder()
          .usingDriverExecutable(new File(driverPath))
          .build()

        new EdgeDriver(service, edgeOptions)

      case "chrome" =>
        val chromeOptions = new ChromeOptions()
        if (sys.props.getOrElse("headless", "true").toBoolean) {
          chromeOptions.addArguments("--headless=new")
        }
        new ChromeDriver(chromeOptions)

      case "firefox" =>
        val firefoxOptions = new FirefoxOptions()
        if (sys.props.getOrElse("headless", "true").toBoolean) {
          firefoxOptions.addArguments("--headless=new")
        }
        new FirefoxDriver(firefoxOptions)

      case other =>
        throw new IllegalArgumentException(s"Unsupported browser: $other")
    }
  }
}
