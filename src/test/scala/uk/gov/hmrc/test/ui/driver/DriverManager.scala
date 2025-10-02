package uk.gov.hmrc.test.ui.driver

import java.io.File
import java.nio.file.{Files, Paths}
import java.util.UUID
import org.openqa.selenium.WebDriver
import org.openqa.selenium.edge.{EdgeDriver, EdgeDriverService, EdgeOptions}
import org.openqa.selenium.chrome.{ChromeDriver, ChromeOptions}
import org.openqa.selenium.firefox.{FirefoxDriver, FirefoxOptions}

object DriverManager {

  lazy val instance: WebDriver = {
    val browser = sys.props.getOrElse(
      "browser",
      sys.env.getOrElse(
        "BROWSER_IMAGE",
        throw new IllegalArgumentException(
          "'browser' system property or BROWSER_IMAGE env var must be set"
        )
      )
    ).toLowerCase

    println(s"[DriverManager] Selected browser: $browser")

    val headless = sys.env.get("BROWSER_OPTION_HEADLESS")
      .orElse(sys.props.get("headless"))
      .getOrElse("true").toBoolean

    println(s"[DriverManager] Headless mode enabled: $headless")

    browser match {
      case "edge" =>
        val edgeBinary = sys.env.get("EDGE_BINARY").orElse(sys.props.get("edge.binary"))
        val driverPath = sys.env.get("WEBDRIVER_EDGE_DRIVER")
          .orElse(sys.props.get("webdriver.edge.driver"))
          .getOrElse(
            throw new IllegalArgumentException(
              "System property 'webdriver.edge.driver' or env var WEBDRIVER_EDGE_DRIVER must be set"
            )
          )

        val edgeOptions = new EdgeOptions()
        if (headless) edgeOptions.addArguments("--headless=new")

        // Create a unique user-data-dir to avoid conflicts
        val buildId = sys.env.getOrElse("BUILD_ID", "local")
        val uniqueId = UUID.randomUUID().toString
        val uniqueProfileDir =
          Paths.get(s"/tmp/edge-profile-$buildId-${System.currentTimeMillis}-$uniqueId")
        Files.createDirectories(uniqueProfileDir)
        println(s"[DriverManager] Using Edge user-data-dir: ${uniqueProfileDir.toAbsolutePath}")
        edgeOptions.addArguments(s"--user-data-dir=${uniqueProfileDir.toAbsolutePath}")

        // Set Edge binary if specified
        edgeBinary.foreach(edgeOptions.setBinary)

        val service = new EdgeDriverService.Builder()
          .usingDriverExecutable(new File(driverPath))
          .build()

        val driver = new EdgeDriver(service, edgeOptions)

        // Cleanup user profile on JVM exit
        sys.addShutdownHook {
          try deleteRecursively(uniqueProfileDir.toFile)
          catch { case ex: Exception => println(s"Failed to delete profile: ${ex.getMessage}") }
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
