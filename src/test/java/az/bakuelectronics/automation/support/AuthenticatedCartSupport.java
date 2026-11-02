package az.bakuelectronics.automation.support;

import az.bakuelectronics.automation.config.ConfigReader;
import az.bakuelectronics.automation.driver.DriverManager;
import az.bakuelectronics.automation.pages.CartPage;
import az.bakuelectronics.automation.pages.CartSnapshot;
import az.bakuelectronics.automation.pages.LoginPage;
import az.bakuelectronics.automation.pages.ProductPage;
import az.bakuelectronics.automation.support.api.BasketTestSupport;
import az.bakuelectronics.automation.utils.CampaignPopup;
import az.bakuelectronics.automation.utils.PriceUtils;
import io.qameta.allure.Allure;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;

import java.math.BigDecimal;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@SensitiveAuth
public class AuthenticatedCartSupport extends BaseTest {
    protected static final String A = "playstation-5-slim-blue-ray-201123";
    protected static final String B = "ps5-teenage-mutant-ninja-turtles-shredders-revenge-223041";

    protected record Product(String slug, String name, String code, BigDecimal price) { }
    protected CartPage cart;
    protected AccountCartOwnership ownership;
    private PrivateCartJournal journal;
    private CartSnapshot originalPrices;
    protected String stage = "not started";

    protected void at(String value) {
        stage = value;
        Allure.attachment("Authenticated cart stage", value); // Fixed text only.
    }

    protected void login() {
        long started = System.nanoTime();
        String[] condition = {"open homepage"};
        java.util.function.Consumer<String> checkpoint = value -> {
            condition[0] = value;
            Allure.attachment("Authentication checkpoint", (System.nanoTime() - started) / 1_000_000 + " ms: " + value);
        };
        try {
        checkpoint.accept("open homepage");
        driver().get(ConfigReader.getUiBaseUrl() + "/");
        checkpoint.accept("dismiss campaign popup");
        CampaignPopup.dismissIfPresent(driver());
        var wait = new WebDriverWait(driver(), Duration.ofSeconds(20));
        checkpoint.accept("wait for account SSO link and click once");
        wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("a[href^='https://sso.redid.az/auth/']"))).click();
        checkpoint.accept("wait for SSO host");
        wait.until(ExpectedConditions.urlMatches("https://sso\\.redid\\.az(?:/.*)?"));
        // The existing protected LoginPage flow reads credentials only from the process environment.
        new LoginPage(driver()).loginWithConfiguredCredentials(Duration.ofSeconds(
                Long.getLong("authenticated.ui.mfa.timeout.seconds", 180L)), checkpoint);
        checkpoint.accept("wait for authenticated profile control and click once");
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("button.header_UserIcon__gOK4_"))).click();
        checkpoint.accept("wait for visible logout action");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[normalize-space()='Çıxış']")));
        checkpoint.accept("wait for visible orders link");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("a[href='/istifadeci/sifarisler']")));
        checkpoint.accept("open basket directly");
        driver().get(ConfigReader.getUiBaseUrl() + "/sebet");
        checkpoint.accept("dismiss basket campaign popup");
        CampaignPopup.dismissIfPresent(driver());
        checkpoint.accept("wait for basket route");
        cart = new CartPage(driver()).waitForCartPage();
        checkpoint.accept("authenticated basket ready");
        } catch (RuntimeException | Error failure) {
            String frame = java.util.Arrays.stream(failure.getStackTrace())
                    .filter(f -> f.getClassName().startsWith("az.bakuelectronics."))
                    .findFirst().map(StackTraceElement::toString).orElse("framework wait");
            Allure.attachment("Authentication failure condition", condition[0] + "; "
                    + failure.getClass().getSimpleName() + "; " + frame);
            attachAuthenticationFlags();
            throw failure;
        }
    }

    private void attachAuthenticationFlags() {
        try {
            Object flags = ((JavascriptExecutor) driver()).executeScript("""
                const visible = selector => [...document.querySelectorAll(selector)].some(e => !!e.getClientRects().length);
                return {ssoHost: location.hostname === 'sso.redid.az',
                  expectedHome: location.origin === arguments[0] && location.pathname === '/',
                  hasQuery: location.search.length > 0, hasFragment: location.hash.length > 0,
                  expectedTitle: document.title.includes('Baku Electronics'), ready: document.readyState === 'complete',
                  passwordVisible: visible('input[name="password"]'),
                  otpVisible: visible('input[autocomplete="one-time-code"], input[name*="otp" i]'),
                  captchaVisible: visible('iframe[src*="captcha"], iframe[src*="challenge"]'),
                  profileVisible: visible('button.header_UserIcon__gOK4_'),
                  campaignVisible: visible('button.campaignModalClose'),
                  rejectionVisible: visible('form > p.text-red-600')};
                """, ConfigReader.getUiBaseUrl());
            Allure.attachment("Authentication page booleans (no values or URLs)", String.valueOf(flags));
        } catch (RuntimeException unavailable) {
            Allure.attachment("Authentication page booleans", "unavailable");
        }
    }

    protected void scenario(Runnable body) {
        BasketTestSupport.lock();
        Throwable primary = null;
        try {
            at("login");
            login();
            at("capture initial basket");
            ownership = new AccountCartOwnership(cart.waitForInventory());
            if (!ownership.baseline().isEmpty()) originalPrices = cart.waitForAccountSnapshot();
            journal = new PrivateCartJournal(Path.of(".phase4-account-cart.secret").toAbsolutePath());
            journal.save(ownership, "baseline captured");
            at("verify reauthentication before mutations");
            reopenAndVerify();
            Assert.assertEquals(cart.waitForInventory(), ownership.baseline());
            body.run();
        } catch (RuntimeException | Error failure) {
            primary = failure;
            Allure.attachment("Failure stage (no private data)", stage + "; " + failure.getClass().getSimpleName());
            if (ownership != null && cart != null) {
                for (String slug : ownership.owned().keySet()) {
                    Allure.attachment("Owned product mutation state", cart.failureState(slug));
                    attachOwnedControlGeometry(slug);
                }
            }
            throw failure;
        } catch (java.io.IOException failure) {
            primary = failure;
            throw new IllegalStateException("Private ownership journal unavailable", failure);
        } finally {
            try {
                if (journal != null) {
                    try {
                        at("recovery through fresh browser and account reauthentication");
                        reopenAndVerify();
                        for (String slug : ownership.owned().keySet()) {
                            ownership.verifySafeRecovery(cart.waitForInventory());
                            if (cart.waitForInventory().containsKey(slug)) cart.removeOwnedProduct(slug);
                            journal.save(ownership, "recovery removal verified");
                        }
                        driver().navigate().refresh();
                        Assert.assertEquals(cart.waitForInventory(), ownership.baseline(), "Original inventory must be restored");
                        if (originalPrices != null) Assert.assertEquals(cart.waitForAccountSnapshot(), originalPrices,
                                "Original account rows and prices must be unchanged");
                        journal.complete();
                        at("RESTORED: original account basket verified after reauthentication and refresh");
                    } catch (Exception | AssertionError recovery) {
                        Allure.attachment("Account restoration", "UNVERIFIED: stop further mutations; private journal retained");
                        if (primary instanceof org.testng.SkipException)
                            throw new AssertionError("Recovery failure overrides a blocked scenario", recovery);
                        if (primary != null) primary.addSuppressed(recovery);
                        else throw new AssertionError("Account basket restoration failed", recovery);
                    }
                }
            } finally { BasketTestSupport.unlock(); }
        }
    }

    protected void reopenAndVerify() {
        DriverManager.quitDriver();
        DriverManager.createDriver();
        login();
        ownership.verifySafeRecovery(cart.waitForInventory());
    }

    protected Product inspect(String slug) {
        Assert.assertFalse(ownership.baseline().containsKey(slug), "Fixture must be absent from initial basket");
        driver().get(ConfigReader.getUiBaseUrl() + "/mehsul/" + slug);
        CampaignPopup.dismissIfPresent(driver());
        ProductPage page = new ProductPage(driver()).waitForProductPage();
        var state = page.settledPurchaseState();
        Assert.assertEquals(URI.create(state.url()).getPath().replaceAll("/$", ""), "/mehsul/" + slug);
        Assert.assertTrue(slug.endsWith("-" + state.code()));
        Assert.assertEquals(state.usableCartActions(), 1);
        Assert.assertTrue(state.stockText().isBlank(), "Available product required");
        return new Product(slug, state.name(), state.code(), PriceUtils.parseAzn(page.getCurrentPrice()));
    }

    protected void add(Product product) {
        cart.openCart();
        Map<String, Integer> before = cart.waitForInventory();
        ownership.verifySafeRecovery(before);
        Assert.assertFalse(before.containsKey(product.slug()));
        Assert.assertEquals(inspect(product.slug()), product);
        ownership.register(product.slug());
        persist("add registered before dispatch");
        new ProductPage(driver()).clickAddToCart();
        cart.openCart();
        Map<String, Integer> expected = new HashMap<>(before);
        expected.put(product.slug(), 1);
        Assert.assertEquals(cart.waitForInventory(), expected);
    }

    protected void increase(Product product) {
        ownership.verifySafeRecovery(cart.waitForInventory());
        Assert.assertEquals(cart.getProductQuantity(product.slug()), 1);
        ownership.expectTwo(product.slug());
        persist("quantity two registered before dispatch");
        cart.increaseQuantity(product.slug());
    }

    private void persist(String stage) {
        try { journal.save(ownership, stage); }
        catch (java.io.IOException failure) { throw new IllegalStateException("Ownership was not durably recorded", failure); }
    }

    protected CartSnapshot summary(Map<String, Product> products, Map<String, Integer> quantities) {
        Map<String, Integer> expected = new HashMap<>(ownership.baseline());
        expected.putAll(quantities);
        Assert.assertEquals(cart.waitForInventory(), expected);
        CartSnapshot snapshot = cart.waitForAccountSnapshot();
        Assert.assertEquals(snapshot.rows().keySet(), expected.keySet());
        snapshot.rows().forEach((slug, row) -> {
            Assert.assertEquals(row.quantity(), expected.get(slug).intValue());
            if (products.containsKey(slug)) {
                Product p = products.get(slug);
                Assert.assertEquals(row.name(), CartSnapshot.normalize(p.name()));
                Assert.assertEquals(row.discountedUnitPrice().compareTo(p.price()), 0);
            } else Assert.assertEquals(row, originalPrices.rows().get(slug), "Pre-existing row remains unchanged");
        });
        Assert.assertTrue(snapshot.hasExpectedArithmetic(), "Exact BigDecimal discount and total arithmetic");
        return snapshot;
    }

    /** Structural evidence only for a test-owned row; never capture account data or page source. */
    protected void inspectOwnedPriceStructure(String slug) {
        Assert.assertTrue(ownership.owned().containsKey(slug));
        Object structure = ((JavascriptExecutor) driver()).executeScript("""
            const a = [...document.querySelectorAll('main a[href]')]
              .find(a => new URL(a.href).pathname === '/mehsul/' + arguments[0]);
            if (!a) throw Error('Owned row missing');
            let row = a.parentElement;
            while (row && ![...row.querySelectorAll('button')].some(b => b.innerText.trim() === '+')) row = row.parentElement;
            if (!row) throw Error('Owned row controls missing');
            const slugs = [...new Set([...row.querySelectorAll('a[href]')].map(a =>
              new URL(a.href).pathname.match(/^\\/mehsul\\/([a-zA-Z0-9_-]+)\\/?$/)?.[1]).filter(Boolean))];
            if (slugs.length !== 1 || slugs[0] !== arguments[0]) throw Error('Ambiguous owned row');
            const normalize = text => text.replace(/[\\s\\u00a0]+/g, ' ').trim();
            const names = [...new Set([...row.querySelectorAll('a[href]')].map(a => normalize(a.innerText)).filter(Boolean))];
            if (names.length !== 1) throw Error('Ambiguous owned row name');
            const summaries = [...document.querySelectorAll("main [class^='sebet_BasketRightBodyItem__']")]
              .filter(r => [...r.querySelectorAll("[class^='sebet_ItemName__']")]
                .some(e => normalize(e.innerText) === names[0]));
            if (summaries.length !== 1) throw Error('Ambiguous owned summary row');
            const numbers = root => [...root.querySelectorAll('*')].filter(e => e.getClientRects().length
              && !e.children.length && /[0-9]/.test(e.innerText)
              && /^[0-9\\s.,₼-]+(?:AZN|ədəd)?$/.test(e.innerText.trim()))
              .map(e => ({tag: e.tagName, className: e.getAttribute('class') || '', numericText: e.innerText.trim()}));
            return {mainRowNumericFields: numbers(row), summaryRowNumericFields: numbers(summaries[0])};
            """, slug);
        Allure.attachment("Owned row numeric DOM fields (no account data)", String.valueOf(structure));
    }

    private void attachOwnedControlGeometry(String slug) {
        try {
            Object geometry = ((JavascriptExecutor) driver()).executeScript("""
                const link = [...document.querySelectorAll('main a[href]')]
                  .find(a => new URL(a.href).pathname === '/mehsul/' + arguments[0]);
                let row = link?.parentElement;
                while (row && ![...row.querySelectorAll('button')].some(b => b.innerText.trim() === '+')) row = row.parentElement;
                if (!row) return 'owned control row unavailable';
                return [...row.querySelectorAll('button')].filter(b => ['+', '-'].includes(b.innerText.trim())).map(b => {
                  const r = b.getBoundingClientRect();
                  const x = r.x + r.width / 2, y = r.y + r.height / 2;
                  const top = document.elementFromPoint(x, y);
                  return {control: b.innerText.trim(), disabled: b.disabled, width: r.width, height: r.height,
                    x, y, viewportHeight: innerHeight, unobscured: !!top && (top === b || b.contains(top)),
                    coveringTag: top?.tagName, coveringClass: top?.getAttribute('class')};
                });
                """, slug);
            Allure.attachment("Owned control geometry (no page text)", String.valueOf(geometry));
        } catch (RuntimeException unavailable) {
            Allure.attachment("Owned control geometry", "unavailable");
        }
    }
}
