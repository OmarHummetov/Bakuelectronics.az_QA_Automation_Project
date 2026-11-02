# BE-UI-001 — Catalog keyboard focus and Escape dismissal

**Type:** accessibility / keyboard interaction. **Suggested severity:** medium; priority requires product triage. **Status:** historical reproduction confirmed; see fresh recheck status in [audit](../PRESENTATION_AUDIT.md). Production code cause remains unconfirmed.

## Environment and preconditions

- Public homepage: [Baku Electronics](https://www.bakuelectronics.az/), Azerbaijani UI, fresh unauthenticated Chrome profile.
- Desktop outer window 1440×1000; actual CSS viewport is captured in attachments. Dismiss the campaign modal using its visible close control. Use actual keyboard input; no forced click or synthetic DOM state.
- Automation: `KeyboardAccessibilityNavigationTest.keyboardCatalogHasVisibleFocusAndEscapeDismissal`; Phase 3 runner disables campaign suppression to avoid an artificial invisible backdrop.

## Reproduction

1. Open the homepage. Capture the catalog trigger's unfocused appearance.
2. Press Tab until `Kataloq` receives focus (bounded to 25 presses; discovery observed 12). Compare its visible appearance with the unfocused state.
3. Press Shift+Tab to the preceding logo, then Tab back to `Kataloq`.
4. If the menu is already open, use Enter on the focused trigger to close it. Press Enter again to open it.
5. Press Tab to the public search input and Shift+Tab back to the trigger. Press Escape once.

**Expected:** a visible keyboard-focus indicator; Enter opens the menu; Escape dismisses it and preserves/returns focus on the trigger. Visible focus is supported by [WCAG 2.4.7](https://www.w3.org/WAI/WCAG22/Understanding/focus-visible.html). Escape follows the [WAI-ARIA APG disclosure navigation example](https://www.w3.org/WAI/ARIA/apg/patterns/disclosure/examples/disclosure-navigation/), including ordinary site navigation without `role=menu`. APG is interaction guidance; do not infer a blanket WCAG Escape violation without checking the component's trigger/hover/focus semantics.

**Actual historical result:** `:focus-visible` matched, but computed outline was `none` and shadow/background/border did not change. Escape left the explicitly keyboard-opened catalog open. Tab, Shift+Tab and Enter worked; no trap was observed in this bounded journey. Both required assertions failed in one SoftAssert; focus remained on `Kataloq`.

## Evidence and interpretation

- [Phase 3 evidence](../ui-extension-phase3.md), final run `20261028-214402-cd85c4e5`: keyboard FAIL within 3 PASS / 1 FAIL; zero errors/skips.
- Local bundle: `target/allure/ui-extension-phase3/20261028-214402-cd85c4e5/`, raw `results/`, focus/menu attachments, failure screenshot and `report/index.html`.
- Fresh isolated recheck: `target/presentation-audit-20261029/defects-final/`; [audit](../PRESENTATION_AUDIT.md) records the actual outcome. Earlier sandbox setup failure is separate infrastructure evidence, not a product reproduction.
- Likely investigation areas: focus styling and keyboard close handling. CSS/JavaScript source, event listeners and pseudo-elements have not been inspected sufficiently to assign root cause. Computed styles alone cannot prove that every possible visual indicator is absent.

## Phase B evidence review — 29 October 2026

Retained raw result `c9baae45-a6bf-45aa-b8e2-64ef312ec153` reaches the keyboard assertions and records both focus and Escape failures. Escape leaves `megaMenu_show__8NYPv` present after settled observations. This is product interaction evidence; the end-of-journey screenshot and computed styles do not exhaustively establish absence of all visible focus indicators. Paired unfocused/focused screenshots, descendant/pseudo-element styles and event/focus observations are still needed for that stronger conclusion.

Mobile results `65280e7b-ca5a-4ae4-b321-1379b285d427` and `1095b495-a78e-4c62-b275-dd066b53d654` concern the suppression-created backdrop, a separate harness mechanism. ChromeDriver startup failures and recheck `013d8fa9-f80e-44de-8e22-40db8b1858be` (timeout at `HomePage.open()` / `driver.get`, before keyboard logic) are infrastructure observations. They neither reproduce nor resolve the keyboard symptom. Phase B changes only the suppression default and preserves both keyboard assertions. New execution outcomes are recorded separately in [AUTOMATION_FIX_REPORT.md](../AUTOMATION_FIX_REPORT.md); historical evidence is retained.

Fresh Phase B network-enabled result `c5ee3f26-9d85-4d46-8a2f-66b374ab5df9`, under `target/automation-fix-20261029-cd94e8d0/live-network/results/`, reached the original keyboard journey and again failed both original focus and Escape assertions. Suppression used the corrected default false, with no command-line override; retries were disabled. The initial restricted attempt under `live/` failed ChromeDriver host resolution before browser creation and skipped the method; it is separately retained infrastructure evidence. This fresh assertion failure strengthens reproducibility of the bounded symptom, while the paired-image/descendant/pseudo-element focus limitations above remain.

## Recheck command (live, guest read-only)

Run only in a suitable desktop Chrome environment; no login or basket changes. From the repository root with Java 23 selected:

```powershell
$keyboardRun = Join-Path $PWD ('target/keyboard-recheck-' + [guid]::NewGuid().ToString('N'))
mvn --batch-mode --no-transfer-progress '-Dtest=KeyboardAccessibilityNavigationTest' '-Dtest.retry.max=0' '-Dsurefire.rerunFailingTestsCount=0' '-Dui.campaignPopup.suppress=false' "-Dallure.results.directory=$keyboardRun/results" "-Dtest.reports.directory=$keyboardRun/surefire" test
```

**Acceptance:** visibly indicate keyboard focus, close the menu with Escape, retain logical trigger focus and preserve Tab/Shift+Tab/Enter navigation. Keep both assertions and verify supported desktop viewports. Azərbaycanca: kataloq fokusunun görünməsi və Escape ilə bağlanması yoxlanılır; bütün sayt üçün əlçatanlıq uyğunluğu iddia edilmir.

## Final verification — 29 October 2026

The unchanged TestNG method ran once with retries disabled and the corrected suppression default, completing **20:26:23 +04:00**: **1 FAIL, 0 errors/skips**. UUID `0f92ab42-753e-4467-891e-690c17fbbb1f`, under `target/remaining-failures-final-20261029/keyboard-live/results/`, records both original SoftAssert failures. Tab/Shift+Tab, Enter-open and search traversal passed their preceding assertions. After Escape, the settled state remained open with `megaMenu_show__8NYPv`; trigger focus was preserved. The retained failure screenshot visibly shows the open catalog without a campaign dialog or backdrop. Allure generation succeeded and independently reports one failed test; this is not a passing execution.

A separate temporary, read-only diagnostic captured paired `Unfocused-catalog-styles.png` and `Keyboard-focused-catalog.png` plus full computed styles in `target/remaining-failures-final-20261029/keyboard-probe/`. Visual inspection shows no visible focus indication on the catalog trigger. Both checkpoints had ready state `complete`, no visible matching campaign close/dialog/backdrop, and a center hit test reaching the trigger. The focused sample had active trigger focus and `:focus-visible=true`. Across **30 element/pseudo-element records** (trigger and nine descendants, each with normal, `::before` and `::after` styles), the only computed-style difference was trigger outline width **2.4px → 0px**; outline style was `none` in both. These observations strengthen the bounded visible-focus finding and exclude the previously identified suppression/backdrop mechanism at those checkpoints. They do not establish whole-site accessibility conformance or a production source-code cause.

Diagnostic limits are retained: its first restricted attempt failed before browser creation because the cached driver was not executable in the sandbox. A separate desktop-enabled attempt captured the pairs but stopped at the earlier Enter-open assertion (`open=false`); it did **not** reach Escape and is not counted as another TestNG result. Added screenshot/style collection changes journey timing, so the reason for that earlier stop remains unconfirmed. Escape reproduction above comes from the actual unchanged TestNG execution. Neither observation was discarded or converted into PASS/SKIP. See [final status](../REMAINING_FAILURES_FINAL.md) for all execution coordinates and remaining limitations.
