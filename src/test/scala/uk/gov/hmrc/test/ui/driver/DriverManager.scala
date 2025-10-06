package uk.gov.hmrc.test.ui.driver

import java.io.File
import java.nio.file.{Files, Path, Paths}
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import org.openqa.selenium.WebDriver
import org.openqa.selenium.edge.{EdgeDriver, EdgeDriverService, EdgeOptions}
import org.openqa.selenium.chrome.{ChromeDriver, ChromeOptions}
import org.openqa.selenium.firefox.{FirefoxDriver, FirefoxOptions}

object DriverManager {

  // Global counter to help track unique profile directories across threads
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

        // Option A: Logging + retry wrapper
        val uniqueProfileDir: Path = createProfileDirWithRetry()

        // Option B: Force separate temp directory to isolate Jenkins builds
        edgeOptions.addArguments(s"--user-data-dir=${uniqueProfileDir.toAbsolutePath}")

        // Option C: Add stability-related flags for Edge in CI environments
        edgeOptions.addArguments(
          "--no-first-run",
          "--no-default-browser-check",
          "--disable-dev-shm-usage",
          "--disable-gpu",
          "--remote-allow-origins=*"
        )

        edgeBinary.foreach(edgeOptions.setBinary)

        val service = new EdgeDriverService.Builder()
          .usingDriverExecutable(new File(driverPath))
          .build()

        println(s"[DriverManager] Starting EdgeDriver with profile dir: ${uniqueProfileDir.toAbsolutePath}")
        val driver = new EdgeDriver(service, edgeOptions)

        // Register shutdown cleanup
        sys.addShutdownHook {
          try {
            println(s"[DriverManager] Cleaning up profile dir: ${uniqueProfileDir.toAbsolutePath}")
            deleteRecursively(uniqueProfileDir.toFile)
          } catch {
            case ex: Exception =>
              println(s"[DriverManager] Failed to delete temp profile: ${ex.getMessage}")
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

  /** Option A — Create unique Edge user profile dir with retry + log */
  private def createProfileDirWithRetry(maxRetries: Int = 3): Path = {
    val buildId = sys.env.getOrElse("BUILD_TAG", "local")
    var lastEx: Exception = null

    for (attempt <- 1 to maxRetries) {
      val count = profileCounter.incrementAndGet()
      val dir = Paths.get(s"/tmp/edge-profile-$buildId-${UUID.randomUUID().toString}")
      try {
        Files.createDirectories(dir)
        val threadName = Thread.currentThread().getName
        println(s"[DriverManager] [Attempt $attempt] Created profile dir #$count by thread '$threadName': $dir")
        return dir
      } catch {
        case ex: Exception =>
          lastEx = ex
          println(s"[DriverManager] [Attempt $attempt] Failed to create Edge profile dir: ${ex.getMessage}")
      }
    }
    throw new RuntimeException(s"Failed to create Edge profile directory after $maxRetries attempts", lastEx)
  }

  /** Recursively delete profile directory */
  private def deleteRecursively(file: File): Unit = {
    if (file.isDirectory) file.listFiles().foreach(deleteRecursively)
    if (!file.delete()) {
      println(s"[DriverManager] Warning: failed to delete ${file.getAbsolutePath}")
    }
  }
}
