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
    // Log working directory
    println(s"Working Directory: ${new File(".").getAbsolutePath}")

    // Log system properties
    println("=== JVM System Properties ===")
    sys.props.foreach { case (k, v) => println(s"$k = $v") }

    // Log environment variables
    println("=== Environment Variables ===")
    sys.env.foreach { case (k, v) => println(s"$k = $v") }

    val browser = sys.props.getOrElse(
      "browser",
      throw new IllegalArgumentException("'browser' system property must be set")
    )

    println(s"[DriverManager] Selected browser: $browser")

    // Decide headless mode: allow overriding by system property first, then environment variable
    val headless = sys.props.get("headless")
      .orElse(sys.env.get("BROWSER_OPTION_HEADLESS"))
      .map(_.toBoolean)
      .getOrElse(true)

    println(s"[DriverManager] Headless mode enabled: $headless")

    browser match {
      case "edge" =>
        val edgeBinary = sys.props.get("edge.binary").orElse(sys.env.get("EDGE_BINARY"))
        val driverPath = sys.props.get("webdriver.edge.driver")
          .orElse(sys.env.get("WEBDRIVER_EDGE_DRIVER"))
          .getOrElse(throw new IllegalArgumentException("Edge driver path must be set via 'webdriver.edge.driver' or 'WEBDRIVER_EDGE_DRIVER'"))

        val edgeOptions = new EdgeOptions()

        if (headless) {
          edgeOptions.addArguments("--headless=new")
        }

        // Create unique user-data-dir
        val buildId = sys.env.getOrElse("BUILD_ID", "local")
        val uniqueId = UUID.randomUUID().toString
        val uniqueProfileDir = Paths.get(s"/tmp/edge-profile-$buildId-${System.currentTimeMillis}-$uniqueId")

        Files.createDirectories(uniqueProfileDir)

        println(s"[DriverManager] Launching Edge with unique user-data-dir: $uniqueProfileDir")
        edgeOptions.addArguments(s"--user-data-dir=${uniqueProfileDir.toAbsolutePath.toString}")

        // Log all Edge options we have set
        println("=== Edge Options Arguments ===")
        List("--headless=new", s"--user-data-dir=${uniqueProfileDir.toAbsolutePath.toString}")
          .filter(edgeOptions.toString.contains) // only print the ones actually used
          .foreach(arg => println(s"Edge Arg: $arg"))
        println("=== End of Edge Options ===")

        edgeBinary.foreach(bin => {
          println(s"[DriverManager] Using Edge binary: $bin")
          edgeOptions.setBinary(bin)
        })

        val service = new EdgeDriverService.Builder()
          .usingDriverExecutable(new File(driverPath))
          .build()

        val driver = new EdgeDriver(service, edgeOptions)

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
        if (headless) {
          chromeOptions.addArguments("--headless=new")
        }
        println("[DriverManager] Launching Chrome")
        new ChromeDriver(chromeOptions)

      case "firefox" =>
        val firefoxOptions = new FirefoxOptions()
        if (headless) {
          firefoxOptions.addArguments("--headless=new")
        }
        println("[DriverManager] Launching Firefox")
        new FirefoxDriver(firefoxOptions)

      case other =>
        throw new IllegalArgumentException(s"Unsupported browser: $other")
    }
  }

  private def deleteRecursively(file: File): Unit = {
    if (file.isDirectory) {
      Option(file.listFiles()).getOrElse(Array.empty).foreach(deleteRecursively)
    }
    file.delete()
  }
}
