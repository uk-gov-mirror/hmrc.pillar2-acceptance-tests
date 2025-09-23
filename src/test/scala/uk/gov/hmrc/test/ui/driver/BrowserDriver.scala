package uk.gov.hmrc.test.ui.driver

import com.typesafe.scalalogging.LazyLogging
import org.openqa.selenium.WebDriver
import uk.gov.hmrc.selenium.webdriver.Driver

object BrowserDriver extends LazyLogging {

  private val browserName = sys.props.getOrElse("browser", "'browser' System property not set")
  private val jenkinsHome = sys.env.get("JENKINS_HOME")
  private val edgeVersion = sys.env.getOrElse("EDGE_VERSION", "138.0.3351.95")

  if (browserName.equalsIgnoreCase("edge") && jenkinsHome.isDefined) {
    val edgeDriverPath = s"$jenkinsHome/.local/edgedriver-$edgeVersion/msedgedriver"
    val edgeBinaryPath = s"$jenkinsHome/.local/microsoft-edge-$edgeVersion/microsoft-edge"

    System.setProperty("webdriver.edge.driver", edgeDriverPath)
    System.setProperty("webdriver.edge.binary", edgeBinaryPath)
    // Disable SeleniumManager to avoid online lookup
    System.setProperty("selenium.manager.enabled", "false")

    logger.info(s"Running Edge $edgeVersion on Jenkins")
    logger.info(s"EdgeDriver path: $edgeDriverPath")
    logger.info(s"Edge binary path: $edgeBinaryPath")
  } else {
    logger.info(s"Instantiating Browser: $browserName (local or non-Jenkins environment)")
  }

  lazy val driver: WebDriver = Driver.instance
}
