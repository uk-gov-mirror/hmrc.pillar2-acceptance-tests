package uk.gov.hmrc.test.ui.driver

import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.{ChromeDriver, ChromeOptions}
import org.openqa.selenium.edge.{EdgeDriver, EdgeDriverService, EdgeOptions}
import org.openqa.selenium.firefox.{FirefoxDriver, FirefoxOptions}

import java.io.File
import java.nio.file.{Files, Path, Paths}
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import scala.jdk.CollectionConverters._
import scala.util.Try

object DriverManager {

  private val profileCounter = new AtomicInteger(0)
  private val maxRetries     = 3

  def instance: WebDriver = {
    val browser = sys.props.getOrElse(
      "browser",
      sys.env.getOrElse("BROWSER_IMAGE", throw new IllegalArgumentException("'browser' system property or BROWSER_IMAGE env var must be set"))
    ).toLowerCase

    val headless = sys.env.get("BROWSER_OPTION_HEADLESS")
      .orElse(sys.props.get("headless"))
      .getOrElse("true")
      .toBoolean

    browser match {
      case "edge"    => startEdge(headless)
      case "chrome"  => startChrome(headless)
      case "firefox" => startFirefox(headless)
      case other     => throw new IllegalArgumentException(s"Unsupported browser: $other")
    }
  }

  private def startEdge(headless: Boolean): WebDriver = {
    val driverPath = sys.env.get("WEBDRIVER_EDGE_DRIVER")
      .orElse(sys.props.get("webdriver.edge.driver"))
      .getOrElse(throw new IllegalArgumentException(
        "System property 'webdriver.edge.driver' or env var WEBDRIVER_EDGE_DRIVER must be set"
      ))

    val edgeBinary = sys.env.get("EDGE_BINARY").orElse(sys.props.get("edge.binary"))
    val service = new EdgeDriverService.Builder()
      .usingDriverExecutable(new File(driverPath))
      .usingAnyFreePort()
      .withSilent(true)
      .build()

    println(s"[DriverManager] Using EdgeDriver binary: $driverPath")
    edgeBinary.foreach(b => println(s"[DriverManager] Using Edge binary override: $b"))

    cleanupOldProfiles() // 🧹 clear stale ones first

    var driver: Option[EdgeDriver] = None
    var lastError: Throwable       = null

    for (attempt <- 1 to maxRetries if driver.isEmpty) {
      val profileDir = createProfileDir()
      println(s"[DriverManager] [Attempt $attempt] Starting EdgeDriver with profile: $profileDir")
      logTmpContents("BEFORE")

      val options = new EdgeOptions()
      if (headless) options.addArguments("--headless=new")

      // 🧩 Hardened options for Jenkins/Linux CI
      options.addArguments(
        "--no-sandbox",
        "--disable-setuid-sandbox",
        "--disable-dev-shm-usage",
        "--disable-gpu",
        "--disable-software-rasterizer",
        "--disable-extensions",
        "--no-first-run",
        "--no-default-browser-check",
        "--disable-background-networking",
        "--disable-sync",
        "--disable-translate",
        "--disable-features=VizDisplayCompositor,UseOzonePlatform",
        "--remote-allow-origins=*",
        s"--user-data-dir=${profileDir.toAbsolutePath}"
      )

      edgeBinary.foreach(options.setBinary)

      try {
        val createdDriver = new EdgeDriver(service, options)
        driver = Some(createdDriver)
        println(s"[DriverManager] ✅ EdgeDriver started successfully on attempt $attempt")

        // Check it’s alive
        Try {
          createdDriver.get("about:blank")
          println(s"[DriverManager] 🌐 Edge session active (title: ${createdDriver.getTitle})")
        }.recover {
          case ex => println(s"[DriverManager] ⚠️ Unable to verify session: ${ex.getMessage}")
        }

        // Cleanup on Jenkins shutdown
        sys.addShutdownHook {
          println(s"[DriverManager] 🧹 Cleaning up Edge profile on shutdown: $profileDir")
          cleanupProfile(profileDir)
        }

      } catch {
        case ex: Throwable =>
          lastError = ex
          println(s"[DriverManager] ❌ Failed to start EdgeDriver on attempt $attempt: ${Option(ex.getMessage).getOrElse(ex.toString)}")
          ex.printStackTrace()
          cleanupProfile(profileDir)
          Thread.sleep(1500)
      } finally {
        logTmpContents("AFTER")
      }
    }

    driver.getOrElse(throw new RuntimeException(s"Failed to start EdgeDriver after $maxRetries attempts", lastError))
  }

  private def logTmpContents(stage: String): Unit = {
    Try {
      val tmpDir = Paths.get(System.getProperty("java.io.tmpdir"))
      if (Files.exists(tmpDir)) {
        val entries = Files.list(tmpDir).iterator().asScala.map(_.toString).toList
        println(s"[DriverManager] /tmp $stage:\n${entries.mkString("\n")}")
      } else println(s"[DriverManager] /tmp $stage: (no tmp dir)")
    }.recover { case ex =>
      println(s"[DriverManager] Failed to list /tmp contents: ${ex.getMessage}")
    }
  }

  private def cleanupOldProfiles(): Unit = {
    val tmpDir = Paths.get(System.getProperty("java.io.tmpdir"))
    if (Files.exists(tmpDir)) {
      Files.list(tmpDir)
        .iterator().asScala
        .filter(p => p.getFileName.toString.startsWith("edge-profile-"))
        .foreach { p =>
          Try(deleteRecursively(p.toFile)).recover {
            case ex => println(s"[DriverManager] ⚠️ Failed to delete old profile $p: ${ex.getMessage}")
          }
        }
    }
  }

  private def createProfileDir(): Path = {
    val buildId = sys.env.getOrElse("BUILD_ID", "local")
    val dir = Paths.get(System.getProperty("java.io.tmpdir"))
      .resolve(s"edge-profile-$buildId-${System.currentTimeMillis()}-${UUID.randomUUID()}")
    Files.createDirectories(dir)
    profileCounter.incrementAndGet()
    println(s"[DriverManager] Created Edge profile dir: $dir")
    dir
  }

  private def cleanupProfile(dir: Path): Unit = {
    Try(deleteRecursively(dir.toFile)).recover {
      case ex => println(s"[DriverManager] ⚠️ Failed to delete profile dir $dir: ${ex.getMessage}")
    }
  }

  private def deleteRecursively(file: File): Unit = {
    if (file.isDirectory) {
      val files = Option(file.listFiles()).getOrElse(Array.empty)
      files.foreach(deleteRecursively)
    }
    if (file.exists() && !file.delete())
      println(s"[DriverManager] ⚠️ Warning: could not delete ${file.getAbsolutePath}")
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
}
