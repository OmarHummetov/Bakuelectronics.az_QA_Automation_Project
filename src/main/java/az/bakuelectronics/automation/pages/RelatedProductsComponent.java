package az.bakuelectronics.automation.pages;

import az.bakuelectronics.automation.utils.WaitUtils;
import org.openqa.selenium.*;
import java.net.URI;
import java.time.Duration;

/** Title links in the audited buy-together recommendations; never its action buttons. */
public final class RelatedProductsComponent {
    private static final By TITLES = By.cssSelector("main .ProdDetails_ProductBesidesBoxLeftText__EV0CN a[href^='/mehsul/']");
    private final WebDriver driver;
    private final WaitUtils wait;
    public RelatedProductsComponent(WebDriver driver) { this.driver = driver; wait = new WaitUtils(driver, Duration.ofSeconds(15)); }
    public record Destination(String href, String name) { }
    public Destination captureFirst() {
        WebElement link = wait.until(active -> active.findElements(TITLES).stream().filter(WebElement::isDisplayed).findFirst().orElse(null));
        return new Destination(link.getAttribute("href"), link.getText().trim());
    }
    public void follow(Destination captured) {
        WebElement link = wait.until(active -> active.findElements(TITLES).stream().filter(WebElement::isDisplayed)
                .filter(e -> captured.href().equals(e.getAttribute("href")) && captured.name().equals(e.getText().trim()))
                .findFirst().orElse(null));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", link);
        wait.until(active -> (Boolean) ((JavascriptExecutor) active).executeScript(
                "const e=arguments[0],r=e.getBoundingClientRect();return e.contains(document.elementFromPoint(r.x+r.width/2,r.y+r.height/2));", link));
        link.click();
        wait.until(active -> URI.create(active.getCurrentUrl()).getPath().equals(URI.create(captured.href()).getPath()));
        String[] last = {null}; long[] since = {0};
        wait.until(active -> {
            var page = new ProductPage(active);
            String current = active.getCurrentUrl() + "|" + page.getProductName() + "|" + page.getProductCode();
            if (!current.equals(last[0])) { last[0] = current; since[0] = System.nanoTime(); return false; }
            return System.nanoTime() - since[0] >= 1_200_000_000L;
        });
    }
}
