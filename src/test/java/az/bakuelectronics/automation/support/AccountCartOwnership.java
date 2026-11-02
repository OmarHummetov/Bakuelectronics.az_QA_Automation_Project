package az.bakuelectronics.automation.support;

import java.util.HashMap;
import java.util.Map;

/** Ownership is registered before dispatch. Existing account rows are never owned. */
public final class AccountCartOwnership {
    private final Map<String, Integer> baseline;
    private final Map<String, Integer> owned = new HashMap<>();

    public AccountCartOwnership(Map<String, Integer> baseline) {
        validate(baseline);
        this.baseline = Map.copyOf(baseline);
    }

    private static void validate(Map<String, Integer> items) {
        if (items.entrySet().stream().anyMatch(e -> !e.getKey().matches("[a-zA-Z0-9_-]+")
                || e.getValue() == null || e.getValue() < 1))
            throw new IllegalArgumentException("Invalid cart inventory");
    }

    public void register(String slug) {
        validate(Map.of(slug, 1));
        if (baseline.containsKey(slug) || owned.putIfAbsent(slug, 1) != null)
            throw new IllegalStateException("Product is pre-existing or already owned");
    }

    public void expectTwo(String slug) {
        if (!Integer.valueOf(1).equals(owned.get(slug)))
            throw new IllegalStateException("Quantity change requires owned quantity one");
        owned.put(slug, 2);
    }

    public void verifySafeRecovery(Map<String, Integer> current) {
        validate(current);
        if (!baseline.entrySet().stream().allMatch(e -> e.getValue().equals(current.get(e.getKey())))
                || current.entrySet().stream().anyMatch(e -> !baseline.containsKey(e.getKey())
                && (!owned.containsKey(e.getKey()) || e.getValue() > owned.get(e.getKey()))))
            throw new IllegalStateException("Ambiguous basket: stop all mutations; unrelated items are untouched");
    }

    public Map<String, Integer> baseline() { return baseline; }
    public Map<String, Integer> owned() { return Map.copyOf(owned); }
}
