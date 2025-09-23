package uk.gov.hmrc.test.ui.driver

import com.typesafe.scalalogging.LazyLogging
import org.openqa.selenium.WebDriver
import uk.gov.hmrc.selenium.webdriver.Driver

object BrowserDriver extends LazyLogging {

  private val browserName = sys.props.getOrElse("browser", "'browser' system property not set")
  private val jenkinsHome = sys.env.get("JENKINS_HOME")

  // These are set by the setup-edge.sh script
  private val edgeDriverPath = sys.env.get("WEBDRIVER_EDGE_DRIVER")
  private val edgeBinaryPath = sys.env.get("EDGE_BINARY")
  private val edgeVersion = sys.env.getOrElse("EDGE_VERSION", "unknown")

  if (browserName.equalsIgnoreCase("edge")) {
    (edgeDriverPath, edgeBinaryPath) match {
      case (Some(driverPath), Some(binaryPath)) =>
        System.setProperty("webdriver.edge.driver", driverPath)
        System.setProperty("webdriver.edge.binary", binaryPath)
        System.setProperty("selenium.manager.enabled", "false")

        logger.info(s"Running Edge $edgeVersion on Jenkins")
        logger.info(s"EdgeDriver path: $driverPath")
        logger.info(s"Edge binary path: $binaryPath")

      case _ =>
        throw new IllegalStateException(
          s"Edge browser selected but required environment variables are missing. " +
            s"Ensure setup-edge.sh script has run before tests."
        )
    }
  } else {
    logger.info(s"Instantiating Browser: $browserName (local or non-Jenkins environment)")
  }

  lazy val driver: WebDriver = Driver.instance
}
