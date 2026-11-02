# Default-search API investigation — 2 November 2026, Asia/Baku

**Classification: CONTRACT_UNKNOWN. Both direct endpoint tests remain FAIL.** No confirmed automation
defect justifies changing a request, parser, assertion or shared reporter. The immediate cause is an empty
upstream object; the backend reason and whether that object is permitted remain unknown.

## Reproduction and sanitized response

The two tests use `ApiClient.publicClient()` and the following request:

```http
GET https://5vyra9x.bakuelectronics.az/default-search-result
Accept: application/json
Content-Type: application/json
lang: az
```

There are no query parameters, request body, Authorization header or configured account credentials in
this public request. `ApiConfig` obtains its base from `ConfigReader`; the default is the host above.
The client forwards the GET without rewriting the endpoint or parameters. No missing search query is
established: `/fast-search-expanded?query=samsung` is a different operation used for integration discovery.

Fresh independent GET observation:

```http
HTTP 200
Content-Type: application/json

{}
```

The complete body is two UTF-8 bytes, with zero root fields and no products. The independent PowerShell
JSON parser yields an empty object. Both fresh REST Assured tests yield an empty `LinkedHashMap` from
`response.jsonPath().get("$")`. This rules out a path/alias/product-parser error as the cause of the
reported zero-field assertion. The sanitized record is
`target/default-search-review-20261102/public-response.json`.

`verifyJsonRoot()` first checks the root type, then asserts a nonempty map. Its label "contains required
fields" means at least one field, not a named schema. This same assertion exists in HEAD and the current
working tree. Even bypassing it would leave the subsequent existing product-presence/schema assertions
unsatisfied. Removing it, accepting `{}` or substituting the expanded-search response would not resolve
the direct endpoint contract and would weaken the tests without evidence.

## Contract and historical evidence

The repository has no discovered owner-approved OpenAPI/Swagger/Postman specification defining whether
default suggestions must always contain products, named root fields, empty-state shape, language behavior
or data-availability guarantees. The README assertion table and test expectations are not independent
API authority. Public searches for this endpoint and this host's Swagger/OpenAPI documentation returned
no applicable results; the Firecrawl lookup failed with DNS `ENOTFOUND api.firecrawl.dev`. Absence from
these searches does not establish that private documentation does not exist. No guessed documentation,
private route or unauthorized diagnostic endpoint was probed.

Retained 29 October evidence at
`target/safe-live-regression-20261029-d5a45213/network/public-api/results/` contains both tests as PASS.
Contract result `6c32beb8-e55c-4982-97c4-269615952a34-result.json` records five root fields and a product
at `$.data[0]` with nonblank slug/name and numeric-string price. It has no full-response attachment;
the remaining field names and complete historical body cannot be reconstructed from that result.
The 24 and 25 October standard API reports also contain both tests as PASS.

The latest pre-investigation direct contract result under
`target/allure/api/20261102-183138-5d76a940/results/` is FAIL at the zero-field assertion.
These observations establish a change from previous populated responses. They do not distinguish an
application defect, intended empty suggestions, environment configuration or missing backend data.
The earlier [product-discovery report](BASKET_PRODUCT_DISCOVERY_2026-11-02.md) calls this a contract
regression; that wording should be read as an observed behavior change pending authoritative policy.

Resolution requires the API owner's versioned contract for the deployed environment: whether `{}` is
valid, mandatory response/product fields, empty-data semantics, required request context and availability
guarantees. If products are mandatory, preserve these failures and file an application defect against
that contract. If `{}` is explicitly valid, review the two endpoint-specific expectations separately.
Neither outcome is assumed here. No new application bug is confirmed.

## Recent changes and other failures

The existing uncommitted `ProductApiService` change requests default suggestions first, then one bounded
expanded-search GET if no complete candidates are available. This is integration fixture selection;
neither direct default-search test calls that service. It cannot cause their request or parsing failure.
The existing `ApiClient` diff only removes an unrelated PATCH convenience method; its GET construction
is unchanged. No source changes were made in this investigation.

| Test / evidence | Finding and disposition |
| --- | --- |
| `DefaultSearchAvailabilityTest` | Fresh FAIL: HTTP 200/JSON/object pass; nonempty root fails, actual 0. CONTRACT_UNKNOWN. |
| `DefaultSearchResultContractTest` | Fresh FAIL with identical cause. Product checks after the root assertion are not reached. |
| `BasketSnapshotReadTest` | Fresh PASS, including HTTP 200, nonempty object, products list, timing and snapshot parsing. No shared empty-root failure. |
| `BasketPostZeroQuantityTest` | Historical FAIL: POST zero changed basket to quantity one. CONTRACT_UNKNOWN; separate issue, not reproduced. |
| `BasketUnauthorizedRequestTest` | Historical FAIL at intentional pre-request safety guard. EXPECTED_SAFETY_BLOCK; no HTTP response, not reproduced. |

The two historical diagnostic findings remain documented in [API contract review](API_CONTRACT_REVIEW.md).
The inspected 24/25 October standard API reports contain ten passing tests each; they do not establish
current live success for unexecuted mutation tests. No unauthorized diagnostics or basket mutations ran.
`AuthenticationFlowTest` was not selected: it performs login POSTs and is not a read-only-group test.
The requested authenticated snapshot check used the existing configured authentication flow, then only
GET `/getBasket`; credentials, tokens, cookies and basket contents were not added to this report.

## Verification and compatibility

All fresh Maven runs used Oracle JDK **23.0.2** (`23.0.2+7-58`), cached dependencies and isolated output.
A separate fresh-output compilation compiled 29 production and 108 test/support sources for Java 23
with Maven exit 0 (`target/default-search-review-20261102/compile.log`). It used a generated copy of the
POM with only source/build paths redirected; the repository POM and existing build outputs were preserved.
The full offline run also recompiled all test/support sources. Live test retries and
Surefire reruns were set to zero; the offline suite retained its existing retry-policy fixtures.

| Selection | Actual outcome |
| --- | --- |
| Full `testng-offline.xml` via `-Poffline` | **330 PASS**, 0 failures/errors/skips; Maven exit 0. |
| Both public default-search classes | **2 FAIL**, 0 errors/skips; Maven exit 1, preserved. |
| `BasketSnapshotReadTest` | **1 PASS**, 0 failures/errors/skips; Maven exit 0. |
| `ProductApiUiConsistencyTest` | **1 PASS**, 0 failures/errors/skips; Maven exit 0. Fallback supplied usable product; UI route/identity/discounted price checks passed. |
| `ApiClientAuthenticationTest,ProductIdentityMatchingTest,WaitConditionResultTest` | **16 PASS**, 0 failures/errors/skips; Maven exit 0. |
| `scripts/Test-AllureWorkflow.ps1` | PASS: 178 XML references, 14 groups, 10 standard plus 2 diagnostic API methods and safety/selector checks. No live tests from this script. |

The baseline comparison uses full method name and ordered parameter names/values, preserving invocation
multiplicity, against `target/allure/unit/20261102-183457-98e58ec7/results/`: **330 -> 330**, zero missing,
added or non-passed invocations. Evidence: `target/default-search-review-20261102/offline-invocation-comparison.json`.

Both integration classes compile and retain their existing signatures/service calls. The public product
integration passed fresh. `BasketApiUiVisibilityTest` was not rerun because it adds a product and invokes
restoration mutations. Its retained 2 November result
`target/allure/integration/20261102-183834-d596fe5f/results/bc9a4d96-0a0f-4673-8e98-2cfcd6cdd41b-result.json`
is PASS; its associated cleanup container records passed restoration and complete original basket
verification after fresh authentication and refresh. That is historical runtime evidence, not a fresh
pass in this investigation. No executable changes affect either integration.

BE-UI-001 and BE-UI-002 tests/reports remain unchanged. No tests/classes/features/dependencies were added,
no assertions/retries/guards were changed, and no Git staging, commit or push occurred. The repository
was already dirty; existing work was preserved.

## Isolated Allure reports and file changes

Fresh reports are under `target/default-search-review-20261102/`:

- [Public API — two actual failures](../target/default-search-review-20261102/public-api/report/index.html)
- [Authenticated basket read](../target/default-search-review-20261102/basket-read/report/index.html)
- [Offline — 330 passed](../target/default-search-review-20261102/offline/report/index.html)
- [Public product integration](../target/default-search-review-20261102/integration/report/index.html)
- [Focused checks](../target/default-search-review-20261102/focused/report/index.html)

Each category has separate `results/`, `surefire/`, `report/` and `allure-generation.log`; the root has
the corresponding Maven logs. All five Allure generation commands exited 0; report widget totals match
the actual outcomes above. Historical results were inspected only as explicitly requested evidence,
without importing, rewriting or regenerating their reports. HTML generation does not change outcomes.

Files changed by this investigation only:

- `docs/DEFAULT_SEARCH_API_REVIEW_2026-11-02.md`: reproducible sanitized observation, contract gap,
  related-failure classifications and actual verification results.
- `docs/KNOWN_ISSUES.md`: link this unresolved issue and qualify the earlier contract-regression wording.

No confirmed automation fix was available. The unresolved blocker is the missing authoritative contract
and explanation for the deployed endpoint's empty data, not a compilation or offline regression.
