package com.vt;

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.remote.DesiredCapabilities;

import com.vt.dto.LoginDTO;
import com.vt.dto.NapTheDTO;
import com.vt.login.LoginProcessor;
import com.vt.logout.LogoutProcessor;
import com.vt.napthe.NaptheFTTHTraSauProcessor;

public class ViettelAutoProcessor{
	public String execute(LoginDTO loginDto, NapTheDTO naptheDto) { 
		System.out.println("Doing heavy processing - START " + Thread.currentThread().getName());
		
		//
		// String geckoDriverPath = System.getProperty(DRIVER_PATH);
		// if (StringUtils.isEmpty(geckoDriverPath)) {
		// // user path
		// final String geckoDriver = System.getProperty("user.dir") +
		// "\\tool\\driver\\window_64\\geckodriver.exe";
		// System.setProperty("webdriver.gecko.driver", geckoDriver);
		// }
		//
		// FirefoxBinary firefoxBinary = new FirefoxBinary();
		// firefoxBinary.addCommandLineOptions("--headless");
		// FirefoxOptions firefoxOptions = new FirefoxOptions();
		// firefoxOptions.setBinary(firefoxBinary);
		// FirefoxDriver driver = new FirefoxDriver(firefoxOptions);

		String phantomjs = System.getProperty(PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY);
		if (StringUtils.isEmpty(phantomjs)) {
			phantomjs = System.getProperty("user.dir") + "\\tool\\phantomjs\\";
			System.setProperty(PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY, phantomjs);
		}
		DesiredCapabilities caps = new DesiredCapabilities();
		caps.setJavascriptEnabled(true);
		caps.setCapability("takesScreenshot", true);
		caps.setCapability(PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY, phantomjs + "phantomjs.exe");

		System.out.println("----STARTING-----");
		WebDriver driver = new PhantomJSDriver(caps);

		long startTime = System.nanoTime();

		try {
			Thread.sleep(1000);

			LoginProcessor loginProcessor = new LoginProcessor(driver, loginDto);
			boolean isLogged = loginProcessor.execute();
			if (!isLogged) {
				System.out.println("LOGIN FAILED");
				return "ERROR: LOGIN FAILED";
			}

			NaptheFTTHTraSauProcessor naptheProcessor = new NaptheFTTHTraSauProcessor(driver, naptheDto);
			String message = naptheProcessor.execute();
			System.out.println(message);

			LogoutProcessor logoutProcessor = new LogoutProcessor(driver);
			logoutProcessor.execute();

			System.out.println("----END-----");

			long endTime = System.nanoTime();
			long duration = (endTime - startTime);
			double seconds = (double) duration / 1000000000.0;

			System.out.println("TIME SPENT: " + seconds + "s");
			System.out.println("Doing heavy processing - END " + Thread.currentThread().getName());
			driver.quit();
			return message;

		} catch (Exception e) {
			System.out.println("INTERNAL SERVER ERROR");
			driver.quit();
			return "SERVER ERROR: " + e.getMessage();
		}

	}
}