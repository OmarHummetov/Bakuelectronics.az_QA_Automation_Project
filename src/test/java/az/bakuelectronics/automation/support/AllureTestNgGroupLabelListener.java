package az.bakuelectronics.automation.support;

import io.qameta.allure.Allure;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.util.Set;
import java.util.logging.Logger;

/**
 * Exposes the existing TestNG smoke/regression groups as Allure tags.
 */
public final class AllureTestNgGroupLabelListener implements ITestListener {

    private static final Set<String> ALLURE_GROUPS = Set.of("smoke", "regression");
    private static final Logger LOGGER = Logger.getLogger(AllureTestNgGroupLabelListener.class.getName());

    @Override
    public void onTestStart(ITestResult result) {
        Set<String> groups = Set.of(result.getMethod().getGroups());
        Set<String> matchingGroups = groups.stream()
                .filter(ALLURE_GROUPS::contains)
                .collect(java.util.stream.Collectors.toSet());

        if (matchingGroups.size() > 1) {
            LOGGER.warning("Test belongs to both smoke and regression groups; no Allure tag added: "
                    + result.getMethod().getQualifiedName());
            return;
        }

        if (matchingGroups.size() == 1) {
            addTagIfMissing(matchingGroups.iterator().next());
        }
    }

    private static void addTagIfMissing(String group) {
        Allure.label("tag", group);
    }
}
