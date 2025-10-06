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

        val service = new EdgeDriverService.Builder()
          .usingDriverExecutable(new File(driverPath))
          .build()

        println(s"[DriverManager] --- BEFORE starting Edge ---")
        logExistingProfiles()

        val driver = startEdgeWithRetry(service, edgeBinary, headless, maxRetries = 3)

        println(s"[DriverManager] --- AFTER Edge started ---")
        logExistingProfiles()

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

  private def startEdgeWithRetry(
                                  service: EdgeDriverService,
                                  edgeBinary: Option[String],
                                  headless: Boolean,
                                  maxRetries: Int
                                ): WebDriver = {
    var lastEx: Throwable = null

    for (attempt <- 1 to maxRetries) {

      val profileDir = createProfileDir()
      println(s"[DriverManager] [Attempt $attempt] Trying EdgeDriver with profile: $profileDir")

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
        println(s"[DriverManager] ✅ EdgeDriver started successfully on attempt $attempt with $profileDir")

        // Cleanup hook on JVM exit
        sys.addShutdownHook {
          println(s"[DriverManager] 🧹 Cleaning up profile on shutdown: $profileDir")
          cleanupProfile(profileDir)
        }

        return driver
      } catch {
        case ex: Throwable =>
          lastEx = ex
          println(s"[DriverManager] ❌ Failed to start EdgeDriver on attempt $attempt: ${ex.getMessage}")
          ex.printStackTrace()
          cleanupProfile(profileDir)
      }
    }

    throw new RuntimeException(s"Failed to start EdgeDriver after $maxRetries attempts", lastEx)
  }

  private def createProfileDir(): Path = {
    val buildId = sys.env.getOrElse("BUILD_TAG", "local")
    val count = profileCounter.incrementAndGet()
    val dir = Paths.get(s"/tmp/edge-profile-$buildId-${UUID.randomUUID()}")
    Files.createDirectories(dir)
    println(s"[DriverManager] Created Edge profile dir #$count: $dir")
    dir
  }

  private def logExistingProfiles(): Unit = {
    val tmpDir = Paths.get("/tmp")
    if (Files.exists(tmpDir)) {
      val profiles = Files.list(tmpDir).iterator().asScala
        .filter(p => p.getFileName.toString.startsWith("edge-profile-"))
        .toList
      if (profiles.isEmpty)
        println("[DriverManager] No edge-profile-* directories currently exist.")
      else {
        println("[DriverManager] Existing edge-profile-* directories:")
        profiles.foreach(p => println(s"  - $p"))
      }
    } else println("[DriverManager] /tmp does not exist or is not accessible.")
  }

  private def cleanupProfile(dir: Path): Unit = {
    try {
      deleteRecursively(dir.toFile)
      println(s"[DriverManager] Deleted Edge profile dir: $dir")
    } catch {
      case ex: Exception =>
        println(s"[DriverManager] Failed to delete profile dir $dir: ${ex.getMessage}")
    }
  }

  private def deleteRecursively(file: File): Unit = {
    if (file.isDirectory) file.listFiles().foreach(deleteRecursively)
    if (file.exists() && !file.delete()) println(s"[DriverManager] Warning: could not delete ${file.getAbsolutePath}")
  }
}
