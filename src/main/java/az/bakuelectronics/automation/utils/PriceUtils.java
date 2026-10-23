package az.bakuelectronics.automation.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class PriceUtils {
    private PriceUtils() {
    }

    public static BigDecimal parseAzn(String text) {
        if (text == null) {
            throw new IllegalArgumentException("AZN price must not be null.");
        }
        String numeric = text.replaceAll("[\\s\\p{Zs}]+", " ").trim()
                .replaceFirst("(?i)^(?:AZN|₼) *| *(?:AZN|₼)$", "");
        boolean negative = numeric.startsWith("-");
        if (negative) {
            numeric = numeric.substring(1).trim();
        }
        if (!numeric.matches("(?:[0-9]+(?:[.,][0-9]{1,2})?"
                + "|[0-9]{1,3}(?:,[0-9]{3})+(?:\\.[0-9]{1,2})?"
                + "|[0-9]{1,3}(?:\\.[0-9]{3})+(?:,[0-9]{1,2})?"
                + "|[0-9]{1,3}(?: [0-9]{3})+(?:[.,][0-9]{1,2})?)")) {
            throw new IllegalArgumentException("Expected a single AZN amount with valid grouping and precision.");
        }
        numeric = numeric.replace(" ", "");
        int lastSeparator = Math.max(numeric.lastIndexOf(','), numeric.lastIndexOf('.'));
        int trailingDigits = numeric.length() - lastSeparator - 1;
        if (lastSeparator >= 0 && trailingDigits <= 2) {
            numeric = numeric.substring(0, lastSeparator).replace(",", "").replace(".", "")
                    + "." + numeric.substring(lastSeparator + 1);
        } else {
            numeric = numeric.replace(",", "").replace(".", "");
        }
        BigDecimal amount = new BigDecimal(numeric).setScale(2, RoundingMode.UNNECESSARY);
        return negative ? amount.negate() : amount;
    }
}
