# Basket product discovery — 2 November 2026

## Exact failure cause

`BasketApiUiVisibilityTest` depended solely on `GET /default-search-result` for its fixture. The endpoint now returns HTTP 200, `application/json`, and an empty object (`{}`), both publicly and with the configured account's bearer authentication. There are zero product records to parse or validate. The exception occurs before ownership registration, basket mutation or UI login.

This is not a credential failure, price parsing failure, stock rejection, changed product-field alias, or unstable ordering. The old selector already sorted complete candidates lexically by slug. The immediate failure is an empty upstream response combined with the selector's assumption that default suggestions always contain a complete product. The backend reason for the empty response is unknown.

The unchanged `DefaultSearchResultContractTest` independently failed its existing nonempty JSON-root assertion (actual field count 0). This observed API contract regression remains a failure; adapting integration fixture discovery does not repair or waive the direct endpoint contract.

## Sanitized discovery evidence

Requests use the configured API base (`https://5vyra9x.bakuelectronics.az`), `lang: az`, and JSON accept headers. No credential, bearer value, cookie, account row or personal identifier is included in this record.

| Request | HTTP | Product data |
| --- | --- | --- |
| Public `/default-search-result`, no parameters | 200 | `{}`; zero candidates |
| Authenticated `/default-search-result`, no parameters | 200 | `{}`; zero candidates |
| Public `/fast-search-expanded?query=samsung` | 200 | `data`: 3 complete products; `most_searched`: 4 unpriced suggestions; 3 related categories |

The alternative contract was observed in the site's own search-bar bundle, `/_next/static/chunks/1211-f3e7a807bd4a3eef.js`: `GET /fast-search-expanded` with a `query` parameter and language header. `Samsung` is already a verified search fixture in `SearchBrowserHistoryTest`. Its real response supplies model-bearing titles required by the unchanged product-detail integration.

Required selector fields remain nonblank `slug`, nonblank `name` or `title`, and a parsed non-null decimal `price`. Optional ID/product code and discount parsing are unchanged. All 3 returned product records satisfy these requirements, with positive prices and actual numeric discounts. Unpriced suggestions and related categories are not product candidates; no price or identity is invented. Neither search response supplies a stock field, so availability was independently checked in the real UI before mutation.

## Minimal correction

Only `ProductApiService.java` changes executable behavior. It first requests default suggestions as before. If that successful response has no complete candidates, it makes exactly one `/fast-search-expanded?query=samsung` request. It applies the existing completeness filter and lexical slug ordering. A configured slug must still be present in the eligible results. HTTP failures, malformed data and absence of suitable candidates still fail; there are no sleeps, retries or skips.

Diagnostics report request source, HTTP status, candidate/complete counts, and counts rejected for blank slug/name or missing price. Existing model parsing, API client, login/Page Objects, product identity/quantity/price assertions, suites, account guards, journals and cleanup are unchanged. No test classes or scenarios were added.

## Recovery and fixture verification

Before any new mutation, the failed run's cleanup container was inspected: API restoration and complete baseline verification after fresh UI authentication/refresh were recorded as passed. No ownership journal remained. A separate fresh-authentication API read succeeded (RedID HTTP 200) and confirmed the prior integration product `fridger-yoshiro-yrf-t248df55hs-193910` absent. No recovery mutation was needed.

The final selector chose `telefon-samsung-a185-8256gb-silver-a18-235300`, from real search data rather than a pinned product. Before mutation, an authenticated GET confirmed that exact candidate absent from the baseline. A read-only UI inspection verified the exact product route, slug/code suffix, no out-of-stock message, one enabled add-to-basket action, and API/UI discounted price parity: 969.99 − 70.00 = 899.99 AZN. No add control was clicked by that diagnostic.

## Verification

- Java 23.0.2 compilation: PASS.
- Existing focused identity and API-client checks: 15 PASS; zero failures/errors/skips.
- Suite/runner validation: PASS; 178 XML references and all 14 existing groups retained.
- Direct default-search contract check: 1 FAIL, preserved as the upstream API contract finding above.
- Complete offline allowlist: **330 PASS**, zero failures/errors/skips; no classes or scenarios added.
- Current integration suite: **2 PASS**, zero failures/errors/skips, Maven exit 0.
- `ProductApiUiConsistencyTest`: PASS with exact product route, model-name identity and API/UI discounted price of 899.99 AZN.
- `BasketApiUiVisibilityTest`: PASS. RedID authentication and basket addition returned HTTP 200. The UI used the same configured account and verified exact slug `telefon-samsung-a185-8256gb-silver-a18-235300`, expected quantity 1, actual quantity 1.
- Cleanup: existing targeted API restoration passed; complete original basket matched after fresh browser authentication and refresh. The ownership journal completed and no journal remains.
- Final independent verification: a separate JVM performed fresh API authentication (RedID HTTP 200) and authenticated basket GET; the exact selected test-owned product was absent. It was absent from the initial baseline, so removal was the correct restoration.

During diagnostic preparation, one read-only probe failed compilation while Maven rebuilt its classpath; it made no live request or mutation. It was rerun successfully after compilation. This is not counted as a test result.

Existing authenticated cart tests, BE-UI-001 and BE-UI-002 are unchanged. They do not call this selector; no additional live cart mutation scenarios are needed to cover this change. No staging, commit, push, checkout or payment actions were performed.

## Evidence locations

- Initial public/authenticated response shape: `target/product-discovery-probe-20261102.log`.
- Supported Samsung search response: `target/product-discovery-20261102/samsung-search.log`.
- Available fixture and baseline absence: `target/product-discovery-20261102/availability-samsung.log`.
- Compilation/focused checks: `target/product-discovery-20261102/compile.log`, `focused.log`.
- Preserved direct API failure Allure: `target/allure/api/20261102-183138-5d76a940/report-20261102-183553-7de9aedd/index.html`.
- Integration Allure (both existing integrations): `target/allure/integration/20261102-183834-d596fe5f/report-20261102-184158-fae32999/index.html`.
- Offline Allure: `target/allure/unit/20261102-183457-98e58ec7/report-20261102-183846-61859d27/index.html`.
- Final fresh-authentication absence check: `target/product-discovery-20261102/final-recovery.log`.

All reports contain isolated actual outcomes from their corresponding new runs. The failed direct API contract report is retained separately from the passing integration/offline reports. The default endpoint's empty-response contract regression is the remaining upstream issue; its backend implementation cause has not been established.
