# Phase 4 readiness audit — 29 October 2026, Asia/Baku

> Historical record. On 30 October 2026, cleanup removed the two never-accepted guest cart tests and the authenticated independent-row-total test, plus their obsolete guest suite/runner. Names, counts, commands and unchanged-file statements below describe that earlier execution only. Archived commands for removed targets are not current instructions. See [current inventory and verification](PROJECT_CLEANUP_REPORT.md).

**Both live tests remain BLOCKED. No live browser/cart operation was performed in this audit.**
This document supplements the historical [Phase 4 implementation](ui-extension-phase4.md) and
[safe live regression](SAFE_LIVE_REGRESSION.md); their results and known defects are retained.

## Decisions and outstanding evidence

| Test | Decision | Concrete evidence and missing acceptance |
| --- | --- | --- |
| `GuestCartPersistenceTest.productAndQuantityPersistWithinGuestSession` | **BLOCKED** | Java setup and PowerShell runner still stop before browser creation. The body retains exact identity/quantity, strict single-product arithmetic, refresh and home/return snapshot equality, and finally cleanup assertions. Actual execution of the new JavaScript reader in a populated guest browser, persistence and same-session recovery are unverified. This body uses A only; it does not itself demonstrate two-product recovery or an independent displayed line total. |
| `MultipleProductsCartCalculationTest.twoProductsCalculateAndRemoveIndependently` | **BLOCKED** | Same guards. No observed populated A/B DOM or actual JS execution, independently displayed row total, or same-session A removal/B preservation/recovery evidence. The body retains A=1/B=1, A=2/B=1, unchanged unit prices, exact aggregate delta, and complete unchanged B row assertions. |

The following assumptions still need evidence, rather than an approval flag or a blind rerun:

1. Current populated DOM: the control-row XPath selects exactly the actual basket rows, including responsive/hidden copies;
   each has one identity, title, plus/minus pair and unambiguous removal control. Summary CSS prefixes still identify the
   intended visible fields, and the actual `INVENTORY_SCRIPT`/`MULTI_SUMMARY_SCRIPT` return those values in a browser.
   Proxy/XML tests validate contracts/XPath only, not browser layout or JavaScript behavior.
2. Displayed prices: old/current fields are **unit** prices at quantities one and two, summary quantity agrees with the
   control, discount is the magnitude of the summed unit savings, and no unsupported charge exists (even a zero fee is rejected).
   `calculatedTotal()` is unit price times quantity, not an independently observed displayed row total.
   Identify an actual independent row-total field and add its assertion with reviewed evidence. If the application has no
   such field, record that requirement as blocked pending an explicit scope decision; never fabricate it from unit price.
3. Recovery: in an approved non-production isolated guest session, establish add/add/increment, remove A while retaining B,
   then remove B and observe the empty message and settled empty inventory. Validate delayed/ambiguous dispatch, a partial
   removal failure, and preservation of an unrelated sentinel product in a separate recovery fixture. Browser/session loss
   cannot be recovered by these DOM-only helpers; it must stop execution with recovery unverified.
4. Fixtures and identities: A (`201123`) and B (`223041`) must currently be available at the exact configured routes, with
   distinct codes and stable captured names/prices. No substituted product or historical cached availability is sufficient.
   Summary mapping requires unique normalized names. Variant identity is not represented; use only fixtures uniquely
   identified by slug/code. Query/fragment-bearing links and other unobserved route forms still require fixture validation.
5. Isolation: fresh unauthenticated Chrome, no shared profile/imported storage/account credentials, same session until
   restoration, no external tab/operator or concurrent basket runner. The JVM/file lock coordinates cooperating runners
   only. It cannot protect against an external actor adding the same slug as an owned product.

The shared safety guard currently applies the stronger Phase 4 prerequisites to both tests. Neither is live accepted,
and no live PASS/FAIL/SKIP result is invented by this document.

## Confirmed framework fixes

- **Survivor expectation rebasing:** pending removal previously kept its dispatch state but recalculated survivors on each
  call. A later cleanup could accept a survivor quantity changed after the original click. The original survivor map now
  remains attached to the pending command, including through the final settled-inventory check. Re-observation never
  dispatches another removal, and delayed completion can be confirmed even after the target is absent.
- **Recommendation interference:** owned removal previously checked every matching product link under `main` for absence.
  A remaining recommendation/title copy could prevent confirmation after the actual cart row disappeared. Owned removal
  now uses the complete control-row inventory. Legacy single-product removal retains its empty-cart assertion.
- **Ambiguous removal action:** owned removal previously selected the first non-plus/minus button. It now refuses to click
  unless exactly one visible candidate exists. Actual semantics of that sole control still require DOM/recovery evidence.
- **Route inconsistency:** inventory accepted absolute product links ending in `/`, while the mutation-row locator did not.
  The locator now accepts that form too; an XML check evaluates the actual mutation XPath against the absolute route.
- **Parser boundary:** malformed non-string/null row fields now produce a deliberate validation error; duplicate normalized
  names are rejected in Java as well as JavaScript, and summary amount types are checked. BigDecimal calculations and the
  distinction between derived and displayed totals are unchanged.

Changes are backward compatible. No UI/API/integration assertion, suite membership, retry policy, legacy guest cleanup,
authenticated API cleanup, known application defect or historical report was removed or weakened.
The Phase 4 offline runner's expected result count is updated for the expanded contract class; both live guards remain.

## Cleanup guarantees and limits

`OwnedGuestCart` holds the mutation lock from baseline verification through restoration. It refuses a nonempty baseline
without running the scenario or cleaning it; it registers each slug before an add may dispatch. Cleanup visits only owned
slugs, skips already absent rows, and attempts remaining owned rows after a per-row failure. Unowned rows are never passed
to the removal method. If an unrelated product appears, it stays in the cart and the final empty-baseline assertion fails.
The helper does not claim to restore arbitrary nonempty baskets or merge concurrent same-slug changes.

Offline fixtures demonstrate empty restoration for two owned products and partial-removal recovery, with foreign products
preserved. Failed cleanup fails an otherwise successful scenario; on scenario failure it is suppressed on the original
failure. Navigation failure prevents blind deletion and remains visible. Pending-removal tests exercise the actual
`CartPage` state machine through proxy controls, not just an overridden cleanup method. This proves the bounded algorithm,
not the behavior of the live application. Recovery failures are not converted to PASS or retried blindly.

## Verification

Java 23 verification completed at **23:13:58 +04:00**. All audit outputs are isolated under
`target/phase4-readiness-20261029/`; historical directories and report pointers are not reused.

| Verification | Actual result | Evidence |
| --- | --- | --- |
| Pre-change complete offline baseline | **295 PASS**, zero failures/errors/skips | `baseline/maven.log`, `baseline/surefire/`, `baseline/results/` |
| Final Phase 4 offline runner | **42 PASS**, zero failures/errors/skips; Maven/Allure exits 0 | `runner.log`; fresh report linked below |
| Final complete offline suite | **310 PASS**, zero failures/errors/skips; Maven exit 0 | `final/maven.log`, `final/surefire/`, `final/results/` |
| Invocation preservation | **295 original + 15 new**; zero missing original keys | `invocation-comparison.json`; fullName plus ordered parameter names/values, with multiplicities |
| Historical baseline cross-check | Historical safe-live-regression offline 295 and this baseline have zero missing/added keys | Same comparison JSON; historical source `target/safe-live-regression-20261029-d5a45213/offline/results/` |
| Guard checks | Java setup refusal PASS as an offline contract; live runner rejected before Maven/browser | `unverifiedRecoveryBlocksBeforeBrowserCreation`; `live-guard.log` |
| Static Allure workflow | PASS; no live tests invoked | `scripts/Test-AllureWorkflow.ps1` console result |

Fresh focused Allure report (offline contracts only, **LIVE BLOCKED**):
[42-check report](../target/allure/ui-extension-phase4/20261029-231008-45e6eb08/report/index.html).
This report's `run.json` records 42 passed results and both exit codes zero. Report generation itself is not a live PASS.
The complete suite retains its baseline retry configuration because its fixtures test retry policies; the focused runner
sets both retry limits to zero. Synthetic nested failure messages in the full log are expected fixtures; the outer suite
and all 310 raw results passed.

The first expanded focused run had **39 PASS / 1 FAIL**: an expanded fixture accidentally changed survivor B before the
click, so the existing removal-time survivor-change assertion no longer exercised a change. The fixture was corrected,
the original assertion retained, and the next 41-check focused run passed. One additional route check then produced the
final 42-check runner above. These attempts remain in `focused/` and `focused-final/`; none was overwritten or relabeled.
The initial baseline and first focused build briefly overlapped after baseline compilation; the final focused runner and
full suite ran sequentially after source edits. No parallel-build reliability is claimed.

Changed files for this audit only (the checkout already contained unrelated changes):

- `src/main/java/az/bakuelectronics/automation/pages/CartPage.java`
- `src/main/java/az/bakuelectronics/automation/pages/CartSnapshot.java`
- `src/test/java/az/bakuelectronics/automation/unit/AdvancedGuestCartTest.java`
- `scripts/Run-UiExtensionPhase4.ps1`
- `docs/ui-extension-phase4.md` (addendum link; historical text retained)
- `docs/PHASE4_READINESS_AUDIT.md` (this report)

## Safe sequential live acceptance procedure

1. Keep both guards enabled while collecting reviewed populated DOM and recovery evidence in a local/staging fixture.
   Execute the actual JavaScript readers and actual Selenium row actions there, including two rows, duplicated links,
   hidden copies, supported routes, price arithmetic, delayed completion, partial failures and foreign-row preservation.
   Do not use production cart mutations to discover a missing recovery contract.
2. Resolve the displayed-row-total requirement using actual evidence; add the missing assertion if the field exists.
   Review selectors and the exact removal action, fixture identities and both guards in a code change. Re-run the complete
   offline suite, retaining the original 295 invocation identities and all live assertions. A parameter cannot bypass guards.
3. Obtain explicit authorization for the bounded live guest mutations after prerequisites are satisfied. This audit grants
   no such authorization. Reserve exclusive execution, disable automatic retries/Surefire reruns, and preserve fresh output
   directories per attempt. Use Java 23, `parallel="none"`, `thread-count="1"`, `test.retry.max=0` and
   `surefire.rerunFailingTestsCount=0`. Maven dependency offline mode does not prevent application traffic.
4. Execute persistence alone in a fresh guest browser. Verify its empty baseline in that same browser, available A,
   single add and exact quantity/pricing, refresh, home/return equality, then verified empty cleanup before quitting.
   Review the actual test and cleanup outcomes before proceeding; do not continue after an unverified cleanup.
5. Execute calculation alone in a separate fresh guest browser: empty baseline; inspect A/B; single adds; A=1/B=1;
   one increment to A=2/B=1; independent displayed totals and aggregate/discount; remove A once; identical B row;
   finally remove remaining owned B and confirm settled empty baseline. Keep the guest session alive through restoration.
6. On an ambiguous action, observe only; never send a replacement click. Preserve primary and cleanup errors, retain
   sanitized evidence and stop further scenarios when recovery is unverified. Never clear the whole basket or remove an
   unowned slug. Session/driver loss requires an operator recovery decision, not a fresh browser claimed as restoration.
   Report actual outcomes separately from offline checks and report generation. Existing BE-UI-001, BE-UI-002 and
   contract-unknown BE-API-001 remain historical unresolved findings outside this audit.

No staging, commit, push, login, authenticated basket access, checkout, order submission or live cart mutation is part
of this audit. Production acceptance remains blocked until the above evidence and required assertion are supplied.
