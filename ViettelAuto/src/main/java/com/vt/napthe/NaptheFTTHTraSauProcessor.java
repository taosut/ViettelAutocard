package com.vt.napthe;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.DeathByCaptcha.Captcha;
import com.vt.dto.NapTheDTO;
import com.vt.service.CaptchaService;
import com.vt.service.VNCharacterService;
import com.vt.webelement.PageUtils;

public class NaptheFTTHTraSauProcessor {

	private final static String BASE_URL = "https://viettel.vn/my-viettel/quan-ly-cuoc-thanh-toan/nap-the";

	// element
	private final static String FORM_ID = "fee_payment";
	private final static String SO_THUE_BAO_ID = "payment_isdn";
	private final static String MA_CAPTCHA_ID = "pay_captcha";
	private final static String DICH_VU_ID = "pay_service_type";

	private final static String CHECKBOX_OTHER_INPUT_ID = "checkbox_sosanh1";
	private final static String CHECKBOX_OTHER_XPATH = "//form[@id='fee_payment']/div[2]/div//input";

	private final static String MA_THE_CAO_XPATH = "//form[@id='fee_payment']/div[5]/div[2]//input[@name='pay[code]']";
	private final static String IMG_CAPTCHA_XPATH = "//form[@id='fee_payment']/div[6]/div[2]/a/img[@class='captcha']";

	private WebDriver driver;
	private NapTheDTO napTheDto;

	public NaptheFTTHTraSauProcessor(WebDriver driver, NapTheDTO napTheDto) {
		this.driver = driver;
		this.napTheDto = napTheDto;
	}

	public String execute() throws IOException {
		 PageUtils.offlogging();
		 
		if (napTheDto == null || StringUtils.isEmpty(napTheDto.getMaTheCao())
				|| StringUtils.isEmpty(napTheDto.getSoThueBao()) || napTheDto.getServiceType() == null) {
			return "ERROR: param error";
		}

		StringBuilder strBuilder = new StringBuilder();
		int countRetry = 0;
		while (true) {
			countRetry = countRetry + 1;
			System.out.println("RETRY TIMES: " + countRetry);
			if (countRetry > 7) {
				System.out.println("too many retry ... quit after :" + countRetry);
				break;
			}
			strBuilder = new StringBuilder();
			System.out.println("prepareto get to the page");

			try {
				driver.manage().timeouts().pageLoadTimeout(10, TimeUnit.SECONDS);
				driver.get(BASE_URL);

			} catch (Exception e) {
				// TODO: handle exception
				System.out.println("get request timeout");

				continue;
			}

			System.out.println("get request success -prepare to wait element");
			// wait loading element
			(new WebDriverWait(driver, 10)).until(new ExpectedCondition<Boolean>() {
				public Boolean apply(WebDriver d) {
					return driver.findElement(By.xpath(CHECKBOX_OTHER_XPATH)) != null
							&& driver.findElement(By.id(FORM_ID)) != null;
				}
			});

			System.out.println("get check box nạp cho số khác");
			// checkbox element
			WebElement checkboxContainerEl = driver.findElement(By.xpath(CHECKBOX_OTHER_XPATH));
			if (checkboxContainerEl == null) {
				return "ERROR: WebElement checkboxContainerEl";
			}
			if (!checkboxContainerEl.isSelected()) {
				checkboxClicked(driver);
			}

			System.out.println("get check box nạp cho số khác");

			// // wait for captcha
			(new WebDriverWait(driver, 30)).until(new ExpectedCondition<Boolean>() {
				public Boolean apply(WebDriver d) {
					return d.findElement(By.xpath(IMG_CAPTCHA_XPATH)) != null;
				}
			});

			System.out.println("get được captcha element");

			String fileLocation = captureCaptcha(driver);
			if (StringUtils.isEmpty(fileLocation)) {
				return "ERROR: Captcha is not captured";
			}
			System.out.println("get được captcha img");

			Captcha captcha = null;
			int retrycaptcha = 0;
			while (null == captcha || captcha.text.isEmpty()) {
				retrycaptcha += 1;
				if (retrycaptcha > 3) {
					break;
				}
				try {
					captcha = CaptchaService.requestCheckCaptcha(fileLocation);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			String captchaText = captcha.text;
			System.out.println("got captcha : " + captchaText);
			WebElement dichVuEl = driver.findElement(By.id(DICH_VU_ID));
			WebElement sothuebaoEl = driver.findElement(By.id(SO_THUE_BAO_ID));
			WebElement mathecaoEl = driver.findElement(By.xpath(MA_THE_CAO_XPATH));
			WebElement captchaEl = driver.findElement(By.id(MA_CAPTCHA_ID));

			if (dichVuEl == null || sothuebaoEl == null || mathecaoEl == null || captchaEl == null) {
				return "ERROR: Form fee_payment is invisible.";
			}

			System.out.println("got elements to fill params");
			// change data here
			dichVuEl.sendKeys(napTheDto.getServiceType().name());
			sothuebaoEl.sendKeys(napTheDto.getSoThueBao());
			mathecaoEl.sendKeys(napTheDto.getMaTheCao());
			captchaEl.sendKeys(captchaText);

			System.out.println("values filled");

			try {

				// submit form
				WebElement formEl = driver.findElement(By.id(DICH_VU_ID));
				formEl.submit();
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
				System.out.println("submit form error");
				continue;
			}

			System.out.println("form submitted");

			PageUtils.waitForLoad(driver);

			// get error message
			List<WebElement> errorMessagesEl = driver.findElements(By.className("error-message"));
			System.out.println("got response message array");
			if (errorMessagesEl != null && errorMessagesEl.size() > 0) {
				for (WebElement el : errorMessagesEl) {
					if (!strBuilder.toString().contains(el.getText()) && StringUtils.isNotEmpty(el.getText())) {
						strBuilder.append(el.getText());
					}
				}
			}

			String message = strBuilder.toString();
			System.out.println("got response mesage : " + message);
			if (StringUtils.isNotEmpty(message)) {
				String messageFormat = VNCharacterService.removeAccent(message);
				String mabaomat = VNCharacterService.removeAccent("Mã bảo mật");

				if (!messageFormat.contains(mabaomat)) {
					System.out.println("done, break");
					break;
				} else {
					CaptchaService.reportIncorectCaptcha(captcha);
				}
			}
		}
		System.out.println("return: " + strBuilder.toString());
		return strBuilder.toString();
	}

	private String captureCaptcha(WebDriver driver) throws IOException {
		// get image captcha
		WebElement imgCaptchaEl = driver.findElement(By.xpath(IMG_CAPTCHA_XPATH));

		String linkImageUrlCaptcha = imgCaptchaEl.getAttribute("src");
		System.out.println("linkImageUrlCaptcha: " + linkImageUrlCaptcha);

		if (StringUtils.isNotEmpty(linkImageUrlCaptcha)) {

			// Get entire page screenshot
			File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
			BufferedImage fullImg = ImageIO.read(screenshot);

			// Get the location of element on the page
			Point point = imgCaptchaEl.getLocation();

			// Get width and height of the element
			int eleWidth = imgCaptchaEl.getSize().getWidth();
			int eleHeight = imgCaptchaEl.getSize().getHeight();

			// Crop the entire page screenshot to get only element screenshot
			BufferedImage eleScreenshot = fullImg.getSubimage(point.getX(), point.getY(), eleWidth, eleHeight);
			ImageIO.write(eleScreenshot, "png", screenshot);

			// get file location
			String fileLocation = getPathFileCaptcha(linkImageUrlCaptcha);
			// Copy the element screenshot to disk
			File screenshotLocation = new File(fileLocation);
			FileUtils.copyFile(screenshot, screenshotLocation);

			return fileLocation;
		}
		return "";
	}

	private void checkboxClicked(WebDriver driver) {
		JavascriptExecutor jse = (JavascriptExecutor) driver;
		WebElement element = driver.findElement(By.id(CHECKBOX_OTHER_INPUT_ID));
		jse.executeScript("arguments[0].click();", element);
	}

	private String getPathFileCaptcha(String linkImageSrc) {
		String fileName = getImageNameBySID(linkImageSrc);
		String folder = System.getProperty("user.dir") + "\\captcha\\";
		String destFile = folder + fileName;
		return destFile;
	}

	public String getImageNameBySID(String linkImageSrc) {
		String[] params = linkImageSrc.split("&");
		for (String param : params) {
			String name = param.split("=")[0];
			String value = param.split("=")[1];
			if (StringUtils.equals("https://viettel.vn/captcha?sid", name)) {
				return value + ".png";
			}
		}
		return "captcha_im.png";
	}

	public WebDriver getDriver() {
		return driver;
	}

	public void setDriver(WebDriver driver) {
		this.driver = driver;
	}

	public NapTheDTO getNapTheDto() {
		return napTheDto;
	}

	public void setNapTheDto(NapTheDTO napTheDto) {
		this.napTheDto = napTheDto;
	}

}
