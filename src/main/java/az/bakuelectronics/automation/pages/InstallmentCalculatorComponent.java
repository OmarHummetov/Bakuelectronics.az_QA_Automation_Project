package az.bakuelectronics.automation.pages;

import az.bakuelectronics.automation.utils.PriceUtils;
import az.bakuelectronics.automation.utils.WaitUtils;
import org.openqa.selenium.*;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

/** Observes the visible calculator only; no credit application or payment input. */
public final class InstallmentCalculatorComponent {
    private static final By ROOT = By.cssSelector(".ProdDetails_PurchaseItemCredit__NOY9N .credit-tab");
    private static final By TABS = By.cssSelector("[aria-label='Calculator tabs'] [role='tab']");
    private static final By TERMS = By.cssSelector("button.ProductTab_PeriodButton__3eBe4");
    private final WebDriver driver;
    private final WaitUtils wait;

    public InstallmentCalculatorComponent(WebDriver driver) {
        this.driver = driver;
        wait = new WaitUtils(driver, Duration.ofSeconds(15));
    }

    public record State(String provider, int selectedProviders, List<String> terms, int months,
                        int selectedTerms, BigDecimal monthly, BigDecimal total, String initialPayment) {
        public State { terms = List.copyOf(terms); }
    }

    private WebElement root() {
        return wait.until(active -> active.findElements(ROOT).stream().filter(WebElement::isDisplayed).findFirst().orElse(null));
    }

    public List<String> providers() { return root().findElements(TABS).stream().map(WebElement::getText).map(String::trim).toList(); }

    public State selectProvider(String provider) {
        click(root().findElements(TABS).stream().filter(e -> e.getText().trim().equals(provider)).findFirst().orElseThrow());
        wait.until(active -> root().findElements(TABS).stream().anyMatch(e -> e.getText().trim().equals(provider)
                && "true".equals(e.getDomAttribute("aria-selected"))));
        return settled();
    }

    public State selectTerm(int months) {
        click(root().findElements(TERMS).stream().filter(WebElement::isDisplayed)
                .filter(e -> e.getText().trim().equals(months + " ay")).findFirst().orElseThrow());
        wait.until(active -> root().findElements(TERMS).stream().anyMatch(e -> e.isDisplayed()
                && e.getText().trim().equals(months + " ay") && e.getDomAttribute("class").contains("ProductTab_Active__")));
        return settled();
    }

    private void click(WebElement element) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", element);
        wait.until(active -> (Boolean) ((JavascriptExecutor) active).executeScript(
                "const e=arguments[0],r=e.getBoundingClientRect();return e.contains(document.elementFromPoint(r.x+r.width/2,r.y+r.height/2));", element));
        element.click();
    }

    public State settled() {
        State[] last = {null}; long[] since = {0}; int[] polls = {0};
        return wait.until(active -> {
            State current = read();
            if (!current.equals(last[0])) { last[0] = current; since[0] = System.nanoTime(); polls[0] = 1; return null; }
            return ++polls[0] >= 3 && System.nanoTime() - since[0] >= 1_200_000_000L ? current : null;
        });
    }

    @SuppressWarnings("unchecked")
    private State read() {
        List<Object> row = (List<Object>) ((JavascriptExecutor) driver).executeScript("""
                const r=arguments[0], visible=e=>!!e.getClientRects().length;
                const tabs=[...r.querySelectorAll('[aria-label="Calculator tabs"] [role="tab"][aria-selected="true"]')];
                const panel=[...r.querySelectorAll('[role="tabpanel"]')].filter(visible);
                if(panel.length!==1)throw Error('Expected one visible calculator panel');
                const p=panel[0],terms=[...p.querySelectorAll('button.ProductTab_PeriodButton__3eBe4')];
                const selected=terms.filter(e=>e.className.includes('ProductTab_Active__'));
                const amounts=[...p.querySelectorAll('.ProductTab_BottomSectionTextLeft__jKJbn')].map(e=>e.textContent.trim());
                if(amounts.length!==2 || !tabs.length || !selected.length)throw Error('Incomplete calculator state');
                return [tabs[0].textContent.trim(),tabs.length,terms.map(e=>e.textContent.trim()),
                    selected[0].textContent.trim(),selected.length,...amounts,
                    p.querySelector('input.ProductTab_PaymentInput__kvl_v')?.value || ''];
                """, root());
        return new State((String) row.get(0), ((Number) row.get(1)).intValue(), (List<String>) row.get(2),
                parseMonths((String) row.get(3)), ((Number) row.get(4)).intValue(),
                PriceUtils.parseAzn((String) row.get(5)), PriceUtils.parseAzn((String) row.get(6)), (String) row.get(7));
    }

    public static int parseMonths(String text) {
        if (text == null || !text.matches("[1-9][0-9]* ay")) throw new IllegalArgumentException("Unsupported displayed term: " + text);
        return Integer.parseInt(text.substring(0, text.indexOf(' ')));
    }
}
