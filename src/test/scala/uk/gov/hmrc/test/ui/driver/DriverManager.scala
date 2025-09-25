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
      throw new IllegalArgumentException("'browser' system property must be set")
    )

    println("=== JVM System Properties ===")
    sys.props.foreach { case (key, value) => println(s"$key = $value") }
    println("=== End of System Properties ===")

    browser match {

      case "edge" =>
        val edgeBinary = sys.props.get("edge.binary")
        val driverPath = sys.props.getOrElse(
          "webdriver.edge.driver",
          throw new IllegalArgumentException("System property 'webdriver.edge.driver' must be set")
        )

        val edgeOptions = new EdgeOptions()

        if (sys.props.getOrElse("headless", "true").toBoolean) {
          edgeOptions.addArguments("--headless=new")
        }

        val buildId = sys.env.getOrElse("BUILD_ID", "local")
        val uniqueId = UUID.randomUUID().toString
        val threadId = Thread.currentThread().getId
        val uniqueProfileDir = Paths.get(s"/tmp/edge-profile-$buildId-${System.nanoTime()}-$threadId-$uniqueId")

        if (Files.exists(uniqueProfileDir)) {
          deleteRecursively(uniqueProfileDir.toFile)
        }
        Files.createDirectories(uniqueProfileDir)

        println(s"[DriverManager] Launching Edge with unique user-data-dir: $uniqueProfileDir")
        edgeOptions.addArguments(s"--user-data-dir=${uniqueProfileDir.toAbsolutePath.toString}")

        sys.addShutdownHook {
          println(s"[DriverManager] Cleaning up Edge profile directory: $uniqueProfileDir")
          try {
            deleteRecursively(uniqueProfileDir.toFile)
          } catch {
            case ex: Exception =>
              println(s"[DriverManager] Failed to clean Edge profile: ${ex.getMessage}")
          }
        }

        edgeBinary.foreach(edgeOptions.setBinary)

        val service = new EdgeDriverService.Builder()
          .usingDriverExecutable(new File(driverPath))
          .usingAnyFreePort()
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

  private def deleteRecursively(file: File): Unit = {
    if (file.isDirectory) {
      Option(file.listFiles()).getOrElse(Array.empty).foreach(deleteRecursively)
    }
    if (!file.delete()) {
      println(s"[DriverManager] Warning: Failed to delete ${file.getAbsolutePath}")
    }
  }
}
