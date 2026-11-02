# Default-search behavior evidence — 2 November 2026, Asia/Baku

**Classification: CONTRACT_UNKNOWN — unresolved default-suggestion population guarantee.**
The immediate failure is HTTP 200 with literal `{}`, not a request-routing, authentication or JSON-parser
failure. No confirmed automation defect or application defect has been established. Keep the direct
data-contract failure. No executable source, test assertion, reporter, suite or integration was changed.

## Real application requests

A fresh guest Chrome session focused the empty search box, then typed the existing `samsung` fixture.
Only public browsing/search was performed: no login, basket action or unsafe diagnostic.
Network performance events confirmed:

| Trigger | Actual request | Result |
| --- | --- | --- |
| Focus empty search | `GET https://5vyra9x.bakuelectronics.az/default-search-result`, no parameters/body | HTTP 200, JSON; empty UI displays `Nəticə tapılmadı` |
| Type `samsung` | `GET https://5vyra9x.bakuelectronics.az/fast-search-expanded?query=samsung` | HTTP 200, JSON; three product titles displayed |

The browser sends `lang: az`, `Accept: application/json, text/plain, */*` and the public website Referer.
CORS preflight OPTIONS requests were distinguished from the GET JSON responses. The deployed bundle
`https://www.bakuelectronics.az/_next/static/chunks/1211-f3e7a807bd4a3eef.js` independently confirms both
requests, the `query` parameter for expanded search, and a 400 ms input debounce. No parameter for
default suggestions was discovered or invented. Form submission navigates to
`/axtaris-neticesi?name=<trimmed input>`; this is a separate results-page operation.

`ApiClient.publicClient()` requests default suggestions with `lang: az`, JSON Accept/Content-Type,
no parameters/body and no authorization. Direct GETs with these headers and with browser-style
Accept/Referer/Origin headers both returned HTTP 200, `application/json`, `{}`. The header difference
does not explain this observation. `ProductApiService` already uses the confirmed expanded-search
request as a bounded integration fixture fallback; the direct contract test does not call that service.

The UI checks optional `data`, `most_searched` and `related_categories` lengths. If none contains
results, it renders its no-results state. Thus the client tolerates missing/empty suggestion sections.
This establishes client behavior, **not an explicit backend promise that `{}` is a valid successful
default response**. No owner-approved empty-state schema, population guarantee or backend explanation
was obtained. A different populated endpoint does not satisfy the direct default endpoint contract.

## Historical populated response comparison

The requested historical result was inspected at
`target/safe-live-regression-20261029-d5a45213/network/public-api/results/6c32beb8-e55c-4982-97c4-269615952a34-result.json`.
It is an actual PASS with five root fields and a product at `$.data[0]`:

- slug: `iphone-18-pro-256gb-glacier-235686`;
- name: `IPHONE 18 PRO 256GB GLACIER`;
- price: numeric string `3899.99`.

The result has no raw request/response attachments. Its other root field names, full product schema,
wire headers and parameters cannot be reconstructed. The retained test/client source and reviewed
investigation describe the same parameterless public GET with `lang: az`; the current client diff only
removes the unrelated PATCH convenience method. This supports a same-request behavior change, but
does not replace missing historical wire evidence. Current default JSON has zero root fields and no
product, whereas current expanded search has a different, observed schema with `title` and numeric price.

## Populated search validation

The supported `/fast-search-expanded?query=samsung` response contained the following actual products;
the browser displayed all three corresponding titles:

| Slug | Title | Price (AZN) |
| --- | --- | --- |
| `telefon-samsung-a276-8256gb-5g-lght-pink-232620` | Smartfon Samsung A27 8GB/256GB 5G, Light Pink (A276) | 1139.99 |
| `telefon-samsung-a276-6128gb-5g-lght-pink-232616` | Smartfon Samsung A27 6GB/128GB 5G, Light Pink (A276) | 869.99 |
| `telefon-samsung-a185-8256gb-silver-a18-235300` | Smartfon Samsung Galaxy A18 8GB/256GB Silver (A185) | 969.99 |

A response diagnostic asserted nonempty `data`, unique nonblank string slugs, nonblank string
`title`, `image`, `category_name`, and positive numeric prices. It also asserted presence of all six
observed root fields (`data`, `most_searched`, `related_keywords`, `related_categories`, `banner`,
`process_time`) and array types for the suggestion/category/keyword collections. These are checks of
the observed expanded-search response, not newly claimed guarantees for default suggestions.

The unchanged public `ProductApiUiConsistencyTest` independently passed with the Galaxy A18 slug above,
the exact detail route, shared model/memory tokens, and API price 969.99 minus discount 70 matching
UI price **899.99 AZN**. The request is repeatable and read-only; catalogue contents/prices are dynamic,
so these observations are not hardcoded as permanent fixtures.

## Assertions and actual results

Previous and retained default data-contract assertions are identical: HTTP 200, JSON media type/nonblank
body, object root with at least one field, a real product with nonblank slug/name-or-title and numeric
price, and response time. The root-population assertion still fails at actual zero fields; later product
checks are not reached. Availability continues to check HTTP/JSON/object format/timing separately.
`ApiTestReporter` remains strict. No arbitrary object acceptance, fallback substitution, skip or retry
was introduced into the default contract test.

| Verification | Actual outcome |
| --- | --- |
| Oracle Java 23.0.2, independent `javac --release 23` of all 29 production sources | PASS, exit 0 |
| Maven Java 23 test compilation, all 108 test/support sources | PASS, exit 0 |
| Existing public API selection: `DefaultSearchAvailabilityTest`, `DefaultSearchResultContractTest` | **1 PASS / 1 FAIL / 0 SKIP**, TestNG exit 1; live retries disabled |
| Existing public `ProductApiUiConsistencyTest` | **1 PASS / 0 FAIL / 0 SKIP**, TestNG exit 0; live retries disabled |
| Full existing `testng-offline.xml`, `mvn --offline -Poffline test` | **330 PASS / 0 FAIL / 0 ERROR / 0 SKIP**, Maven exit 0 |
| Offline full-method + ordered-parameter identity/multiplicity comparison against latest serial baseline | **330 -> 330**, zero missing, added or non-passed identities |
| Final guest search-network diagnostic and expanded-response structure diagnostic | PASS; these are observations, not additional TestNG tests |

The offline run retained its existing retry-policy fixtures. Java/TestNG public selections used the
fresh compiled classpath directly, avoiding simultaneous Maven rebuilds. The authenticated basket
integration was not rerun: this task prohibits login and mutations. Its existing passing evidence and
all integration source/assertions remain unchanged.

Two preliminary temporary browser diagnostics failed on diagnostic-only assumptions (an empty input's
value attribute and product anchor markup). The final probe uses the actual button/title markup and
waits for JSON GET responses rather than preflight completion; it exited 0. Those failed diagnostics
are retained separately and are not counted as passing tests or application defects. Firecrawl failed
with DNS resolution; authorized direct public GETs and Selenium supplied the live evidence instead.

## Evidence and remaining uncertainty

New generated evidence is isolated under `target/search-contract-evidence-20261102/`:
`search-bundle.js`, `default.json`, `expanded.json`, `browser-header-parity.json`,
`browser-network-final.log`, `compile.log`, `javac-main.log`, `public-api.log`, `integration.log`,
`offline.log`, `offline-invocation-comparison.json`, and separate API/integration/offline result directories.
Historical reports were only read; generated files were not staged. No commit or push occurred.

The missing evidence is an authoritative deployed API contract or backend-owner explanation defining
whether `{}` is permitted, which default fields are mandatory, and whether populated default suggestions
are guaranteed. The frontend's no-results handling alone cannot resolve that distinction. Until it is
resolved, preserve the failing default data-contract assertion; do not classify the separate expanded
search success as a repair of this endpoint.

Files changed for this task: this evidence report and the link in `docs/KNOWN_ISSUES.md`.
