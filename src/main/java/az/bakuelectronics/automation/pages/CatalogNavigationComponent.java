package az.bakuelectronics.automation.pages;

import az.bakuelectronics.automation.utils.WaitUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/** Public catalog interactions shared by responsive and bounded keyboard journeys. */
public final class CatalogNavigationComponent {
    public static final String SMARTPHONES = "/catalog/telefonlar-qadcetler/smartfonlar-mobil-telefonlar";
    private static final By CATALOG = By.cssSelector("button.header_catalogBtn__uitj2");
    private static final By MENU = By.cssSelector("nav.megaMenu_megaMenu__J7sAI");
    private final WebDriver driver;
    private final WaitUtils wait;
    public CatalogNavigationComponent(WebDriver driver) { this.driver = driver; wait = new WaitUtils(driver, Duration.ofSeconds(15)); }

    public record MenuState(boolean open, String classes, long viewportWidth, long viewportHeight) { }
    public record Focus(String tag, String name, String id, String classes, boolean visible, boolean focusVisible,
                        String outlineStyle, String outlineWidth, String shadow, String background, String border) { }

    public MenuState settledMenu() {
        MenuState[] last = {null}; long[] since = {0}; int[] polls = {0};
        return wait.until(active -> {
            MenuState current = menuState();
            if (!current.equals(last[0])) { last[0] = current; since[0] = System.nanoTime(); polls[0] = 1; return null; }
            return ++polls[0] >= 3 && System.nanoTime() - since[0] >= 1_200_000_000L ? current : null;
        });
    }

    private MenuState menuState() {
        String classes = wait.until(active -> active.findElements(MENU).stream().findFirst().orElse(null)).getDomAttribute("class");
        List<?> size = (List<?>) ((JavascriptExecutor) driver).executeScript("return [innerWidth,innerHeight];");
        return new MenuState(classes.contains("megaMenu_show__8NYPv"), classes,
                ((Number) size.get(0)).longValue(), ((Number) size.get(1)).longValue());
    }

    public MenuState openMobile() {
        if (settledMenu().open()) throw new IllegalStateException("Catalog must start closed");
        click(CATALOG);
        wait.until(active -> menuState().open());
        return settledMenu();
    }

    public MenuState closeMobile() {
        // Inspected direct header of the root drawer; submenu close buttons are separate copies.
        click(By.cssSelector(".megaMenu_megaMenu__wrapper__ke4SP > [class*='megaMenu_megaMenu__header__'] button.megaMenu_megaMenu__close__9nGPJ"));
        wait.until(active -> !menuState().open());
        return settledMenu();
    }

    public ProductGrid.Snapshot navigateMobileSmartphones() {
        click(By.xpath("//nav//button[contains(@class,'megaMenu_categories__btn__')"
                + " and normalize-space()='Smartfonlar və planşetlər']"));
        click(By.xpath("//nav//button[contains(@class,'megaMenu_categories__btn__') and normalize-space()='Smartfonlar']"));
        click(By.cssSelector("nav a.megaMenu_categories__btn__ODo4e[href='" + SMARTPHONES + "']"));
        wait.until(active -> URI.create(active.getCurrentUrl()).getPath().equals(SMARTPHONES));
        return new CategoryPage(driver).snapshot();
    }

    private void click(By locator) {
        try {
            WebElement target = wait.until(active -> {
                if (active.findElements(By.cssSelector("button.campaignModalClose")).stream().anyMatch(WebElement::isDisplayed)) {
                    az.bakuelectronics.automation.utils.CampaignPopup.dismissIfPresent(active);
                    return null;
                }
                return active.findElements(locator).stream().filter(WebElement::isDisplayed)
                        .filter(WebElement::isEnabled).filter(this::hitTarget).findFirst().orElse(null);
            });
            target.click();
        } catch (TimeoutException timeout) {
            var candidates = driver.findElements(locator).stream().map(element -> ((JavascriptExecutor) driver).executeScript("""
                    const e=arguments[0],r=e.getBoundingClientRect(),h=document.elementFromPoint(r.x+r.width/2,r.y+r.height/2);
                    return {tag:e.tagName,classes:e.className,text:e.textContent.trim(),rect:r.toJSON(),
                        hit:h?.outerHTML.slice(0,600),viewport:[innerWidth,innerHeight],scroll:[scrollX,scrollY]};
                    """, element)).toList();
            throw new TimeoutException("Catalog target not ready: " + locator + "; candidates=" + candidates, timeout);
        }
    }

    private boolean hitTarget(WebElement element) {
        return (Boolean) ((JavascriptExecutor) driver).executeScript("""
                const e=arguments[0],r=e.getBoundingClientRect(),x=r.x+r.width/2,y=r.y+r.height/2;
                return r.width>0&&r.height>0&&x>=0&&y>=0&&x<innerWidth&&y<innerHeight
                    &&e.contains(document.elementFromPoint(x,y));
                """, element);
    }

    public Focus catalogFocus() { return describe(wait.visible(CATALOG)); }
    public Focus activeFocus() { return describe(driver.switchTo().activeElement()); }

    private Focus describe(WebElement element) {
        List<?> styles = (List<?>) ((JavascriptExecutor) driver).executeScript("""
                const e=arguments[0],s=getComputedStyle(e),r=e.getBoundingClientRect();
                return [e.matches(':focus-visible'),s.outlineStyle,s.outlineWidth,s.boxShadow,s.backgroundColor,s.border,
                    r.width>0&&r.height>0&&r.x+r.width>0&&r.y+r.height>0&&r.x<innerWidth&&r.y<innerHeight];
                """, element);
        return new Focus(element.getTagName(), element.getAccessibleName(), element.getDomAttribute("id"),
                element.getDomAttribute("class"), element.isDisplayed() && (Boolean) styles.get(6),
                (Boolean) styles.get(0), (String) styles.get(1), (String) styles.get(2), (String) styles.get(3),
                (String) styles.get(4), (String) styles.get(5));
    }

    public List<Focus> tabToCatalog() {
        List<Focus> trace = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            tab(false); Focus focus = activeFocus(); trace.add(focus);
            if (focus.classes() != null && focus.classes().contains("header_catalogBtn__")) return List.copyOf(trace);
        }
        throw new IllegalStateException("Catalog unreachable within 25 Tab presses; possible keyboard trap. Trace=" + trace);
    }

    public Focus tab(boolean reverse) {
        Actions action = new Actions(driver);
        if (reverse) action.keyDown(Keys.SHIFT).sendKeys(Keys.TAB).keyUp(Keys.SHIFT).perform();
        else action.sendKeys(Keys.TAB).perform();
        return activeFocus();
    }

    public MenuState enterCatalog() {
        if (!driver.switchTo().activeElement().equals(driver.findElement(CATALOG)))
            throw new IllegalStateException("Enter is restricted to the focused public catalog button");
        new Actions(driver).sendKeys(Keys.ENTER).perform();
        return settledMenu();
    }

    public MenuState escape() { new Actions(driver).sendKeys(Keys.ESCAPE).perform(); return settledMenu(); }
}
