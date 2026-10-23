package az.bakuelectronics.automation.support;

import org.testng.Assert;
import org.testng.annotations.Test;

public class JsonArrayLoadingTest {

    @Test
    public void readStringArrayReturnsAllSearchQueries() {
        String[] queries = JsonUtils.readStringArray(
                "testdata/negative-search-data.json",
                "searchQueries"
        );

        Assert.assertEquals(queries.length, 3);
        Assert.assertEquals(
                queries[0],
                "ZXQ_NONEXISTENT_PRODUCT_987654321"
        );
        Assert.assertEquals(
                queries[1],
                "NO_SUCH_BAKU_PRODUCT_246813579"
        );
        Assert.assertEquals(
                queries[2],
                "UNLISTED_DEVICE_QUERY_135792468"
        );
    }

    @Test
    public void missingPropertyThrowsIllegalArgumentException() {
        Assert.expectThrows(
                IllegalArgumentException.class,
                () -> JsonUtils.readStringArray(
                        "testdata/negative-search-data.json",
                        "missingProperty"
                )
        );
    }

    @Test
    public void missingResourceThrowsIllegalArgumentException() {
        Assert.expectThrows(
                IllegalArgumentException.class,
                () -> JsonUtils.readStringArray(
                        "testdata/missing-resource.json",
                        "searchQueries"
                )
        );
    }
}
