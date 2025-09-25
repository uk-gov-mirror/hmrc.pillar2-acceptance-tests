package uk.gov.hmrc.test.ui.driver

import java.io.File
import java.nio.file.{Files, Paths}
import java.util.UUID
import scala.jdk.CollectionConverters._
import org.openqa.selenium.WebDriver
import org.openqa.selenium.edge.{EdgeDriver, EdgeDriverService, EdgeOptions}
import org.openqa.selenium.chrome.{ChromeDriver, ChromeOptions}
import org.openqa.selenium.firefox.{FirefoxDriver, FirefoxOptions}

object DriverManager {

  lazy val instance: WebDriver = {
    println("========== DRIVER DEBUG START ==========")

    // 1. JVM System Properties
    println("=== JVM System Properties ===")
    sys.props.foreach { case (k, v) => println(s"$k = $v") }

    // 2. Environment Variables
    println("=== Environment Variables ===")
    sys.env.foreach { case (k, v) => println(s"$k = $v") }

    // 3. Working Directory
    println(s"Working Directory: ${new java.io.File(".").getAbsolutePath}")

    // 4. Existing /tmp edge-profile-* directories before launch
    val tmpDir = Paths.get("/tmp")
    if (Files.exists(tmpDir)) {
      val edgeDirs = Files.list(tmpDir).iterator().asScala
        .filter(path => path.getFileName.toString.startsWith("edge-profile-"))
        .toList

      println("=== /tmp edge-profile-* folders BEFORE starting ===")
      edgeDirs.foreach(dir => println(s"Found: ${dir.toAbsolutePath}"))
      println("=== End of /tmp listing ===")
    } else {
      println("WARNING: /tmp directory does not exist or cannot be accessed!")
    }

    // Determine browser
    val browser = sys.props.getOrElse(
      "browser",
      throw new IllegalArgumentException("'browser' system property must be set")
    )
    println(s"[DriverManager] Selected browser: $browser")

    // Ensure EdgeDriver logs are verbose
    System.setProperty("webdriver.edge.verboseLogging", "true")
    System.setProperty("webdriver.edge.logfile", "/tmp/edge-driver.log")

    browser match {

      case "edge" =>
        val edgeBinary = sys.props.get("edge.binary")
        val driverPath = sys.props.getOrElse(
          "webdriver.edge.driver",
          throw new IllegalArgumentException("System property 'webdriver.edge.driver' must be set")
        )

        val edgeOptions = new EdgeOptions()
        val addedArgs = scala.collection.mutable.ListBuffer[String]()

        // Enable headless mode if property is true
        val isHeadless = sys.props.getOrElse("headless", "true").toBoolean
        println(s"[DriverManager] Headless mode enabled: $isHeadless")
        if (isHeadless) {
          edgeOptions.addArguments("--headless=new")
          addedArgs += "--headless=new"
        }

        // Generate unique profile directory
        val buildId = sys.env.getOrElse("BUILD_ID", "local")
        val uniqueId = UUID.randomUUID().toString
        val uniqueProfileDir = Paths.get(s"/tmp/edge-profile-$buildId-${System.currentTimeMillis}-$uniqueId")

        Files.createDirectories(uniqueProfileDir)
        println(s"[DriverManager] Launching Edge with unique user-data-dir: $uniqueProfileDir")
        val profileArg = s"--user-data-dir=${uniqueProfileDir.toAbsolutePath.toString}"
        edgeOptions.addArguments(profileArg)
        addedArgs += profileArg

        // Print Edge command-line arguments
        println("=== Edge Options Arguments ===")
        addedArgs.foreach(arg => println(s"Edge Arg: $arg"))
        println("=== End of Edge Options ===")

        edgeBinary.foreach(binaryPath => {
          println(s"[DriverManager] Using Edge binary: $binaryPath")
          edgeOptions.setBinary(binaryPath)
        })

        // Build the EdgeDriver service
        val service = new EdgeDriverService.Builder()
          .usingDriverExecutable(new File(driverPath))
          .build()

        val driver = new EdgeDriver(service, edgeOptions)

        // Register shutdown hook to clean up temp profiles
        sys.addShutdownHook {
          println(s"[DriverManager] Cleaning up Edge profile: $uniqueProfileDir")
          try {
            deleteRecursively(uniqueProfileDir.toFile)
          } catch {
            case ex: Exception =>
              println(s"[DriverManager] Failed to clean Edge profile: ${ex.getMessage}")
          }
        }

        driver

      case "chrome" =>
        val chromeOptions = new ChromeOptions()
        val addedArgs = scala.collection.mutable.ListBuffer[String]()

        val isHeadless = sys.props.getOrElse("headless", "true").toBoolean
        println(s"[DriverManager] Chrome headless mode: $isHeadless")
        if (isHeadless) {
          chromeOptions.addArguments("--headless=new")
          addedArgs += "--headless=new"
        }

        println("=== Chrome Options Arguments ===")
        addedArgs.foreach(arg => println(s"Chrome Arg: $arg"))
        println("=== End of Chrome Options ===")

        new ChromeDriver(chromeOptions)

      case "firefox" =>
        val firefoxOptions = new FirefoxOptions()
        val addedArgs = scala.collection.mutable.ListBuffer[String]()

        val isHeadless = sys.props.getOrElse("headless", "true").toBoolean
        println(s"[DriverManager] Firefox headless mode: $isHeadless")
        if (isHeadless) {
          firefoxOptions.addArguments("--headless")
          addedArgs += "--headless"
        }

        println("=== Firefox Options Arguments ===")
        addedArgs.foreach(arg => println(s"Firefox Arg: $arg"))
        println("=== End of Firefox Options ===")

        new FirefoxDriver(firefoxOptions)

      case other =>
        throw new IllegalArgumentException(s"Unsupported browser: $other")
    }
  }

  /**
   * Recursively delete a directory or file.
   */
  private def deleteRecursively(file: File): Unit = {
    if (file.isDirectory) {
      file.listFiles().foreach(deleteRecursively)
    }
    if (!file.delete()) {
      println(s"[DriverManager] WARNING: Failed to delete ${file.getAbsolutePath}")
    }
  }
}
