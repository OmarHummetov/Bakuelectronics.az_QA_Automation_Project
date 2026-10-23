
[CmdletBinding()]
param(
    [Parameter(Mandatory, Position = 0)][ValidateSet('Run', 'Generate', 'Open', 'Validate')][string]$Action,
    [Parameter(Position = 1)][ValidateSet('unit', 'api', 'api-known-issues', 'integration', 'ui')][string]$Category = 'unit',
    [string]$RunId,
    [string]$Test,
    [ValidateSet('DedicatedLogin', 'PersonalLogin', 'WrongPassword', 'InvalidSlugDiagnostic')][string]$ProtectedScenario,
    [switch]$AllowLive,
    [switch]$CachedDependencies,
    [switch]$DryRun,
    [string]$AllureCli
)
$ErrorActionPreference = 'Stop'
$Category = $Category.ToLowerInvariant()
$repo = Split-Path $PSScriptRoot -Parent
$categoryRoot = Join-Path $repo "target/allure/$Category"
$idPattern = '^\d{8}-\d{6}-[a-f0-9]{8}$'
$suites = [ordered]@{ unit = 'testng-offline.xml'; api = 'testng-api.xml'; 'api-known-issues' = 'testng-api-known-issues.xml'; integration = 'testng-integration.xml'; ui = 'testng-ui.xml' }
$titles = @{ unit = 'Unit / Offline Tests'; api = 'Standard API Tests'; 'api-known-issues' = 'API Known Issues Diagnostics'; integration = 'Integration Tests'; ui = 'UI Tests' }

function Read-Suite([string]$Path) {
    $settings = New-Object System.Xml.XmlReaderSettings
    $settings.DtdProcessing = [System.Xml.DtdProcessing]::Ignore
    $settings.XmlResolver = $null
    $reader = [System.Xml.XmlReader]::Create($Path, $settings)
    try { $xml = New-Object System.Xml.XmlDocument; $xml.Load($reader); return ,$xml }
    finally { $reader.Dispose() }
}
function New-Id { (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [guid]::NewGuid().ToString('N').Substring(0, 8) }
function Save-Json($Value, [string]$Path) { $Value | ConvertTo-Json -Depth 10 | Set-Content -LiteralPath $Path -Encoding UTF8 }
function Read-Json([string]$Path) { Get-Content -LiteralPath $Path -Raw | ConvertFrom-Json }
function Get-UnixMilliseconds($Value) {

    if ($Value -is [DateTime]) { return ([DateTimeOffset]$Value).ToUnixTimeMilliseconds() }
    return [DateTimeOffset]::Parse([string]$Value, [Globalization.CultureInfo]::InvariantCulture).ToUnixTimeMilliseconds()
}
function Find-Allure {
    if ($AllureCli) { return (Get-Command $AllureCli -ErrorAction Stop).Source }
    $bundled = Join-Path $repo '.allure/allure-2.39.0/bin/allure.bat'
    if (Test-Path -LiteralPath $bundled) { return $bundled }
    return (Get-Command allure -ErrorAction Stop).Source
}


$catalog = @{}
$seen = @{}
foreach ($key in $suites.Keys) {
    $xml = Read-Suite (Join-Path $repo $suites[$key])
    $classes = @($xml.SelectNodes('/suite/test/classes/class') | ForEach-Object { $_.GetAttribute('name') })
    if (!$classes.Count -or $xml.SelectNodes('//packages|//suite-files').Count) { throw "Category suite must contain explicit classes: $key" }
    foreach ($class in $classes) {
        if ($seen.ContainsKey($class)) { throw "Duplicate/cross-category registration: $class" }
        $seen[$class] = $key
    }
    $catalog[$key] = $classes
}
if ($Action -eq 'Validate') {
    foreach ($key in $suites.Keys) { [pscustomobject]@{ Category = $key; Suite = $suites[$key]; Classes = $catalog[$key].Count } }
    return
}
if ($Action -ne 'Run' -and ($Test -or $ProtectedScenario -or $AllowLive -or $CachedDependencies -or $DryRun)) {
    throw 'Selection, live approval and dependency switches apply only to Run.'
}

if ($Action -eq 'Run') {
    if ($RunId) { throw 'Run always creates a fresh ID; RunId is only for Generate/Open.' }
    if ($Category -ne 'unit' -and !$AllowLive -and !$DryRun) { throw 'LIVE tests require an explicit -AllowLive action after account/mutation approval.' }
    if ($Test -and $ProtectedScenario) { throw 'Choose Test or ProtectedScenario, not both.' }
    $classes = @($catalog[$Category])
    $excluded = 'manual-auth,sensitive-auth'
    $approvalArgs = @()
    $scope = switch ($Category) {
        unit { 'FULL offline allowlist (planned)' }
        api { 'STANDARD: 10 API tests; POST-zero and unauthorized known issues excluded; optional invalid-slug diagnostic excluded' }
        'api-known-issues' { 'DIAGNOSTIC: 2 known issues; POST-zero contract mismatch and unauthorized guest recovery blocker; not the standard API suite' }
        integration { 'FULL integration suite (planned)' }
        ui { 'STANDARD: manual-auth/sensitive-auth excluded; not all UI scenarios' }
    }
    if ($ProtectedScenario) {
        $excluded = ''
        switch ($ProtectedScenario) {
            { $_ -in 'DedicatedLogin', 'PersonalLogin' } {
                if ($Category -ne 'ui') { throw 'Login scenarios belong to ui.' }
                $Test = 'AccountLoginTest#configuredIdentifierCanBeEnteredInTheCorrectSsoMode'
                $personal = ($ProtectedScenario -eq 'PersonalLogin').ToString().ToLowerInvariant()
                $dedicated = ($ProtectedScenario -eq 'DedicatedLogin').ToString().ToLowerInvariant()
                $approvalArgs = @('-Dauthenticated.ui.approved=true', "-Dauthenticated.ui.dedicatedAccount=$dedicated", "-Dauthenticated.ui.personalAccountApproved=$personal")
            }
            'WrongPassword' {
                if ($Category -ne 'ui') { throw 'WrongPassword belongs to ui.' }
                $Test = 'LoginInputRejectionTest#incorrectPasswordIsRejectedForDedicatedTestAccount'
                $approvalArgs = @('-Dlogin.negative.password.enabled=true', '-Dlogin.negative.password.diagnostic=false', '-Dlogin.negative.password.personalAccount.enabled=false')
            }
            'InvalidSlugDiagnostic' {
                if ($Category -ne 'api') { throw 'InvalidSlugDiagnostic belongs to api.' }
                $classes += 'az.bakuelectronics.automation.api.InvalidProductSlugDiagnosticTest'
                $Test = 'InvalidProductSlugDiagnosticTest#observeInvalidSlugOnceOnIsolatedAccount'
                $approvalArgs = @('-Dinvalid.slug.diagnostic.approved=true')
            }
        }
        $approvalArgs += @('-Dtest.retry.max=0', '-Dsurefire.rerunFailingTestsCount=0')
    }
    if ($Test) {
        if ($Test -cnotmatch '^(?<class>[A-Za-z_]\w*(?:\.[A-Za-z_]\w*)*)(?:#(?<method>[A-Za-z_]\w*))?$') { throw 'Test must be one exact Class or Class#method; no wildcards, lists or groups.' }
        $selectedClass = $Matches['class']
        $selectedMethod = $Matches['method']
        $classes = @($classes | Where-Object { $_ -ceq $selectedClass -or ($_ -split '\.')[-1] -ceq $selectedClass })
        if ($classes.Count -ne 1) { throw "Test is not in category $Category." }
        if (!$ProtectedScenario -and ($selectedClass -match '(^|\.)AccountLoginTest$' -or $selectedMethod -eq 'incorrectPasswordIsRejectedForDedicatedTestAccount')) {
            throw 'Use the explicit ProtectedScenario option and obtain its separate approval.'
        }
        $scope = "PARTIAL: $Test" + $(if ($ProtectedScenario) { " ($ProtectedScenario)" } else { '; protected groups excluded' })
    }
    $newId = New-Id
    $runRoot = Join-Path $categoryRoot $newId
    $mavenArgs = @('--batch-mode', '--no-transfer-progress', '-Daether.syncContext.named.factory=rwlock-local',
        "-Dsurefire.suiteXmlFiles=$($suites[$Category])", "-Dtest.excludedGroups=$excluded",
        "-Dtest.reports.directory=$runRoot/surefire", "-Dallure.results.directory=$runRoot/results")
    if ($Category -eq 'unit') { $mavenArgs += '-Poffline' }
    if ($CachedDependencies) { $mavenArgs += '--offline' }
    if ($Test) { $mavenArgs += "-Dtest=$Test" }
    $mavenArgs += $approvalArgs
    $mavenArgs += 'test'
    if ($DryRun) { return [pscustomobject]@{ Category = $Category; Scope = $scope; Classes = $classes; Arguments = $mavenArgs } }
    if ($Category -ne 'unit') {
        Write-Warning 'LIVE: may authenticate and modify guest/account baskets. Use approved exclusive accounts; snapshot/cleanup/locks remain active. -AllowLive is not account-owner permission.'
        if ($Category -eq 'api') { Write-Warning 'Standard API excludes POST-zero and unauthorized known issues. Invalid-slug rejection status/schema remains unresolved.' }
        if ($Category -eq 'api-known-issues') { Write-Warning 'DIAGNOSTIC: POST-zero may fail its unchanged-basket assertion; unauthorized request fails closed before HTTP. No approval flag bypasses its guard.' }
    }
    New-Item -ItemType Directory -Path "$runRoot/results" -ErrorAction Stop | Out-Null
    $run = [ordered]@{ version = 1; category = $Category; runId = $newId; suite = $suites[$Category]; scope = $scope;
        selection = $Test; excludedGroups = $excluded; classes = $classes; startedAt = [DateTimeOffset]::Now.ToString('o');
        finishedAt = $null; mavenExitCode = $null; report = $null }
    Save-Json $run "$runRoot/run.json"
    ('mvn ' + (($mavenArgs | ForEach-Object { "'$_'" }) -join ' ')) | Set-Content "$runRoot/maven-command.txt"
    Push-Location $repo
    try {
        & mvn @mavenArgs *> "$runRoot/maven.log"
        $run.mavenExitCode = $LASTEXITCODE
    } finally {
        Pop-Location
        $run.finishedAt = [DateTimeOffset]::Now.ToString('o')
        Save-Json $run "$runRoot/run.json"
    }
    Save-Json @{ category = $Category; runId = $newId } "$categoryRoot/latest-run.json"
    Write-Host "Run: $runRoot ($scope)"
    Get-Content "$runRoot/maven.log" -Tail 18 | Write-Host
    exit $run.mavenExitCode
}


if (!$RunId) {
    $pointer = Read-Json (Join-Path $categoryRoot $(if ($Action -eq 'Open') { 'latest-report.json' } else { 'latest-run.json' }))
    if ($pointer.category -cne $Category) { throw 'Category pointer mismatch.' }
    $RunId = $pointer.runId
}
if ($RunId -cnotmatch $idPattern) { throw 'Invalid run ID.' }
$runRoot = Join-Path $categoryRoot $RunId
$run = Read-Json "$runRoot/run.json"
if ($run.version -ne 1 -or $run.category -cne $Category -or $run.runId -cne $RunId) { throw 'Run/category provenance mismatch.' }
if (!$run.finishedAt -or $null -eq $run.mavenExitCode) { throw 'Run has not completed; inspect its Maven log.' }
$cli = Find-Allure
if ($Action -eq 'Generate') {
    $raw = @(Get-ChildItem "$runRoot/results" -Filter '*-result.json' | ForEach-Object { Read-Json $_.FullName })
    if (!$raw.Count) { throw 'No Allure results; inspect Maven/Surefire output.' }
    $start = Get-UnixMilliseconds $run.startedAt
    $stop = Get-UnixMilliseconds $run.finishedAt
    foreach ($result in $raw) {
        $class = @($result.labels | Where-Object name -eq 'testClass' | ForEach-Object value)
        if ($class.Count -ne 1 -or $run.classes -cnotcontains $class[0]) { throw 'Foreign/nested test results detected; results preserved, generation refused.' }
        if ($result.start -lt $start -or $result.stop -gt $stop) { throw 'Result timestamps do not belong to this run.' }
        if ($run.selection -like '*#*' -and $result.fullName -cne ($class[0] + '.' + ($run.selection -split '#')[1])) { throw 'Result is outside the selected method.' }
    }
    $reportName = 'report-' + (New-Id)
    $reportPath = Join-Path $runRoot $reportName
    $title = "$($titles[$Category]) | $($run.scope) | Maven exit $($run.mavenExitCode) | $RunId"
    if ($run.mavenExitCode -ne 0) { $title = "INCOMPLETE/FAILED RUN | $title"; Write-Warning 'Maven failed; report preserves available failures and may have missing executions.' }
    & $cli generate "$runRoot/results" -o $reportPath --name $title
    if ($LASTEXITCODE -ne 0) { throw 'Allure generation failed; output preserved.' }
    if (!(Test-Path -LiteralPath "$reportPath/index.html")) { throw 'Allure did not create index.html.' }
    $run.report = $reportName
    Save-Json $run "$runRoot/run.json"
    Save-Json @{ category = $Category; runId = $RunId } "$categoryRoot/latest-report.json"
    Write-Host "Report: $reportPath/index.html"
    return
}
if ($run.report -cnotmatch ('^report-' + $idPattern.TrimStart('^'))) { throw 'No valid generated report for this run; use Generate first.' }
$reportPath = Join-Path $runRoot $run.report
if (!(Test-Path -LiteralPath "$reportPath/index.html")) { throw 'Report index is missing.' }

& $cli open $reportPath --host 127.0.0.1 --port 0
if ($LASTEXITCODE -ne 0) { throw 'Allure local server failed.' }
