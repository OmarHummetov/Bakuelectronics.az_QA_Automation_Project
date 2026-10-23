# Invalid product slug: contract evidence and optional diagnostic

Current filename equivalents: [test naming inventory](test-naming-inventory.md). The committed-version evidence below retains its historical name.

Local inspection on 2026-10-21 found no Swagger/OpenAPI/Postman artifact or documented
expected HTTP status for authenticated `POST /addbasket` with a nonexistent slug.
README's API table identifies the endpoint and negative test, but specifies no status.
The only available committed version of `InvalidProductSlugTest` (initial commit
`281c67f`) also has no status assertion. Successful add tests and the unauthenticated
request test describe different cases; their statuses do not establish this contract.
Generated `target/`, `.allure/`, `.firecrawl/` output is excluded by AGENTS.md and was
not inspected. No live documentation or endpoint requests were made.

As of 2026-10-22, `BasketInvalidSlugResponseTest` retains its response assertions and adds
locked baseline capture/verification, whole-basket unchanged checks and always-run
snapshot restoration with lock release. It still has no expected status assertion.
An observed status, even if repeatable, is implementation evidence only.
Obtain an API-owner-approved specification for the
same authenticated request, environment and version before adding an exact assertion.

## Optional manual diagnostic (not executed)

1. Obtain explicit approval for one invalid-slug POST, basket GETs, and conditional
   cleanup DELETEs. This document and the opt-in flags do not themselves grant approval.
2. Provision an isolated disposable test account. Never use a personal account.
   Ensure its basket is empty, it has no sensitive account data, and no other process
   or person uses it during the diagnostic. Provision a short-lived bearer token for
   that account through the approved account workflow outside this diagnostic.
3. In a fresh terminal/JVM, inject `BAKU_INVALID_SLUG_TOKEN` securely from your secret
   manager (no token literals in commands, history, source or chat). The diagnostic
   uses this variable only; it does not log in or use ordinary credentials/cached tokens.
   Set the attestation only after verifying the token belongs to the isolated account:

   ```powershell
   $env:JAVA_HOME = 'C:/Program Files/Java/jdk-23'
   $env:BAKU_INVALID_SLUG_ACCOUNT = 'isolated-test-account'

   mvn --offline --batch-mode --no-transfer-progress '-Daether.syncContext.named.factory=rwlock-local' '-Dtest=InvalidProductSlugDiagnosticTest#observeInvalidSlugOnceOnIsolatedAccount' '-Dtest.excludedGroups=' '-Dinvalid.slug.diagnostic.approved=true' '-Dsurefire.rerunFailingTestsCount=0' test
   ```

   This targets the repository's configured API base URL and basket read endpoint
   (defaults: `https://5vyra9x.bakuelectronics.az`, `/getBasket`). Confirm those locally
   before execution; use the existing environment configuration for another approved
   environment. `--offline` disables Maven downloads, **not the diagnostic's HTTP calls**.
   Run only this method, without HTTP/debug logging, additional listeners, retrying
   runners, parallel suites or other account activity. Do not rerun a failed attempt
   without investigating and obtaining fresh approval.
4. The diagnostic is excluded by default via `manual-auth`, requires explicit opt-ins,
   and permits one invocation per JVM. It disables TestNG and HTTP retries and redirects.
   It locks basket access and requires a readable, empty baseline before the single
   invalid-slug POST. It compares snapshots even if the POST returns a successful status;
   transport/parse failures still enter cleanup. Cleanup discovers all changed lines,
   conditionally removes new lines, and verifies restoration using the existing support.
   There are no setup add/update calls and no second invalid-slug POST.
5. Response output contains only status, allowlisted normalized Content-Type (parameters
   removed; unknown types become `other`) and present allowlisted field names
   `detail`, `message`, `error`. Values, other headers, arbitrary field names and bodies
   are never recorded. Basket identifiers are aliased before reaching cleanup reports;
   exceptions are sanitized. Cleanup emits operational success/failure information.
   If the basket cannot be parsed/restored or a line cannot safely be removed using the
   existing slug-based gateway, the diagnostic fails: inspect the isolated account
   manually and do not retry automatically.
6. Remove `BAKU_INVALID_SLUG_TOKEN` and `BAKU_INVALID_SLUG_ACCOUNT` from the terminal
   environment and revoke the short-lived token afterward. Share only the sanitized
   observation and cleanup result with the API owner to request the intended contract.

Account isolation is an operator attestation, not something a bearer token can prove
locally. An empty basket alone does not establish that an account is a test account.
