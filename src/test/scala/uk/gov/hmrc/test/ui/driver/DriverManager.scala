package uk.gov.hmrc.test.ui.driver

import java.io.File
import java.nio.file.{Files, Path}
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

import org.openqa.selenium.WebDriver
import org.openqa.selenium.edge.{EdgeDriver, EdgeDriverService, EdgeOptions}
import org.openqa.selenium.chrome.{ChromeDriver, ChromeOptions}
import org.openqa.selenium.firefox.{FirefoxDriver, FirefoxOptions}

object DriverManager {

  // Counter to track how many profile dirs are created
  private val profileCounter = new AtomicInteger(0)

  def instance: WebDriver = {
    val browser = sys.props.getOrElse(
      "browser",
      sys.env.getOrElse(
        "BROWSER_IMAGE",
        throw new IllegalArgumentException("'browser' system property or BROWSER_IMAGE env var must be set")
      )
    ).toLowerCase

    def headless = sys.env.get("BROWSER_OPTION_HEADLESS")
      .orElse(sys.props.get("headless"))
      .getOrElse("true")
      .toBoolean

    browser match {

      case "edge" =>
        val edgeBinary = sys.env.get("EDGE_BINARY").orElse(sys.props.get("edge.binary"))
        val driverPath = sys.env.get("WEBDRIVER_EDGE_DRIVER")
          .orElse(sys.props.get("webdriver.edge.driver"))
          .getOrElse(throw new IllegalArgumentException(
            "System property 'webdriver.edge.driver' or env var WEBDRIVER_EDGE_DRIVER must be set"
          ))

        val edgeOptions = new EdgeOptions()
        if (headless) edgeOptions.addArguments("--headless=new")

        val uniqueProfileDir: Path = Files.createTempDirectory(s"edge-profile-${UUID.randomUUID()}")
        val count = profileCounter.incrementAndGet()
        println(s"[DriverManager] Profile dir #$count created: $uniqueProfileDir")

        edgeOptions.addArguments(s"--user-data-dir=${uniqueProfileDir.toAbsolutePath}")

        edgeBinary.foreach(edgeOptions.setBinary)

        val service = new EdgeDriverService.Builder()
          .usingDriverExecutable(new File(driverPath))
          .build()

        val driver = new EdgeDriver(service, edgeOptions)

        sys.addShutdownHook {
          try {
            deleteRecursively(uniqueProfileDir.toFile)
            println(s"[DriverManager] Deleted profile dir: $uniqueProfileDir")
          } catch {
            case ex: Exception => println(s"[DriverManager] Failed to delete temp profile: ${ex.getMessage}")
          }
        }

        driver

      case "chrome" =>
        val chromeOptions = new ChromeOptions()
        if (headless) chromeOptions.addArguments("--headless=new")
        new ChromeDriver(chromeOptions)

      case "firefox" =>
        val firefoxOptions = new FirefoxOptions()
        if (headless) firefoxOptions.addArguments("--headless=new")
        new FirefoxDriver(firefoxOptions)

      case other =>
        throw new IllegalArgumentException(s"Unsupported browser: $other")
    }
  }

  private def deleteRecursively(file: File): Unit = {
    if (file.isDirectory) file.listFiles().foreach(deleteRecursively)
    file.delete()
  }
}
