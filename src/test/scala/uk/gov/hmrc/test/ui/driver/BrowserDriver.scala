package uk.gov.hmrc.test.ui.driver

import com.typesafe.scalalogging.LazyLogging
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.edge.{EdgeDriver, EdgeOptions}
import org.openqa.selenium.firefox.FirefoxDriver

trait BrowserDriver extends LazyLogging {

  private val browserName = sys.props.getOrElse("browser", "chrome").toLowerCase
  private val jenkinsHome = sys.env.get("JENKINS_HOME")
  private val edgeVersion = sys.env.getOrElse("EDGE_VERSION", "138.0.3351.95")

  // Configure Edge for Jenkins
  if (browserName == "edge" && jenkinsHome.isDefined) {
    val edgeDriverPath = s"$jenkinsHome/.local/edgedriver-$edgeVersion/msedgedriver"
    val edgeBinaryPath = s"$jenkinsHome/.local/microsoft-edge-$edgeVersion/microsoft-edge"

    System.setProperty("webdriver.edge.driver", edgeDriverPath)
    System.setProperty("selenium.manager.enabled", "false") // disable online lookup

    logger.info(s"Running Edge $edgeVersion on Jenkins")
    logger.info(s"EdgeDriver path: $edgeDriverPath")
    logger.info(s"Edge binary path: $edgeBinaryPath")
  } else {
    logger.info(s"Instantiating browser: $browserName (local or non-Jenkins environment)")
  }

  // Lazy WebDriver instantiation
  lazy val driver: WebDriver = browserName match {
    case "firefox" =>
      new FirefoxDriver()
    case "edge" =>
      val options = new EdgeOptions()
      sys.env.get("EDGE_BINARY").foreach(options.setBinary)
      new EdgeDriver(options)
    case _ => // default to Chrome
      new ChromeDriver()
  }
}
