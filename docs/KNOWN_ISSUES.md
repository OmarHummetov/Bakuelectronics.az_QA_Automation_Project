# Known issues / Məlum problemlər

2 November 2026: the unsupported default-population scenario was replaced, one for one, by
`FastSearchExpandedContractTest.shouldReturnRelevantProductsWithValidSellingPrices`.
The website's verified `GET /fast-search-expanded?query=samsung` returns a populated `data` array.
The replacement validates the actual schema, every product's unique slug/nonblank title,
Samsung relevance, positive numeric price and valid discounted selling price. It is registered in
standard API, legacy combined and safe aggregate suites. The existing diagnostic suite retains
only POST-zero and unauthorized checks. No duplicate integration or extra test method was added.

**The original `/default-search-result` populated-data requirement remains CONTRACT_UNKNOWN.**
HTTP 200 with `{}` is still the verified behavior. The UI handles absent suggestions, but no authoritative
backend contract guarantees populated defaults or explicitly defines `{}` as valid. The old test's
historical FAIL remains a FAIL; replacement success applies to a different, verified operation.
`DefaultSearchAvailabilityTest` continues to validate default-endpoint transport/JSON/object/timing.
The shared reporter and unrelated API/integration assertions are unchanged.

Earlier investigations and historical outcomes remain in
[real search behavior evidence](DEFAULT_SEARCH_BEHAVIOR_2026-11-02.md) and
[original contract investigation](DEFAULT_SEARCH_API_REVIEW_2026-11-02.md).
The intermediate suite-only exclusion has been superseded by this source replacement.

Actual replacement verification (Oracle Java 23.0.2, retries/reruns disabled for live checks):

| Selection | Actual result |
| --- | --- |
| Standalone TestNG `-testclass az.bakuelectronics.automation.api.FastSearchExpandedContractTest`, no suite XML | **1 PASS, 0 FAIL, 0 SKIP**, exit 0 |
| Standard `suites/api.xml` with `-groups public-api` | **2 PASS, 0 FAIL, 0 SKIP**, exit 0 |
| Existing public `ProductApiUiConsistencyTest` | **1 PASS, 0 FAIL, 0 SKIP**, exit 0; exact slug/model and discounted price 899.99 AZN |
| Full offline allowlist, existing retry-policy fixtures retained | **330 PASS, 0 FAIL, 0 ERROR, 0 SKIP**, Maven exit 0 |
| Offline full-method/ordered-parameter identity comparison | **330 -> 330**, zero missing, added or non-passed records |
| Independent Java 23 compilation | **29 production + 108 test/support sources PASS** |
| Suite/runner validation | **PASS**, 10 standard + 2 diagnostic API scenarios; all 14 groups and 178 XML references valid |

The whole standard API suite was **not executed**: eight retained authenticated/mutation scenarios
remain unexecuted in this run. The six basket mutation scenarios use a live configured account;
exclusive disposable-account safety was not established for this task. The public group was selected
to respect the restriction against unsafe basket mutations. This is a partial standard-suite result,
not an all-10 PASS or eight SKIPs. All eight source assertions/registrations remain unchanged.

Evidence is isolated under `target/fast-search-contract-20261102/`: `expanded.json`, `compile.log`,
`javac-main.log`, `javac-test.log`, `individual.log`, `standard-public.log`, `integration.log`,
`offline.log`, `offline-invocation-comparison.json`, `membership.log` and separate result directories.
The individual command used the TestNG class runner, equivalent to a class-scoped TestNG selection;
the IntelliJ UI was not operated. No staging, commit or push occurred.

30 October 2026 API review: [API contract evidence and resolution requirements](API_CONTRACT_REVIEW.md).
POST-zero remains **CONTRACT_UNKNOWN**; unauthorized POST remains an **EXPECTED_SAFETY_BLOCK**, with its
guest/authentication contract unconfirmed. No live diagnostic or mutation was performed; no assertion or guard changed.
The dated findings below remain historical evidence.

Current cleanup status (30 October 2026): three obsolete blocked cart tests were removed, without changing either BE-UI defect test. Authenticated persistence and independent-summary consistency remain, with historical PASS and restored-basket evidence; no live execution occurred during cleanup. Their passing aggregate checks do not establish a displayed row-total field. See [cleanup report](PROJECT_CLEANUP_REPORT.md).

Historical status reviewed 29 October 2026 (Asia/Baku). Product observations, framework limitations and execution blockers are separate. A passing cleanup or generated HTML report never converts a failed test into a pass.

Final verification: [REMAINING_FAILURES_FINAL.md](REMAINING_FAILURES_FINAL.md) records Java 23 compilation and the unchanged **295/295 offline PASS** baseline, plus a new original keyboard **FAIL** at both focus/Escape assertions. New paired screenshots and trigger/descendant/pseudo-element observations strengthen the bounded focus finding; see [BE-UI-001 final verification](defects/BE-UI-001-keyboard-navigation.md#final-verification--29-october-2026). Earlier references below to missing paired evidence describe the earlier investigations. Phase 4 and unsafe API scenarios remain blocked; no new automation source fix was justified.

## Product findings

| ID | Observation and classification | Evidence / disposition |
| --- | --- | --- |
| BE-UI-001 | Catalog trigger has no detected visible keyboard-focus style; Escape leaves its keyboard-opened menu open. Accessibility/interaction finding; production implementation cause unconfirmed. | [Reproduction and expected behavior](defects/BE-UI-001-keyboard-navigation.md). Phase 3: 3 PASS / 1 FAIL. Phase B network-enabled recheck: keyboard FAIL at both original assertions; mobile PASS at both widths with corrected suppression default. Separate initial driver-setup failures did not reach product assertions. |
| BE-UI-002 | **APPLICATION BUG:** a fresh guest restores the filtered URL and descending product order, but the uniquely active visible desktop button is `Populyar`. The locator correctly reads it; four mobile options are hidden. Not evidence that prices sort incorrectly. | [URL, ordering, screenshots and locator investigation](defects/BE-UI-002-filter-deep-link.md). Eighteen restored observations through 10,507 ms retain the mismatch; independent fresh lookup agrees. Single-test recheck at **19:22:05 +04:00, 29 October 2026**: **1 FAIL**, original line 39; Allure retains 1 failed / 0 broken. Java 23 compilation and **291 offline PASS** verified; automation/assertion unchanged. Phase 5 historical 1 PASS / 1 FAIL remains separate. |
| BE-API-001 | **CONTRACT_UNKNOWN:** POST quantity zero returned HTTP 200 and changed the empty basket to quantity one. Neither a backend defect nor an incorrect test expectation is established without authoritative POST semantics. | [API findings](api-findings.md), run `20261023-010821-0ab3e1d7`: FAIL. R-all restoration passed; independent final basket snapshot absent. PUT-zero success does not define POST behavior. [30 October contract review](API_CONTRACT_REVIEW.md) retains the invariant and specifies missing authority. |

## Test and evidence limitations

- Invalid-slug rejection status/schema is not authoritative. Its passing response/unchanged-state checks do not establish a complete backend contract. Two retained opt-in diagnostic invocations (`a599284d-77aa-4281-b49f-9fb70bd27dfc`, `2d478c33-3e04-4562-9963-6432c5935f1c`) skipped at approval/isolation/token guards; neither observed a live invalid-slug response.
- Focus detection compares outline, shadow, background and border. It does not inspect every possible descendant, pseudo-element or pixel-level indicator. Review the retained screenshot before making a whole-site WCAG conformance claim. The bounded keyboard journey is not a full accessibility audit.
- Phase 3 installment checks validate displayed consistency for observed providers/terms, not financing policy or a universal rounding formula. Two mobile widths in one method are not two independent tests.
- Fixed product fixtures, localized text and generated CSS class names can change. Explicit waits and identity checks reduce ambiguity but do not make live tests deterministic. Chrome 156 / Selenium CDP 155 mismatch warnings appeared during the read-only recheck; the warning alone is not a failed assertion.
- `CartSnapshot.Row.calculatedTotal()` is derived unit price × quantity. It is not a separately displayed row-total field. Synthetic XML/proxy tests do not validate the prospective multi-row JavaScript against a real populated cart.
- Parallel coverage is a three-class, two-thread read-only pilot. No general parallel UI/API mutation reliability or current pilot execution is claimed.
- Historical suites, the 14-run overall aggregate, newer individual/category runs and today's offline verification are separate evidence sets. No current all-green full live regression exists.
- Legacy API/UI search discoverability remains unresolved: later direct product-detail navigation does not reproduce or resolve the original missing-search-result symptom. Availability/search-index contract and contemporaneous complete network evidence are missing.
- Phase B corrects the confirmed remaining harness default: campaign suppression now defaults to false, allowing normal close-button dismissal. Explicit true remains opt-in and can reproduce the historical invisible-backdrop risk. See [fix report](AUTOMATION_FIX_REPORT.md) for before/after checks and remaining live limitations. No asynchronous close/backdrop wait change is inferred from the available evidence.

## Execution and release blockers

| Blocker | Required acceptance evidence |
| --- | --- |
| Unauthorized basket POST is blocked before transport | Retained invocation `e55e84fb-0db9-4429-9e15-d78b19202c91` is a guard FAIL, not an HTTP 401/403 observation. Verify isolated guest identity, same-basket baseline and safe recovery without authenticated fallback before any live negative request. No fabricated skipped/passed result. |
| Public report publication needs privacy review | Original reports retain host/thread metadata, local paths and browser screenshots/URLs. Review a separate sanitized export; preserve original failures and source reports. |
| Remote CI status is unverified | Workflow source proves Java 23 compile + full offline profile only. Obtain an actual GitHub Actions run before claiming a hosted pass or displaying a passing badge. |
| Keyboard finding remains open after Phase B recheck | The initial restricted run failed driver setup; a separate network-enabled run reached both original assertions and failed focus/Escape again (`c5ee3f26-9d85-4d46-8a2f-66b374ab5df9`). See [Phase B evidence](AUTOMATION_FIX_REPORT.md). Preserve earlier navigation timeouts as infrastructure; paired visual focus evidence is still incomplete. |

The repository can be presented as a QA portfolio with disclosed findings and blocked acceptance. It is not ready for a claim of complete, fully passing live regression or unrestricted report publication. See [audit](PRESENTATION_AUDIT.md) for exact verification and change boundaries.

## Azərbaycanca

Məhsul tapıntıları, test məhdudiyyətləri və icra blokları ayrıdır. Fokus/Escape və deep-link sıralama göstəricisi qüsurları saxlanılır. Deep-link məhsulları həqiqətən azalan qiymətlə bərpa edir; problem seçilmiş düymənin `Populyar` göstərməsidir. Backend səbəbi sübut olunmayıb.

Mərhələ 4-ün iki qəbul edilməmiş qonaq testi və ayrıca sətir cəmi tələb edən test silinib; tarixi nəticələr saxlanılıb. Unauthorized POST HTTP sorğusundan əvvəl bloklanır. CI yalnız offline dəsti icra edir; uzaq CI uğuru yoxlanılmayıb. Tarixi hesabatlar anonim deyil və açıq paylaşmadan əvvəl ayrıca məxfilik yoxlaması tələb edir.
