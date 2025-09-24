package uk.gov.hmrc.selenium.webdriver

import org.openqa.selenium._
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.edge.{EdgeDriver, EdgeOptions}
import org.openqa.selenium.firefox.{FirefoxDriver, FirefoxOptions}

object DriverFactory {

  lazy val driver: WebDriver = {
    sys.env.get("BROWSER").map(_.toLowerCase) match {
      case Some("firefox") =>
        println("[DriverFactory] Starting FirefoxDriver...")
        val options = new FirefoxOptions()
        options.addArguments("--headless")
        new FirefoxDriver(options)

      case Some("edge") =>
        println("[DriverFactory] Starting EdgeDriver...")

        val edgeBinaryPath = sys.env.getOrElse("EDGE_BINARY", "/usr/bin/microsoft-edge")
        val edgeDriverPath = sys.env.getOrElse("WEBDRIVER_EDGE_DRIVER", "/usr/local/bin/msedgedriver")

        println(s"[DriverFactory] Using Edge binary at: $edgeBinaryPath")
        println(s"[DriverFactory] Using EdgeDriver at: $edgeDriverPath")

        // Make sure Selenium knows where to find the EdgeDriver binary
        System.setProperty("webdriver.edge.driver", edgeDriverPath)

        val options = new EdgeOptions()
        options.setBinary(edgeBinaryPath)
        options.addArguments("--headless=new", "--no-sandbox", "--disable-setuid-sandbox")

        new EdgeDriver(options)

      case _ =>
        println("[DriverFactory] Defaulting to ChromeDriver...")
        val options = new org.openqa.selenium.chrome.ChromeOptions()
        options.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage")
        new ChromeDriver(options)
    }
  }
}