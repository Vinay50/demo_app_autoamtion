package com.mycompany.synergy.reference.ui.utility;


import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.firefox.internal.ProfilesIni;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.mycompany.synergy.reference.ui.pageobjects.PageObjectBase;

import cucumber.deps.com.thoughtworks.xstream.InitializationException;

public class DriverFactory {

	private static ThreadLocal<WebDriver> driver = new ThreadLocal<WebDriver>();

	private final static String DEFAULT_BROWSERTYPE = "FIREFOX";
	private final static String DEFAULT_BROWSERVERSION_SAUCE = "57";
	private final static String DEFAULT_BROWSERVERSION_LOCAL = "52";
	private final static String DEFAULT_BROWSERENV = "local";
	private final static String DEFAULT_SAUCE_TUNNEL = "OptumSharedTunnel-Stg";
	
	// TODO: Find a better choice of default SauceLabs credentials
	private final static String DEFAULT_SAUCE_USER = "nwilli82";
	private final static String DEFAULT_SAUCE_ACCESSKEY = "7481ed1d-fa1e-47f5-bc43-16ec49efa075";

	/**
	 * Method to return driver instance for current thread. If one is not set,
	 * create a new driver from system property configuration.
	 * 
	 * @return Current thread's driver instance or newly created driver
	 * @throws MalformedURLException
	 *             If invalid SauceLabs URL is built
	 */
	public static WebDriver createAndGetDeviceDriver() throws MalformedURLException {

		if (driver.get() != null) {
			return driver.get();
		}

		// Retrieve desired browser configuration from system properties
		String BrowserVersion = System.getProperty("BrowserVersion");
		;
		String BrowserType = System.getProperty("BrowserType");
		String browserEnv = System.getProperty("BrowserEnv");
		String sauceUsername = System.getenv("SAUCE_USERNAME");
		String saucePassword = System.getenv("SAUCE_ACCESS_KEY");
		String sauceTunnel = System.getenv("SAUCE_TUNNEL_NAME");

		if (browserEnv == null) {
			browserEnv = DEFAULT_BROWSERENV;
		}
		
		if (BrowserVersion == null && browserEnv.equals("saucelab")) {
			BrowserVersion = DEFAULT_BROWSERVERSION_SAUCE;
		} else if (BrowserVersion == null) {
			BrowserVersion = DEFAULT_BROWSERVERSION_LOCAL;
		}
		
		if (BrowserType == null) {
			BrowserType = DEFAULT_BROWSERTYPE;
		}


		if (browserEnv.equalsIgnoreCase("saucelab")) {
			
			if (sauceUsername == null || saucePassword == null ) {
				sauceUsername = DEFAULT_SAUCE_USER;
				saucePassword = DEFAULT_SAUCE_ACCESSKEY;
			}
			
			if (sauceTunnel == null  ) {
				sauceTunnel = DEFAULT_SAUCE_TUNNEL;				
			}
			
			
			DesiredCapabilities capabilities = null;

			switch ( BrowserType.toUpperCase() ) {
			case "IE":
				capabilities = DesiredCapabilities.internetExplorer();
				capabilities.setCapability("platform", "Windows 7");
				break;
				
			case "FIREFOX":
				capabilities = DesiredCapabilities.firefox();
				capabilities.setCapability("platform", "Windows 7");
				break;
				
			case "SAFARI":
				capabilities = DesiredCapabilities.safari();
				capabilities.setCapability("platform", "OS X 10.11");
				capabilities.setCapability("technologyPreview", true);
				break;
				
			case "CHROME":
				capabilities = DesiredCapabilities.chrome();
				capabilities.setCapability("platform", "Windows 7");
				break;
				
			default:
				throw new IllegalArgumentException("Unsupported Platform/Browser Configuration " + BrowserType);	
			}
			
			boolean toRecordLog = !("prod".equalsIgnoreCase(System.getProperty("ExecutionEnv")));
			URL sauceLabUrl = new URL( "http://" + sauceUsername + ":" + saucePassword 
					+ "@ondemand.saucelabs.com:80/wd/hub" );
			
			capabilities.setCapability("version", BrowserVersion);
			capabilities.setCapability("maxDuration", 2700);
			capabilities.setCapability("avoidProxy", true);
			capabilities.setCapability("autoAcceptAlerts", true);

			// Use sauceLabs tunnel either set via SAUCE_TUNNEL_NAME environment
			// variable or from default
			capabilities.setCapability("tunnelIdentifier", sauceTunnel);
			capabilities.setCapability("parent-tunnel", "sauce_admin");
			
			capabilities.setCapability("recordVideo", true);
			capabilities.setCapability("recordScreenshots", toRecordLog);
			capabilities.setCapability("recordLogs", toRecordLog);
			capabilities.setCapability("screenResolution", "1280x768");

			// TODO Re-assess sauceLab timeouts...
			capabilities.setCapability("idleTimeout", 120);
			capabilities.setCapability("commandTimeout", 240);

			// Turn on enhanced network logging.  Needs evaluation on performance impact, may parameterize
			capabilities.setCapability("extendedDebugging", true);

			setDriver(new RemoteWebDriver(sauceLabUrl, capabilities));
			dataStorage.setCustomErrmsg("Play in saucelab: " + "https://saucelabs.com/beta/tests/"
					+ ((RemoteWebDriver) driver.get()).getSessionId().toString());

		} else {
			// Instantiate local browser
			
			switch ( BrowserType.toUpperCase() ) {
			case "IE":
				System.setProperty("webdriver.ie.driver", "lib" + File.separator + "IEDriverServer.exe");
				setDriver(new InternetExplorerDriver());
				break;
				
			case "FIREFOX":
				
				DesiredCapabilities capabilities = new DesiredCapabilities();
				boolean isMarionette = true;
				if (Integer.parseInt(BrowserVersion) < 47) {
					isMarionette = false;
				}
				capabilities.setCapability("marionette", isMarionette);

				// To ensure proper proxy/certificate configuration, use
				// 'default' Firefox profile.
				ProfilesIni profileIni = new ProfilesIni();
				FirefoxProfile fp = profileIni.getProfile("default");
				capabilities.setCapability(FirefoxDriver.PROFILE, fp);
				
				System.setProperty("webdriver.gecko.driver", "lib" + File.separator + "geckodriver.exe");
				
				setDriver(new FirefoxDriver(capabilities));
				break;
				
			case "SAFARI":			
//				break;
				
			case "CHROME":
//				break;
				
			default:
				throw new IllegalArgumentException("Unsupported local Platform/Browser Configuration " + BrowserType);	
			}
			
		}

		driver.get().manage().deleteAllCookies();
		driver.get().manage().window().maximize();
		
		// Initialize threadLocal instances of wait objects with new driver
		// instance
//		PageObjectBase.smallWait.set(new WebDriverWait(getDeviceDriver(), PageObjectBase.SMALL_WAIT_TIME));
//		PageObjectBase.mediumWait.set(new WebDriverWait(getDeviceDriver(), PageObjectBase.MEDIUM_WAIT_TIME));
//		PageObjectBase.longWait.set(new WebDriverWait(getDeviceDriver(), PageObjectBase.LONG_WAIT_TIME));

		return getDeviceDriver();
	}

	/**
	 * 
	 * @return driver object for current execution thread
	 * @throws InitializationException
	 *             If browser has not been set
	 */
	public static WebDriver getDeviceDriver() throws InitializationException {

		if (driver.get() != null) {
			return driver.get();
		}

		throw new InitializationException("Browser Driver Not Initialized");
	}

	/**
	 * 
	 * @param inputDriver
	 *            Driver object to store in ThreadLocal<WebDriver>
	 */
	private static void setDriver(WebDriver inputDriver) {
		driver.set(inputDriver);
	}

	/**
	 * Closes driver for current thread, sets current thread driver to null.
	 * 
	 * @return true if driver shut down, otherwise false.
	 */
	public static boolean closeDeviceDriver() {

		try {
			WebDriver currentDriver = getDeviceDriver();
			if (currentDriver != null) {
				setDriver(null);
				currentDriver.quit();
			}
			return getDeviceDriver() == null;
		} catch (InitializationException e) {
			return true;
		}
	}
}
