package az.bakuelectronics.automation.support;

import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.ElementNotInteractableException;
import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.ITestNGMethod;
import org.testng.annotations.ITestAnnotation;
import org.testng.annotations.Test;
import org.testng.annotations.DataProvider;
import az.bakuelectronics.automation.ui.LoginInputRejectionTest;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathFactory;
import java.nio.file.Path;

public class RetryPolicyTest {

    @Test
    public void doesNotRetryAssertionFailures() {
        RetryAnalyzer analyzer = new RetryAnalyzer();
        Assert.assertFalse(analyzer.retry(result(new AssertionError("business mismatch"))));
    }

    @Test
    public void retriesOneTransientSeleniumFailureOnly() {
        RetryAnalyzer analyzer = new RetryAnalyzer();
        Assert.assertTrue(analyzer.retry(result(new StaleElementReferenceException("stale"))));
        Assert.assertFalse(analyzer.retry(result(new StaleElementReferenceException("stale again"))));
    }

    @DataProvider
    public Object[][] excludedGroups() {
        return new Object[][]{{"no-retry"}, {"api-mutation"}, {"ui-mutation"},
                {"sensitive-auth"}, {"manual-auth"}};
    }

    @Test(dataProvider = "excludedGroups")
    public void excludesUnsafeGroupsEvenWhenAnalyzerIsAttachedDirectly(String group) {
        Assert.assertFalse(new RetryAnalyzer().retry(
                result(new StaleElementReferenceException("transient"), group)));
    }

    @Test
    public void assertionAnywhereInCauseChainPreventsRetry() {
        AssertionError assertion = new AssertionError("mismatch");
        assertion.initCause(new StaleElementReferenceException("stale"));
        for (Throwable failure : new Throwable[]{assertion,
                new RuntimeException(assertion), new TimeoutException("timeout", assertion),
                new StaleElementReferenceException("stale", assertion)}) {
            Assert.assertFalse(new RetryAnalyzer().retry(result(failure)));
        }
    }

    @Test
    public void rejectsNonTransientFailuresAndHandlesCyclicCauses() {
        RuntimeException first = new RuntimeException("first");
        RuntimeException second = new RuntimeException("second", first);
        first.initCause(second);
        for (Throwable failure : new Throwable[]{null, new IllegalStateException("business failure"),
                new WebDriverException("unclassified driver failure"), first}) {
            Assert.assertFalse(new RetryAnalyzer().retry(result(failure)));
        }
    }

    @Test
    public void eligibleFailuresIncludeWrappedTimeoutAndDisconnectedDriver() {
        Assert.assertTrue(new RetryAnalyzer().retry(result(
                new RuntimeException(new TimeoutException("timeout")))));
        Assert.assertTrue(new RetryAnalyzer().retry(result(new WebDriverException("disconnected"))));
        Assert.assertTrue(new RetryAnalyzer().retry(result(new ElementClickInterceptedException("intercepted"))));
        Assert.assertTrue(new RetryAnalyzer().retry(result(new ElementNotInteractableException("not interactable"))));
    }

    @Test
    public void successfulResultsNeverRetryOrConsumeBudget() {
        RetryAnalyzer analyzer = new RetryAnalyzer();
        Assert.assertFalse(analyzer.retry(result(new StaleElementReferenceException("stale"), true)));
        Assert.assertTrue(analyzer.retry(result(new StaleElementReferenceException("stale"))));
    }

    @Test
    public void globalAnalyzerNeverRetriesNoRetryGroup() {
        Assert.assertFalse(new RetryAnalyzer().retry(
                result(new StaleElementReferenceException("transient"), "no-retry")));
        Assert.assertFalse(new NoRetryAnalyzer().retry(
                result(new StaleElementReferenceException("transient"))));
    }

    @Test
    public void transformerForcesNoRetryAnalyzerForNoRetryGroup() {
        AtomicReference<Class<?>> selected = new AtomicReference<>();
        ITestAnnotation annotation = (ITestAnnotation) Proxy.newProxyInstance(
                ITestAnnotation.class.getClassLoader(), new Class<?>[]{ITestAnnotation.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getGroups" -> new String[]{"regression", "no-retry"};
                    case "setRetryAnalyzer" -> { selected.set((Class<?>) args[0]); yield null; }
                    default -> defaultValue(method.getReturnType());
                });
        new RetryAnnotationTransformer().transform(annotation, LoginInputRejectionTest.class, null, null);
        Assert.assertEquals(selected.get(), NoRetryAnalyzer.class);
    }

    @Test
    public void passwordScenarioDeclaresOneInvocationWithoutRetries() throws NoSuchMethodException {
        Test annotation = LoginInputRejectionTest.class
                .getMethod("incorrectPasswordIsRejectedForDedicatedTestAccount").getAnnotation(Test.class);
        Assert.assertEquals(annotation.retryAnalyzer(), NoRetryAnalyzer.class);
        Assert.assertEquals(annotation.invocationCount(), 1);
        Assert.assertTrue(Arrays.asList(annotation.groups()).contains("no-retry"));
        Assert.assertTrue(Arrays.asList(annotation.groups()).contains("sensitive-auth"));
        Assert.assertTrue(Arrays.asList(annotation.groups()).contains("manual-auth"));
        Assert.assertFalse(Arrays.asList(annotation.groups()).contains("regression"));
    }

    @Test
    public void defaultSuiteAndMavenExcludeManualAuthentication() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        var parser = factory.newDocumentBuilder();
        var xpath = XPathFactory.newInstance().newXPath();
        var suite = parser.parse(Path.of("testng.xml").toFile());
        String transformer = RetryAnnotationTransformer.class.getName();
        Assert.assertEquals(xpath.evaluate("count(/suite/listeners/listener[@class-name='"
                + transformer + "'])", suite), "1");
        Assert.assertEquals(xpath.evaluate(
                "count(/suite/test[@name='UI Tests']/groups/run/exclude[@name='manual-auth'])", suite), "1");
        Assert.assertEquals(xpath.evaluate(
                "count(/suite/test[@name='UI Tests']/groups/run/exclude[@name='sensitive-auth'])", suite), "1");
        var pom = parser.parse(Path.of("pom.xml").toFile());
        Assert.assertEquals(xpath.evaluate("/project/build/plugins/plugin[artifactId='maven-surefire-plugin']"
                + "/configuration/properties/property[name='listener']/value", pom), transformer);
        Assert.assertEquals(xpath.evaluate("/project/properties/test.excludedGroups", pom), "manual-auth,sensitive-auth");
        Assert.assertEquals(xpath.evaluate("/project/build/plugins/plugin[artifactId='maven-surefire-plugin']"
                + "/configuration/excludedGroups", pom), "${test.excludedGroups}");
    }

    @Test
    public void browserValidationTestsRemainInRegression() throws NoSuchMethodException {
        for (String name : new String[]{"malformedEmailIsBlockedByBrowserValidation",
                "emptyRequiredIdentifierIsBlockedByBrowserValidation"}) {
            Test annotation = LoginInputRejectionTest.class.getMethod(name).getAnnotation(Test.class);
            Assert.assertEquals(annotation.groups(), new String[]{"regression"});
        }
    }

    private static ITestResult result(Throwable failure, String... groups) {
        return result(failure, false, groups);
    }

    private static ITestResult result(Throwable failure, boolean success, String... groups) {
        ITestNGMethod testMethod = (ITestNGMethod) Proxy.newProxyInstance(
                ITestNGMethod.class.getClassLoader(), new Class<?>[]{ITestNGMethod.class},
                (proxy, method, args) -> method.getName().equals("getGroups")
                        ? groups : defaultValue(method.getReturnType()));
        return (ITestResult) Proxy.newProxyInstance(
                ITestResult.class.getClassLoader(),
                new Class<?>[]{ITestResult.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "isSuccess" -> success;
                    case "getThrowable" -> failure;
                    case "getMethod" -> testMethod;
                    default -> defaultValue(method.getReturnType());
                });
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) return false;
        if (type == char.class) return '\0';
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0F;
        if (type == double.class) return 0D;
        return null;
    }
}
