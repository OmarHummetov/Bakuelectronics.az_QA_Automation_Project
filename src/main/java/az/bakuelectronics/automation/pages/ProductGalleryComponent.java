package az.bakuelectronics.automation.pages;

import az.bakuelectronics.automation.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.time.Duration;

public final class ProductGalleryComponent {
    private static final By ACTIVE_IMAGE = By.cssSelector("[class*=SwiperGallery] .swiper-slide-active img");
    private static final By THUMBNAILS = By.cssSelector(".ThumbsGallery_ThumbSlide__XeS9g");
    private final WebDriver driver;
    private final WaitUtils wait;

    public ProductGalleryComponent(WebDriver driver) {
        this.driver = driver;
        this.wait = new WaitUtils(driver, Duration.ofSeconds(15));
    }

    public record Image(String source, int naturalWidth, int naturalHeight) { }

    public Image loadedActiveImage() {
        return wait.until(active -> {
            WebElement image = active.findElements(ACTIVE_IMAGE).stream().filter(WebElement::isDisplayed)
                    .findFirst().orElse(null);
            if (image == null) return null;
            boolean loaded = Boolean.TRUE.equals(((JavascriptExecutor) active).executeScript(
                    "return arguments[0].complete && arguments[0].naturalWidth > 0"
                            + " && arguments[0].naturalHeight > 0;", image));
            return loaded ? new Image(image.getDomProperty("currentSrc"),
                    Integer.parseInt(image.getDomProperty("naturalWidth")),
                    Integer.parseInt(image.getDomProperty("naturalHeight"))) : null;
        });
    }

    public String selectDifferentThumbnail(String currentSource) {
        WebElement thumbnail = wait.until(active -> active.findElements(THUMBNAILS).stream()
                .filter(WebElement::isDisplayed)
                .filter(element -> !currentSource.equals(element.findElement(By.tagName("img")).getAttribute("src")))
                .findFirst().orElse(null));
        String expected = thumbnail.findElement(By.tagName("img")).getAttribute("src");
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", thumbnail);
        thumbnail.click();
        wait.until(active -> expected.equals(active.findElement(ACTIVE_IMAGE).getDomProperty("currentSrc")));
        return expected;
    }
}
