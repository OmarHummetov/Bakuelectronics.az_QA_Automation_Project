# Authenticated Phase 4 follow-up — 30 October 2026 (Asia/Baku)

> Historical record. On 30 October 2026, cleanup removed the two never-accepted guest cart tests and the authenticated independent-row-total test, plus their obsolete guest suite/runner. Names, counts, commands and unchanged-file statements below describe that earlier execution only. Archived commands for removed targets are not current instructions. See [current inventory and verification](PROJECT_CLEANUP_REPORT.md).

This record supplements `PHASE4_AUTHENTICATED_EXECUTION_2026-10-30.md`; all earlier results and failures
remain unchanged. Account-owner authorization from the chat covers normal login and cart operations.
No checkout, payment, order, profile change, unrelated API mutation, Git staging, commit, or push is authorized or performed here.

## Authentication diagnosis and preflight

The historical failed result retains only a `TimeoutException` stage and a deliberately redacted stack.
Its 180-second wait is the post-password conjunction of **exact configured homepage URL** and
**title containing Baku Electronics**. The redacted historical artifact does not retain which operand
was false or the original page state. It cannot establish a specific SSO, network, MFA, or locator root cause.
No unproven authentication defect is fixed by relaxing this guard, increasing a timeout, or adding retries.

The protected flow now records fixed checkpoints, monotonic elapsed milliseconds, and separate URL/title
booleans. A failure records the exact checkpoint and first project stack frame plus only page-state
booleans: expected origin/root, query/fragment presence, SSO host, title/readiness, and visibility of
password, OTP, CAPTCHA, profile, campaign, and rejection controls. No URL, query value, page text, field
value, credential, cookie, token, or screenshot is recorded. Existing sensitive exception redaction remains in place.

`AuthenticatedCartPreflightTest#freshAuthenticationAndReauthenticationPreserveBasketWithoutMutation`
passed independently before any cart additions in this follow-up. It authenticated, captured the complete
basket, closed the browser, reauthenticated, refreshed, and verified the same basket and any original price
snapshot. It performs no cart mutation and creates no ownership journal. Both the original exact URL/title
requirements and visible authenticated profile/logout/orders checks remain mandatory.

In this preflight the first post-password landing completed in about 8.5 seconds and the fresh-browser
landing in about 13.8 seconds. Both URL and title booleans changed from false to true together. The old
180-second timeout did not reproduce. Historical causation beyond the identified compound wait remains
unproven; successful new runs must not be presented as proof of that missing historical page state.

## Multi-product execution

The first follow-up multi-product invocation reached and passed:

- Exact product identities and A=2/B=1 quantities.
- Unchanged original/discounted unit prices, exact BigDecimal basket/discount arithmetic, and A-only total delta.
- Remove A with B's complete row unchanged and all original account rows preserved.
- Remove B and verify the original inventory.
- Fresh-browser account reauthentication and refreshed original-basket restoration; ownership journal cleared.

The observed A unit price was **1449.99 AZN**, B unit price **59.99 AZN**. The expected test-owned line amounts
were **2899.98 AZN** for A and **59.99 AZN** for B (test-owned combined amount **2959.97 AZN**).
A's independently read main-row current-price field displayed **1449.99 AZN** at quantity two. It therefore
does not establish the required independently displayed A line total. The whole test is honestly
**BLOCKED / Allure skipped**, despite the other checks passing. Derived line amounts are not substituted.

A separate DOM investigation records all numeric leaf fields within the verified test-owned main row and
its exact matching summary row, rather than assuming that a field containing `PriceCurrent` is a line total.
It excludes account-wide content, product names, URLs, arbitrary attributes, and all input values. Each
invocation uses a separate report directory with retries disabled and restoration reviewed before another mutation.

The separate DOM investigation reached the same A=2/B=1 state. Its main-row numeric leaf fields were
`1599.99 ₼`, `1449.99 ₼`, and `2`; the exact matching summary row contained `1599.99 ₼`, `1449.99 ₼`,
and `2 ədəd`. These are old/current unit prices and quantity. Neither row exposed a separate `2899.98`
line amount. This independently confirms the missing displayed field in both relevant row structures;
it does not establish an incorrect aggregate calculation. No new application defect report is created
without a product requirement or mislabeled total establishing a product defect.

Both multi-product invocations have **zero failures and one skip**, representing the explicit unavailable
displayed-line-total prerequisite. Both completed the genuine aggregate arithmetic and remove-A/preserve-B
checks, removed B, and verified the original basket through fresh-browser reauthentication and refresh.
The ownership journal was absent after each verified restoration. No skipped result is reported as PASS.

Authenticated persistence reverification: **PASS**, one invocation, zero failures/skips. It exercised the
shared protected flow and numeric evidence helper, verified refresh/navigation/browser-closure persistence,
removed only A, and verified original-basket restoration through fresh authentication and refresh.

Complete offline regression: **320 PASS**, zero failures/errors/skips, on **Java 23.0.2**. Java 23 compilation
succeeded. All 317 previous invocations were preserved: testcase multiplicity comparison found zero
missing or reduced original cases. Three new focused landing-wait tests were added. The separate focused
run passed all four invocations (three new checks plus the existing expanded exact-method approval test).
All previously passing focused cases are also retained in the complete passing suite.

Across the read-only preflight, both multi-product invocations, and persistence, all **12** actual login
cycles passed their unchanged URL/title and authenticated-drawer checks. Recorded post-password landing
waits ranged from **8049 ms to 13839 ms**. A safe read-only inspection during persistence found the expected
homepage with HTTP navigation status 200 and no visible password, OTP, or rejection state. No failure was
reproduced that justifies altering authentication criteria. The exact historical timeout cause remains
unproven, rather than being attributed to an unverified website, network, MFA, or automation defect.

Final basket restoration is verified after every live cart invocation. No pending ownership journal
remains. Both original guest tests and both existing BE-UI defect reports are byte-for-byte unchanged.
No new product calculation defect was independently established. Full multi-product acceptance remains
**BLOCKED by the absent independent displayed A line total**; passing arithmetic/cleanup subsets are not
represented as a passing whole test.

## Files changed in this follow-up

- `src/main/java/az/bakuelectronics/automation/pages/LoginPage.java`: named protected-flow checkpoints and separately observed URL/title wait conditions; acceptance criteria unchanged.
- `src/test/java/az/bakuelectronics/automation/support/AuthenticatedCartSupport.java`: safe timed authentication diagnostics and numeric DOM evidence restricted to owned rows.
- `src/test/java/az/bakuelectronics/automation/ui/AuthenticatedCartPreflightTest.java`: independent read-only login/reauthentication/basket preflight.
- `src/test/java/az/bakuelectronics/automation/support/SensitiveTestReporting.java`: allow only that exact additional preflight method under personal-account authorization.
- `src/test/java/az/bakuelectronics/automation/support/AuthenticatedUiApprovalTest.java`: cover the new exact selection and existing rejected selections.
- `src/test/java/az/bakuelectronics/automation/support/AuthenticatedLandingWaitTest.java`: three focused offline checks for redirect/title readiness, mandatory origin/title conditions, and diagnostic confidentiality.
- `src/test/java/az/bakuelectronics/automation/ui/AuthenticatedMultipleProductsCartCalculationTest.java`: fixed PASS evidence for completed price/removal assertions and test-owned displayed monetary amounts.
- `testng-offline.xml`: append the three new offline invocations; preserve the previous 317.
- This follow-up record.

`CartPage`, `CartSnapshot`, ownership/recovery rules, the two guest tests, and both existing BE-UI defect
reports are preserved. The prior hover fix is reused. The original guest scenarios retain their historical
BLOCKED / not executed in guest mode status. Preservation fingerprints for both guest tests and both
defect reports are stored under the new run root for final comparison.

## Isolated evidence

Run root: `target/phase4-auth-completion-20261030/`.

- `focused-diagnostic/report/index.html`: four passing focused invocations, including the existing approval check and three new checks.
- `preflight-diagnostic/report/index.html`: actual read-only preflight PASS.
- `multiple-1/report/index.html`: actual BLOCKED/skipped result, with passed partial assertions and restoration evidence.
- `multiple-dom/report/index.html`: separate numeric DOM investigation.
- `persistence/report/index.html`: shared-code persistence reverification.
- `offline-final/report/index.html`: final complete offline regression.

Machine-readable summaries: `outcomes.json` and `sso-timings.json` under the same run root.
Artifact review found zero configured credential-value matches and zero live result image files.
