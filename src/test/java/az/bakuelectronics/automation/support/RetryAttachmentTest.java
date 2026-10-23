package az.bakuelectronics.automation.support;

import az.bakuelectronics.automation.ui.CartContentsTest;
import az.bakuelectronics.automation.ui.AccountLoginTest;
import az.bakuelectronics.automation.ui.ProductBrandFilterTest;
import io.qameta.allure.Allure;
import io.qameta.allure.testng.AllureTestNg;
import org.testng.Assert;
import org.testng.IRetryAnalyzer;
import org.testng.TestNG;
import org.testng.annotations.Test;
import org.testng.internal.annotations.DisabledRetryAnalyzer;
import org.testng.internal.annotations.TestAnnotation;

import static az.bakuelectronics.automation.support.OfflineRetryFixtures.*;

public class RetryAttachmentTest {
    @Test
    public void attachesToMethodWithNullTestClassAndToClassAnnotation() throws Exception {
        RetryAnnotationTransformer transformer = new RetryAnnotationTransformer();
        TestAnnotation method = annotation();
        transformer.transform(method, null, null, MethodFixture.class.getMethod("transientFailure"));
        Assert.assertEquals(method.getRetryAnalyzerClass(), RetryAnalyzer.class);
        TestAnnotation legacyDefault = annotation();
        legacyDefault.setRetryAnalyzer(null);
        transformer.transform(legacyDefault, null, null, MethodFixture.class.getMethod("transientFailure"));
        Assert.assertEquals(legacyDefault.getRetryAnalyzerClass(), RetryAnalyzer.class);
        TestAnnotation type = annotation();
        transformer.transform(type, ClassFixture.class, null, null);
        Assert.assertEquals(type.getRetryAnalyzerClass(), RetryAnalyzer.class);
        TestAnnotation unknown = annotation();
        transformer.transform(unknown, null, null, null);
        Assert.assertEquals(unknown.getRetryAnalyzerClass(), DisabledRetryAnalyzer.class);
        TestAnnotation offline = annotation();
        transformer.transform(offline, RetryAttachmentTest.class, null, null);
        Assert.assertEquals(offline.getRetryAnalyzerClass(), DisabledRetryAnalyzer.class);
    }

    @Test
    public void preservesExplicitAnalyzersAndClassExclusions() throws Exception {
        RetryAnnotationTransformer transformer = new RetryAnnotationTransformer();
        for (Class<? extends IRetryAnalyzer> analyzer : java.util.List.of(NoRetryAnalyzer.class, CustomAnalyzer.class)) {
            TestAnnotation explicit = annotation();
            explicit.setRetryAnalyzer(analyzer);
            transformer.transform(explicit, MethodFixture.class, null, null);
            Assert.assertEquals(explicit.getRetryAnalyzerClass(), analyzer);
        }
        for (Class<?> type : new Class<?>[]{BasketMutationBaseTest.class,
                ClassExcludedFixture.class, ClassExplicitFixture.class, InheritedExcludedFixture.class}) {
            TestAnnotation excluded = annotation();
            excluded.setRetryAnalyzer(RetryAnalyzer.class);
            transformer.transform(excluded, type, null, null);
            Assert.assertEquals(excluded.getRetryAnalyzerClass(), NoRetryAnalyzer.class);
        }
        TestAnnotation inherited = annotation();
        transformer.transform(inherited, null, null, ClassExcludedFixture.class.getMethod("failure"));
        Assert.assertEquals(inherited.getRetryAnalyzerClass(), NoRetryAnalyzer.class);
    }

    @Test
    public void unsafeGroupsOverrideEvenCustomAnalyzers() {
        for (String group : new String[]{"no-retry", "api-mutation", "ui-mutation", "manual-auth", "sensitive-auth"}) {
            TestAnnotation annotation = annotation();
            annotation.setGroups(new String[]{group});
            annotation.setRetryAnalyzer(CustomAnalyzer.class);
            new RetryAnnotationTransformer().transform(annotation, null, null, null);
            Assert.assertEquals(annotation.getRetryAnalyzerClass(), NoRetryAnalyzer.class, group);
        }
    }

    @Test
    public void realTestNgInvocationsRespectAttachmentLimitsAndExclusions() {
        ATTEMPTS.clear();
        var parentLifecycle = Allure.getLifecycle();
        var parentRoot = parentLifecycle.getCurrentRootKey();
        var parentExecutable = parentLifecycle.getCurrentExecutableKey();
        Assert.assertTrue(parentRoot.isPresent(), "The outer test must retain ordinary Allure reporting.");
        TestNG runner = new TestNG();
        runner.setUseDefaultListeners(false);


        runner.setListenersToSkipFromBeingWiredInViaServiceLoaders(AllureTestNg.class.getName());
        runner.setVerbose(0);
        runner.addListener(new RetryAnnotationTransformer());
        runner.setTestClasses(new Class<?>[]{MethodFixture.class, ClassFixture.class,
                ClassExcludedFixture.class, ClassExplicitFixture.class});
        runner.run();
        Assert.assertTrue(runner.hasFailure(), "Synthetic failures must remain visible to TestNG.");
        Assert.assertEquals(ATTEMPTS.get("method-transient"), Integer.valueOf(2));
        Assert.assertEquals(ATTEMPTS.get("class-transient"), Integer.valueOf(2));
        for (String key : new String[]{"assertion", "wrapped-assertion", "explicit", "custom", "no-retry",
                "api-mutation", "ui-mutation", "manual-auth", "sensitive-auth", "class-group", "class-explicit"}) {
            Assert.assertEquals(ATTEMPTS.get(key), Integer.valueOf(1), key);
        }
        Assert.assertTrue(runner.getServiceLoaderListeners().stream()
                .noneMatch(AllureTestNg.class::isInstance), "Synthetic results must not reach the Allure adapter.");
        Assert.assertTrue(runner.getTestListeners().stream().noneMatch(AllureTestNg.class::isInstance));
        Assert.assertTrue(runner.getSuiteListeners().stream().noneMatch(AllureTestNg.class::isInstance));
        Assert.assertSame(Allure.getLifecycle(), parentLifecycle);
        Assert.assertEquals(parentLifecycle.getCurrentRootKey(), parentRoot);
        Assert.assertEquals(parentLifecycle.getCurrentExecutableKey(), parentExecutable);
    }

    @Test
    public void ungroupedCartAndAccountScenariosExplicitlyDisableRetries() {
        for (Class<?> type : new Class<?>[]{CartContentsTest.class, AccountLoginTest.class, ProductBrandFilterTest.class}) {
            int checked = 0;
            for (var method : type.getDeclaredMethods()) {
                Test annotation = method.getAnnotation(Test.class);
                if (annotation != null) {
                    Assert.assertEquals(annotation.retryAnalyzer(), NoRetryAnalyzer.class, method.toString());
                    checked++;
                }
            }
            Assert.assertEquals(checked, 1);
        }
    }

    private static TestAnnotation annotation() {
        TestAnnotation annotation = new TestAnnotation();
        annotation.setGroups(new String[0]);
        annotation.setRetryAnalyzer(DisabledRetryAnalyzer.class);
        return annotation;
    }
}
