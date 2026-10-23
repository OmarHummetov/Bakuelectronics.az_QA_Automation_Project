# Authenticated UI execution

`AccountLoginTest` retains `regression`, `sensitive-auth`, its inherited sensitive class marker
and `NoRetryAnalyzer`. The marker protects browser setup, test execution and cleanup.
Shared basket cleanup, locking, API authentication and negative-login protections remain
in place for their existing consumers.

Bare Maven and CI use the offline allowlist. Maven excludes `manual-auth,sensitive-auth`
by default; the full TestNG XML excludes both groups from its UI package scan. The safe
read-only and parallel pilot suites do not select AccountLoginTest. Overriding exclusions alone
cannot authorize execution: every before-method configuration and test body requires
one exact Maven `Class#method` selection, execution approval and exactly one account
approval mode. Wildcards, class-only/multiple-method selections, group selections and
Surefire reruns are rejected before browser setup. IDE/XML execution without these
conditions fails configuration before side effects.

After account-owner approval, provision `BAKU_ELECTRONICS_LOGIN` and
`BAKU_ELECTRONICS_PASSWORD` privately through the Maven process environment. Never put
values in commands, Maven properties, files, transcripts or debug output. Use Java 23,
Maven, Chrome and a compatible ChromeDriver; cache Maven dependencies for the command
below. Confirm the configured UI/SSO environment before login.

`authenticated.ui.dedicatedAccount=true` remains the dedicated-account attestation.
For an explicitly approved personal account instead use
`authenticated.ui.personalAccountApproved=true` and `authenticated.ui.dedicatedAccount=false`.
Both true, neither true, or missing general approval fail closed. Personal approval
applies only to `AccountLoginTest#configuredIdentifierCanBeEnteredInTheCorrectSsoMode`; it cannot
enable negative-login or other tests. Flags do not grant account-owner authorization.
The wrong-password test retains its separate opt-ins and dedicated-account requirement.

Login enters the identifier/password, authenticates and inspects the account drawer.
It does not click orders/logout or modify account settings, orders or basket contents.
Authentication creates server sessions; browser shutdown does not explicitly log out.

After explicit approval, run AccountLoginTest separately:

```powershell
mvn --offline --batch-mode --no-transfer-progress '-Daether.syncContext.named.factory=rwlock-local' '-Dtest=AccountLoginTest#configuredIdentifierCanBeEnteredInTheCorrectSsoMode' '-Dtest.excludedGroups=manual-auth' '-Dauthenticated.ui.approved=true' '-Dauthenticated.ui.personalAccountApproved=true' '-Dauthenticated.ui.dedicatedAccount=false' '-Dsurefire.rerunFailingTestsCount=0' test
if ($LASTEXITCODE -ne 0) { throw 'Login failed; investigate before further execution.' }
```

`--offline` prevents Maven downloads, **not browser traffic** in this explicit UI run.

Quantity 1-to-0 removal is covered separately by `CartZeroQuantityRemovalTest`,
using a fresh anonymous browser, an empty baseline, one product and targeted finally
cleanup. It uses no authenticated API basket or identity configuration and does not
establish UI/API account identity equivalence. No authenticated identity path/field
settings are currently required or consumed by the project.

Sensitive failures discard messages, causes and suppressed exception contents before
TestNG/Allure consume them. Failures remain failures; cleanup errors still fail tests
and release basket locks. Intentional pre-existing skips retain skip semantics.
No screenshot, page source or URL diagnostics are collected for sensitive classes.
Cleanup attachments/logs use fixed redacted text. Ordinary non-sensitive diagnostics
and screenshots remain available. Offline reporting tests exercise synthetic
setup/test/teardown/cleanup failures through actual TestNG and Allure writers.

Basket restoration for the remaining API/integration tests is attempted and verified,
not guaranteed across network failures, expired authentication, forced termination or
external account use. Reserve mutation accounts exclusively across machines/jobs, keep
a manual recovery plan and verify cleanup before further mutations.

Keep HTTP/browser tracing and screen recording disabled. These protections cannot
sanitize arbitrary future output/attachments, external tracing or artifacts from earlier
runs. Do not publish old artifacts containing private data. This cleanup change was
verified offline only; no live execution or account access was performed.
