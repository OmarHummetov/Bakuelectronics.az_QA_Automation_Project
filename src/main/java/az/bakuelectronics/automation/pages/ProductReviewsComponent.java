package az.bakuelectronics.automation.pages;

import az.bakuelectronics.automation.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.regex.Pattern;

/** Read-only accordion interaction; exposes no review input or submission operation. */
public final class ProductReviewsComponent {
    private static final By SECTION = By.xpath("//main//div[contains(@class,'ProdDetails_OtherInnerItem__')]"
            + "[div/button[.//h2[normalize-space()='Rəylər']]]");
    private static final By TOGGLE = By.xpath(".//button[.//h2[normalize-space()='Rəylər']]");
    private static final By PANEL = By.cssSelector("div.MuiCollapse-root");
    private final WebDriver driver;
    private final WaitUtils wait;

    public ProductReviewsComponent(WebDriver driver) {
        this.driver = driver;
        wait = new WaitUtils(driver, Duration.ofSeconds(15));
    }

    public record State(String url, String productName, String productCode, int count,
                        boolean expanded, boolean panelVisible, String toggleText) { }

    public static int parseReviewCount(String text) {
        if (text == null) throw new IllegalArgumentException("Displayed review count is missing");
        var match = Pattern.compile("^\\s*(\\d+)\\s*rəy\\s*$").matcher(text.replace('\u00a0', ' '));
        if (!match.matches()) throw new IllegalArgumentException("Unsupported displayed review count: " + text);
        return Integer.parseInt(match.group(1));
    }

    public State state(boolean expanded) {
        wait.until(active -> {
            WebElement panel = active.findElement(SECTION).findElement(PANEL);
            return panel.getDomAttribute("class").contains(expanded ? "MuiCollapse-entered" : "MuiCollapse-hidden");
        });
        WebElement section = driver.findElement(SECTION);
        WebElement details = driver.findElement(By.cssSelector("main .ProdDetails_ProductDetailsContentRight__qD_hb"));
        if (!details.equals(section.findElement(By.xpath("ancestor::div[contains(@class,'ProdDetails_ProductDetailsContentRight__')][1]")))) {
            throw new IllegalStateException("Review section must belong to the main product details");
        }
        var product = new ProductPage(driver);
        return new State(driver.getCurrentUrl(), details.findElement(By.tagName("h1")).getText().trim(),
                product.getProductCode(),
                parseReviewCount(details.findElement(By.cssSelector("button.ProdDetails_TitleStatsComment__ZTbaa")).getText()),
                section.findElement(PANEL).getDomAttribute("class").contains("MuiCollapse-entered"),
                section.findElement(PANEL).isDisplayed(), section.findElement(TOGGLE).getText());
    }

    public State expand() { clickToggle(); return state(true); }
    public State collapse() { clickToggle(); return state(false); }

    private void clickToggle() {
        WebElement button = wait.visible(SECTION).findElement(TOGGLE);
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", button);
        wait.until(active -> (Boolean) ((JavascriptExecutor) active).executeScript(
                "const e=arguments[0],r=e.getBoundingClientRect();"
                        + "return e.contains(document.elementFromPoint(r.x+r.width/2,r.y+r.height/2));", button));
        button.click();
    }
}
