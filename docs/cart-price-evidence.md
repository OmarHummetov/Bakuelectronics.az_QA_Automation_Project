# Cart summary price evidence and assertions

Current filenames: [test naming inventory](test-naming-inventory.md). The dated live verification section retains its original test name and result.

The user verified these real cart-summary CSS Module class prefixes. Selectors
match class-token prefixes without depending on hash suffixes. Summary fields are
scoped to the unique visible summary container; product fields to its unique row.

| Field | Verified prefix |
| --- | --- |
| Container | `sebet_BasketRight__` |
| Item row | `sebet_BasketRightBodyItem__` |
| Product name | `sebet_ItemName__` |
| Original unit price | `sebet_BasketRightBodyItemPriceOld__` |
| Discounted unit price | `sebet_BasketRightBodyItemPriceCurrent__` |
| Quantity text | `sebet_ItemCount__` |
| Total discount | `sebet_BasketRightDiscountPrice__` |
| Grand total | `sebet_TotalTopPrice__` |

Older user-supplied observations (observation date not recorded): quantity one had
original 1599.99, discounted 1449.99, discount -150,
grand total 1449.99 AZN. Observed quantity two: unchanged unit prices, discount
-300 and grand total 2899.98 AZN. These are evidence, never fixed expectations.
No sanitized HTML fixture or screenshot has been supplied.

`CartQuantityPriceValidationTest` reads both unit prices, summary quantity,
discount and grand total at quantities one and two. It asserts:

- Summary quantity matches the expected quantity and the existing cart control.
- Grand total equals discounted unit price multiplied by quantity.
- Absolute discount equals (original minus discounted unit price) multiplied by quantity.
- Both unit prices remain numerically unchanged between quantities one and two.

Amounts use `PriceUtils.parseAzn` and exact `BigDecimal` arithmetic with `compareTo`.
Signed discounts including `- 150 ₼` are supported. The discount is not subtracted
again from the discounted total. Discounted price must be positive and original
price must be at least the discounted price.

## Snapshot and supported state

`CartPage.waitForPriceSummary` reads all values in one synchronous JavaScript call
per explicit-wait poll. It retains immutable values, never WebElements. It waits
up to ten seconds for two consecutive equal snapshots with expected summary/control
quantities and consistent arithmetic. Missing nodes, incomplete amounts and stale
replacements are retried as reads only. Timeout diagnostics include the last state.
Summary quantity accepts digits with an optional `ədəd` suffix; unsupported formats
fail clearly.

The isolated guest cart must have exactly one main cart product row and one summary
product row. The main row must match the selected slug; the summary name must match
the product-page name after whitespace normalization. Every verified field needs
exactly one visible match. Additional items or duplicate fields fail explicitly.
Main rows are found from quantity controls, using their nearest product-link/plus-button
container. Image/name links and nested product representations cannot create extra
rows. The selected slug must resolve to exactly one visible control row, and any
other visible control row is rejected. Unrelated links without cart controls do not
count as products. Summary controls are excluded from main-row detection. The same
slug-scoped row locator is used for quantity changes and product-specific cleanup.

The supported scenario has no extra price components. A detached summary copy
removes verified name, quantity and price fields plus hidden content. Any remaining
numeric content fails as an unsupported amount/charge, including a zero fee or an
amount without a currency symbol. Unrecognized numeric labels are conservatively
rejected too: extend the evidence and validation before allowing another component.
A nonzero net adjustment in the grand total also fails the arithmetic wait.

Existing single-click quantity change, `no-retry` group, empty guest-cart check,
quantity assertions and product-specific `finally` cleanup remain intact.

## Offline verification and safe live validation

Focused offline tests cover signed parsing, synthetic arithmetic, mixed snapshots,
stale replacements, missing-summary rendering, unsupported states and existing
ambiguous-click safeguards. Snapshot tests use WebDriver proxies; they do not
execute JavaScript against a real browser DOM. Row-selector tests evaluate the actual
XPath arguments against offline XML DOM fixtures, including a reproduction where
the former XPath returns two nested containers for one product. Fixtures cover
duplicate links, unrelated links, distinct products and ambiguous selected rows.
Compile all tests, then run only:

```powershell
mvn -o -DskipTests test-compile
mvn -o '-Dtest=PriceParsingArithmeticTest,CartPagePriceSummaryTest,CartPageQuantityTest,WaitConditionResultTest' test
```

Live cart mutation requires explicit approval. Use a fresh temporary guest profile,
no login or API credentials, and verify an empty cart before adding the product.
Increase exactly once, remove only that product in cleanup and dispose the profile.
Never capture cookies, storage, authorization headers or account details.
The original implementation/offline verification above did not include a live
UI/cart mutation run. Those older observations are not a new execution result.

## Confirmed post-fix live verification — 2026-10-22

The user confirmed a live **PASS** of `CartQuantityPriceValidationTest` at
**15:41 (Asia/Baku)** on **2026-10-22**, after the guest cleanup lifecycle fix.
This is a repeat of an already verified scenario: the total remains **23 distinct
verified live UI invocations**, not 24. It verifies this test in that run; separate
live PASS runs do not constitute a clean full regression run. No new live test was
executed during this documentation update. No raw screenshots or session data are
included here.
