# Repository guidance

## Project
- Baku Electronics QA Automation final project: Java 23, Maven, Selenium, TestNG, REST Assured, Page Object Model (POM), and Allure.
- Course coverage requirements: at least 10 UI tests, 6 API tests covering GET and POST plus PUT or PATCH and DELETE, and 1 UI+API integration test. Preserve this coverage when changing tests.
- Main code is under `src/main/java/az/bakuelectronics/automation/` (`api`, `config`, `driver`, `models`, `pages`, `utils`). Tests are under `src/test/java/az/bakuelectronics/automation/` (`ui`, `api`, `integration`, `support`, `unit`).
- `pom.xml` defines dependencies/build; `testng.xml` is the full regression suite; CI compiles and runs `WaitConditionResultTest`.

## Implementation rules
- Preserve the existing architecture and working tests. Follow established POM and `DriverManager` patterns.
- Keep UI, API, unit/support, and integration tests in their existing areas. Maintain TestNG `smoke` and `regression` groups and relevant API/integration labels.
- Use meaningful assertions, stable locators, and explicit waits; avoid fixed sleeps where a wait can express the expected state.
- Protect credentials, tokens, cookies, and real account data. Do not add secrets to source, test data, reports, or logs.
- Preserve basket state. Mutation tests must capture/restore state or use the existing safe cleanup support where applicable.
- Never run destructive tests or tests against live accounts without explicit approval. Treat authenticated basket/API and UI mutation tests as state-changing.
- Prefer minimal diffs. Avoid unnecessary dependencies, duplicate helpers, and broad refactoring. Do not modify unrelated files.
- Verify affected code with focused tests when safe and within the requested scope. Never report tests as passed unless they were executed successfully; state clearly when verification was not run.

## Generated files and responses
- Ignore generated/local output when inspecting or editing: `target/`, `.allure/`, `.firecrawl/`, and `.git/`.
- Keep responses concise: list changed files, verification results, and any blockers.
