package az.bakuelectronics.automation.support;

import org.testng.IAnnotationTransformer;
import org.testng.annotations.ITestAnnotation;
import org.testng.annotations.Test;
import org.testng.internal.annotations.DisabledRetryAnalyzer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;

public final class RetryAnnotationTransformer implements IAnnotationTransformer {

    @Override
    public void transform(ITestAnnotation annotation, Class testClass,
                           Constructor testConstructor, Method testMethod) {
        Class<?> declaringClass = testClass != null ? testClass
                : testMethod != null ? testMethod.getDeclaringClass() : null;
        if (isExcluded(declaringClass, annotation.getGroups())) {
            annotation.setRetryAnalyzer(NoRetryAnalyzer.class);
            return;
        }
        if (declaringClass != null
                && BaseTest.class.isAssignableFrom(declaringClass)
                && (annotation.getRetryAnalyzerClass() == null
                    || annotation.getRetryAnalyzerClass() == DisabledRetryAnalyzer.class)) {
            annotation.setRetryAnalyzer(RetryAnalyzer.class);
        }
    }

    static boolean isExcluded(Class<?> testClass, String[] groups) {
        if (Arrays.stream(groups).anyMatch(group -> switch (group) {
            case "no-retry", "api-mutation", "ui-mutation", "sensitive-auth", "manual-auth" -> true;
            default -> false;
        })) {
            return true;
        }
        if (testClass == null) {
            return false;
        }
        if (testClass.isAnnotationPresent(SensitiveAuth.class)) return true;
        if (BasketMutationBaseTest.class.isAssignableFrom(testClass)) {
            return true;
        }
        for (Class<?> type = testClass; type != null; type = type.getSuperclass()) {
            Test classAnnotation = type.getDeclaredAnnotation(Test.class);
            if (classAnnotation != null
                    && (classAnnotation.retryAnalyzer() == NoRetryAnalyzer.class
                        || isExcluded(null, classAnnotation.groups()))) {
                return true;
            }
        }
        return false;
    }
}
