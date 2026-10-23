
[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
$workflow = Join-Path $PSScriptRoot 'Allure-Category.ps1'

$mavenProbe = @{ Invoked = $false }
function mvn { $mavenProbe.Invoked = $true; throw 'Maven invocation forbidden during static workflow checks.' }
function Require($condition, $message) { if (!$condition) { throw $message } }
function Expect-Rejection([scriptblock]$operation, [string]$message) {
    $rejected = $false
    try { & $operation | Out-Null } catch { $rejected = $true }
    Require $rejected $message
}
function Read-Xml($path) {
    $settings = New-Object System.Xml.XmlReaderSettings
    $settings.DtdProcessing = [System.Xml.DtdProcessing]::Ignore
    $settings.XmlResolver = $null
    $reader = [System.Xml.XmlReader]::Create($path, $settings)
    try { $xml = New-Object System.Xml.XmlDocument; $xml.Load($reader); return ,$xml } finally { $reader.Dispose() }
}
$sources = @{}
foreach ($file in Get-ChildItem "$repo/src" -Recurse -Filter '*.java') {
    $text = Get-Content $file.FullName -Raw
    $package = [regex]::Match($text, '(?m)^package ([\w.]+);').Groups[1].Value
    $sources["$package.$($file.BaseName)"] = $text
}
foreach ($file in Get-ChildItem $PSScriptRoot -Filter '*.ps1') {
    $parseErrors = $null; $tokens = $null
    [System.Management.Automation.Language.Parser]::ParseFile($file.FullName, [ref]$tokens, [ref]$parseErrors) | Out-Null
    Require (!$parseErrors.Count) "PowerShell parse error: $($file.Name): $parseErrors"
}
$readme = Get-Content "$repo/README.md" -Raw
$snippets = [regex]::Matches($readme, '(?s)```powershell\r?\n(.*?)```')
foreach ($snippet in $snippets) {
    $parseErrors = $null; $tokens = $null
    [System.Management.Automation.Language.Parser]::ParseInput($snippet.Groups[1].Value, [ref]$tokens, [ref]$parseErrors) | Out-Null
    Require (!$parseErrors.Count) 'README PowerShell example has a syntax error.'
}
foreach ($match in [regex]::Matches($readme, '-Dtest=([A-Za-z_][\w.,#]*)')) {
    foreach ($selector in $match.Groups[1].Value -split ',') {
        $parts = $selector -split '#'
        $matchesSource = @($sources.Keys | Where-Object { $_ -ceq $parts[0] -or ($_ -split '\.')[-1] -ceq $parts[0] })
        Require ($matchesSource.Count -eq 1) "Stale/ambiguous README selector: $selector"
        if ($parts.Count -eq 2) { Require ($sources[$matchesSource[0]] -match ('\b' + [regex]::Escape($parts[1]) + '\s*\(')) "Stale README method: $selector" }
    }
}
$references = 0
foreach ($file in Get-ChildItem $repo -Filter 'testng*.xml') {
    $xml = Read-Xml $file.FullName
    $classes = @($xml.SelectNodes('//class') | ForEach-Object { $_.GetAttribute('name') })
    Require (@($classes | Sort-Object -Unique).Count -eq $classes.Count) "Duplicate class in $($file.Name)"
    foreach ($reference in $xml.SelectNodes('//class/@name | //listener/@class-name')) {
        Require ($sources.ContainsKey($reference.Value)) "Unresolved reference: $($reference.Value)"
        $references++
    }
}
$pom = Read-Xml "$repo/pom.xml"
Require ($pom.project.properties.'surefire.suiteXmlFiles' -eq 'testng-offline.xml') 'Default suite must stay offline.'
Require ($pom.project.properties.'test.excludedGroups' -eq 'manual-auth,sensitive-auth') 'Protected exclusions changed.'
$plugin = @($pom.project.build.plugins.plugin | Where-Object artifactId -eq 'maven-surefire-plugin')[0]
Require ($plugin.configuration.reportsDirectory -eq '${test.reports.directory}') 'Surefire output override must be bound in POM.'
Require ($plugin.configuration.suiteXmlFiles.suiteXmlFile -eq '${surefire.suiteXmlFiles}') 'Surefire must use the explicit suite override.'
Require ($plugin.configuration.excludedGroups -eq '${test.excludedGroups}') 'Surefire protected exclusions must remain bound.'
Require ($plugin.configuration.failIfNoTests -eq 'true') 'Empty test selections must fail.'
$catalog = @(& $workflow Validate)
Require ((& $workflow Run UNIT -DryRun).Category -ceq 'unit') 'Category casing changed output ownership.'
Require (($catalog.Classes | Measure-Object -Sum).Sum -eq 55) 'Expected 55 category classes including two known issues, plus one separate invalid-slug diagnostic.'
$full = Read-Xml "$repo/testng.xml"
$regularClasses = @()
foreach ($category in 'unit', 'api', 'integration', 'ui') {
    $plan = & $workflow Run $category -DryRun
    $regularClasses += $plan.Classes
    $suitePath = ($catalog | Where-Object Category -eq $category).Suite
    $suite = Read-Xml (Join-Path $repo $suitePath)
    Require (@($suite.SelectNodes('//listener')).Count -eq 2) "Category listeners missing: $category"
    if ($category -eq 'api') {
        $original = @($full.SelectNodes('/suite/test[@name="Public Read-only API Tests" or @name="Authenticated Read-only API Tests" or @name="API Mutation Tests"]/classes/class') | ForEach-Object { $_.GetAttribute('name') })
        $knownIssues = @((& $workflow Run api-known-issues -DryRun).Classes)
        $standard = @($original | Where-Object { $knownIssues -cnotcontains $_ })
        Require (($plan.Classes -join ',') -ceq ($standard -join ',')) 'Standard API membership/order differs from full suite minus known issues.'
        Require (@(Compare-Object @($plan.Classes + $knownIssues) $original).Count -eq 0) 'API split lost or added a test.'
    }
    if ($category -eq 'integration') {
        $original = @($full.SelectNodes('/suite/test[@name="API/UI Integration Tests"]/classes/class') | ForEach-Object { $_.GetAttribute('name') })
        Require (($plan.Classes -join ',') -ceq ($original -join ',')) 'Integration membership/order changed.'
    }
    Require (@($plan.Classes | Where-Object { $_ -match 'OfflineRetryFixtures|OfflineSensitiveFixtures' }).Count -eq 0) 'Synthetic fixtures registered.'
    Require (@($plan.Arguments | Where-Object { $_ -like '-Dallure.results.directory=*' }).Count -eq 1) 'Exactly one results destination required.'
    Require (($plan.Arguments -join ' ') -match "target[/\\]allure[/\\]$category[/\\]") 'Results destination is not category-local.'
}
$scenarioClasses = @($sources.Keys | Where-Object { $_ -match 'Test$' -and $sources[$_] -match '@Test\b' })
$diagnosticPlan = & $workflow Run api-known-issues -DryRun
$inventory = @($regularClasses) + @($diagnosticPlan.Classes) + 'az.bakuelectronics.automation.api.InvalidProductSlugDiagnosticTest'
Require ($scenarioClasses.Count -eq 56 -and @(Compare-Object $inventory $scenarioClasses).Count -eq 0) 'Some current scenario classes are missing or misclassified in the category inventory.'
$uiPlan = & $workflow Run ui -DryRun
$uiClasses = @($sources.Keys | Where-Object { $_ -like 'az.bakuelectronics.automation.ui.*' -and $sources[$_] -match '@Test\b' })
Require (@(Compare-Object $uiPlan.Classes $uiClasses).Count -eq 0) 'UI explicit list differs from current browser scenarios.'
$apiPlan = & $workflow Run api -DryRun
Require ($apiPlan.Classes.Count -eq 10 -and $diagnosticPlan.Classes.Count -eq 2) 'Expected exactly 10 standard and 2 diagnostic API classes.'
Require ($apiPlan.Scope -like 'STANDARD: 10*known issues excluded*') 'Standard report must disclose exclusions.'
Require ($diagnosticPlan.Scope -like 'DIAGNOSTIC: 2*') 'Diagnostic report scope missing.'
Require ($diagnosticPlan.Arguments -contains '-Dsurefire.suiteXmlFiles=testng-api-known-issues.xml') 'Diagnostic suite override missing.'
Require (($diagnosticPlan.Arguments -join ' ') -match 'target[/\\]allure[/\\]api-known-issues[/\\]') 'Diagnostic results are not isolated.'
foreach ($class in 'BasketPostZeroQuantityTest', 'BasketUnauthorizedRequestTest') {
    $name = "az.bakuelectronics.automation.api.$class"
    Require ($apiPlan.Classes -cnotcontains $name -and $diagnosticPlan.Classes -ccontains $name) "Known issue misclassified: $class"
    Expect-Rejection { & $workflow Run api -Test $class -DryRun } 'Standard API accepted a known issue.'
    $selected = & $workflow Run api-known-issues -Test $class -DryRun
    Require ($selected.Classes.Count -eq 1 -and $selected.Classes[0] -ceq $name) 'Diagnostic exact selection lost.'
}
foreach ($class in @($apiPlan.Classes) + @($diagnosticPlan.Classes)) {
    $annotations = [regex]::Matches($sources[$class], '@Test\s*\((.*?)\)', 'Singleline')
    Require ($annotations.Count -eq 1 -and $annotations[0].Value -match '"api"') "Expected exactly one api method in $class"
    Require ($sources[$class] -notmatch 'enabled\s*=\s*false|invocationCount|dataProvider\s*=|dependsOn') "Unexpected API invocation selection in $class"
}
foreach ($selection in @(
    @{ Plan = $apiPlan; Counts = @{ api = 10; 'public-api' = 2; 'authenticated-api' = 8; 'read-only' = 3; 'api-mutation' = 6; 'negative-auth' = 0 } },
    @{ Plan = $diagnosticPlan; Counts = @{ api = 2; 'authenticated-api' = 2; 'api-mutation' = 2; 'negative-auth' = 1; 'no-retry' = 1 } }
)) {
    foreach ($group in $selection.Counts.Keys) {
        $members = @($selection.Plan.Classes | Where-Object {
            $annotation = [regex]::Match($sources[$_], '@Test\s*\((.*?)\)', 'Singleline').Value
            $annotation -match ('"' + [regex]::Escape($group) + '"')
        })
        Require ($members.Count -eq $selection.Counts[$group]) "Unexpected group selection: $($selection.Plan.Category) / $group"
    }
}
$groups = @([regex]::Matches(($sources.Values -join "`n"), 'groups\s*=\s*(\{[^}]*\}|"[^"]*")') |
    ForEach-Object { [regex]::Matches($_.Groups[1].Value, '"([^"]+)"') } |
    ForEach-Object { $_.Groups[1].Value } | Sort-Object -Unique)
$expectedGroups = @('api', 'api-mutation', 'authenticated-api', 'integration', 'manual-auth', 'negative-auth', 'no-retry', 'public-api', 'read-only', 'regression', 'sensitive-auth', 'smoke', 'ui-mutation', 'unit')
Require (@(Compare-Object $groups $expectedGroups).Count -eq 0) 'The existing 14 TestNG groups changed.'
Require ($apiPlan.Classes -contains 'az.bakuelectronics.automation.api.BasketInvalidSlugResponseTest') 'Invalid-slug contract hidden.'
foreach ($scenario in 'DedicatedLogin', 'PersonalLogin', 'WrongPassword', 'InvalidSlugDiagnostic') {
    $category = if ($scenario -eq 'InvalidSlugDiagnostic') { 'api' } else { 'ui' }
    $plan = & $workflow Run $category -ProtectedScenario $scenario -DryRun
    Require ($plan.Scope -like 'PARTIAL:*' -and $plan.Classes.Count -eq 1) 'Protected report must be an exact partial selection.'
    Require ($plan.Arguments -contains '-Dsurefire.rerunFailingTestsCount=0') 'Protected reruns enabled.'
    $selector = @($plan.Arguments | Where-Object { $_ -like '-Dtest=*' })[0].Substring(7)
    $method = ($selector -split '#')[1]
    Require ($sources[$plan.Classes[0]] -match ('\b' + [regex]::Escape($method) + '\s*\(')) 'Protected selection references a stale method.'
}
Expect-Rejection { & $workflow Run api } 'Live Run proceeded without -AllowLive.'
Expect-Rejection { & $workflow Run api-known-issues } 'Diagnostic Run proceeded without -AllowLive.'
Expect-Rejection { & $workflow Run unit -Test HomePageLoadTest -DryRun } 'UI test accepted as offline.'
Expect-Rejection { & $workflow Run api -Test ApiClientAuthenticationTest -DryRun } 'Offline API helper accepted as live API.'
Expect-Rejection { & $workflow Run unit -Test '*Test' -DryRun } 'Wildcard selection accepted.'
Expect-Rejection { & $workflow Run unit -Test 'RetryPolicyTest,HomePageLoadTest' -DryRun } 'Cross-category list accepted.'
Expect-Rejection { & $workflow Run ui -Test AccountLoginTest -DryRun } 'Protected login accepted without explicit scenario.'
Expect-Rejection { & $workflow Run unit -ProtectedScenario PersonalLogin -DryRun } 'Personal login classified as offline.'
Expect-Rejection { & $workflow Generate api -RunId '../unit' } 'Cross-category path accepted.'
Expect-Rejection { & $workflow Open ui -RunId '../api' } 'Open allowed category traversal.'
Expect-Rejection { & $workflow Generate unit -Test HomePageLoadTest } 'Generate accepted execution switches.'
$unitPointer = Join-Path $repo 'target/allure/unit/latest-run.json'
if (Test-Path -LiteralPath $unitPointer) {
    $unitId = (Get-Content $unitPointer -Raw | ConvertFrom-Json).runId
    foreach ($category in 'api', 'api-known-issues', 'integration', 'ui') {
        Expect-Rejection { & $workflow Generate $category -RunId $unitId } 'Another category reused the unit run.'
    }
}
Require (!$mavenProbe.Invoked) 'A static safety check attempted to invoke Maven.'
Write-Output "PASS: 10 standard + 2 diagnostic API methods, disjoint/full inventory, group filters (14 unchanged groups), $references XML references, $($snippets.Count) README PowerShell examples/selectors, default safety, scopes and rejection guards (no tests executed)."
