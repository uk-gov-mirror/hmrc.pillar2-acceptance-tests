package uk.gov.hmrc.test.ui.driver

import java.io.File
import java.nio.file.{Files, Path, Paths}
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import scala.jdk.CollectionConverters._
import org.openqa.selenium.WebDriver
import org.openqa.selenium.edge.{EdgeDriver, EdgeDriverService, EdgeOptions}
import org.openqa.selenium.chrome.{ChromeDriver, ChromeOptions}
import org.openqa.selenium.firefox.{FirefoxDriver, FirefoxOptions}

object DriverManager {

  private val profileCounter = new AtomicInteger(0)
  private val maxRetries = 3

  /** Returns a configured WebDriver instance based on environment or system property. */
  def instance: WebDriver = {
    val browser = sys.props.getOrElse("browser", sys.env.getOrElse("BROWSER_IMAGE",
      throw new IllegalArgumentException("'browser' system property or BROWSER_IMAGE env var must be set"))).toLowerCase

    val headless = sys.env.get("BROWSER_OPTION_HEADLESS")
      .orElse(sys.props.get("headless"))
      .getOrElse("true")
      .toBoolean

    browser match {
      case "edge"   => startEdge(headless)
      case "chrome" => startChrome(headless)
      case "firefox"=> startFirefox(headless)
      case other    => throw new IllegalArgumentException(s"Unsupported browser: $other")
    }
  }

  private def startEdge(headless: Boolean): WebDriver = {
    val driverPath = sys.env.get("WEBDRIVER_EDGE_DRIVER")
      .orElse(sys.props.get("webdriver.edge.driver"))
      .getOrElse(throw new IllegalArgumentException(
        "System property 'webdriver.edge.driver' or env var WEBDRIVER_EDGE_DRIVER must be set"
      ))

    val edgeBinary = sys.env.get("EDGE_BINARY").orElse(sys.props.get("edge.binary"))
    val service = new EdgeDriverService.Builder().usingDriverExecutable(new File(driverPath)).build()

    logExistingProfiles()

    var lastEx: Throwable = null
    for (attempt <- 1 to maxRetries) {
      val profileDir = createProfileDir()
      println(s"[DriverManager] [Attempt $attempt] Starting EdgeDriver with profile: $profileDir")
      val edgeOptions = new EdgeOptions()
      if (headless) edgeOptions.addArguments("--headless=new")
      edgeOptions.addArguments(
        "--no-first-run",
        "--no-default-browser-check",
        "--disable-dev-shm-usage",
        "--disable-gpu",
        "--remote-allow-origins=*",
        s"--user-data-dir=${profileDir.toAbsolutePath}"
      )
      edgeBinary.foreach(edgeOptions.setBinary)

      try {
        val driver = new EdgeDriver(service, edgeOptions)
        println(s"[DriverManager] ✅ EdgeDriver started successfully on attempt $attempt")
        sys.addShutdownHook {
          println(s"[DriverManager] 🧹 Cleaning up profile on shutdown: $profileDir")
          cleanupProfile(profileDir)
        }
        return driver
      } catch {
        case ex: Throwable =>
          lastEx = ex
          println(s"[DriverManager] ❌ Failed to start EdgeDriver on attempt $attempt: ${ex.getMessage}")
          cleanupProfile(profileDir)
      }
    }

    throw new RuntimeException(s"Failed to start EdgeDriver after $maxRetries attempts", lastEx)
  }

  private def startChrome(headless: Boolean): WebDriver = {
    val chromeOptions = new ChromeOptions()
    if (headless) chromeOptions.addArguments("--headless=new")
    new ChromeDriver(chromeOptions)
  }

  private def startFirefox(headless: Boolean): WebDriver = {
    val firefoxOptions = new FirefoxOptions()
    if (headless) firefoxOptions.addArguments("--headless=new")
    new FirefoxDriver(firefoxOptions)
  }

  private def createProfileDir(): Path = {
    val dir = Files.createTempDirectory(s"edge-profile-${UUID.randomUUID()}")
    profileCounter.incrementAndGet()
    println(s"[DriverManager] Created Edge profile dir: $dir")
    dir
  }

  private def logExistingProfiles(): Unit = {
    val tmpDir = Paths.get(System.getProperty("java.io.tmpdir"))
    if (Files.exists(tmpDir)) {
      val profiles = Files.list(tmpDir).iterator().asScala
        .filter(p => p.getFileName.toString.startsWith("edge-profile-")).toList
      if (profiles.isEmpty) println("[DriverManager] No existing edge-profile-* directories.")
      else {
        println("[DriverManager] Existing edge-profile-* directories:")
        profiles.foreach(p => println(s"  - $p"))
      }
    }
  }

  private def cleanupProfile(dir: Path): Unit = {
    try {
      deleteRecursively(dir.toFile)
      println(s"[DriverManager] Deleted Edge profile dir: $dir")
    } catch {
      case ex: Exception => println(s"[DriverManager] Failed to delete profile dir $dir: ${ex.getMessage}")
    }
  }

  private def deleteRecursively(file: File): Unit = {
    if (file.isDirectory) file.listFiles().foreach(deleteRecursively)
    if (file.exists() && !file.delete())
      println(s"[DriverManager] Warning: could not delete ${file.getAbsolutePath}")
  }
}
