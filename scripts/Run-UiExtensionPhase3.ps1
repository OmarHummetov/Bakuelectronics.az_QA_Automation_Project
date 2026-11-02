[CmdletBinding()]
param(
    [string]$JavaHome = 'C:/Program Files/Java/jdk-23',
    [string]$AllureCli = '',
    [switch]$CachedDependencies,
    [ValidateSet('InstallmentOptionsConsistencyTest', 'RelatedProductsNavigationTest',
        'MobileResponsiveNavigationTest', 'KeyboardAccessibilityNavigationTest')][string]$Test
)
$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
$expected = @('InstallmentOptionsConsistencyTest', 'RelatedProductsNavigationTest',
    'MobileResponsiveNavigationTest', 'KeyboardAccessibilityNavigationTest') |
    ForEach-Object { "az.bakuelectronics.automation.ui.$_" }
$settings = New-Object System.Xml.XmlReaderSettings
$settings.DtdProcessing = [System.Xml.DtdProcessing]::Ignore
$settings.XmlResolver = $null
$reader = [System.Xml.XmlReader]::Create((Join-Path $repo 'suites/ui-extensions/navigation-accessibility.xml'), $settings)
try { $suite = New-Object System.Xml.XmlDocument; $suite.Load($reader) } finally { $reader.Dispose() }
$selected = @($suite.SelectNodes('//class') | ForEach-Object { $_.GetAttribute('name') })
if ($selected.Count -ne 4 -or @(Compare-Object $selected $expected).Count -ne 0 -or $suite.SelectNodes('//package').Count) {
    throw 'Extension suite must select only the four approved guest classes explicitly.'
}
if ($Test) { $expected = @("az.bakuelectronics.automation.ui.$Test") }
if (!(Test-Path -LiteralPath (Join-Path $JavaHome 'bin/java.exe'))) { throw 'Java 23 runtime missing.' }
if (!$AllureCli) { $AllureCli = Join-Path $repo '.allure/allure-2.39.0/bin/allure.bat' }
if (!(Test-Path -LiteralPath $AllureCli)) { throw 'Supply the installed Allure CLI path with -AllureCli.' }
$env:JAVA_HOME = $JavaHome
$env:Path = "$(Join-Path $JavaHome 'bin');$env:Path"
$runId = (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [guid]::NewGuid().ToString('N').Substring(0, 8)
$runRoot = Join-Path $repo "target/allure/ui-extension-phase3/$runId"
New-Item -ItemType Directory -Path "$runRoot/results" | Out-Null
$arguments = @('--batch-mode', '--no-transfer-progress', '-Daether.syncContext.named.factory=rwlock-local',
    '-Dsurefire.suiteXmlFiles=suites/ui-extensions/navigation-accessibility.xml', '-Dtest.retry.max=0', '-Dsurefire.rerunFailingTestsCount=0',
    '-Dui.campaignPopup.suppress=false',
    "-Dallure.results.directory=$runRoot/results", "-Dtest.reports.directory=$runRoot/surefire", 'test')
if ($CachedDependencies) { $arguments = @('--offline') + $arguments }
if ($Test) { $arguments = @("-Dtest=$Test") + $arguments }
$started = (Get-Date).ToString('o')
Push-Location $repo
try {
    $arguments | Set-Content "$runRoot/maven-arguments.txt"
    & mvn @arguments 2>&1 | Tee-Object -FilePath "$runRoot/maven.log"
    $mavenExit = $LASTEXITCODE
    $results = @(Get-ChildItem "$runRoot/results" -Filter '*-result.json' | ForEach-Object {
        Get-Content -LiteralPath $_.FullName -Raw | ConvertFrom-Json
    })
    $foreign = @($results | Where-Object { ($_.fullName -replace '\.[^.]+$', '') -notin $expected })
    if ($foreign.Count) { throw 'Unexpected result identities: preserve this run and inspect it before rendering.' }
    $reportedClasses = @($results | ForEach-Object { $_.fullName -replace '\.[^.]+$', '' })
    $complete = $results.Count -eq $expected.Count -and @($reportedClasses | Sort-Object -Unique).Count -eq $expected.Count
    $title = if ($mavenExit -eq 0 -and $complete) { 'GUEST UI EXTENSION PHASE 3 - 4 READ-ONLY TESTS' }
        else { 'GUEST UI EXTENSION PHASE 3 - FAILED OR INCOMPLETE RUN' }
    if ($Test) { $title = "PARTIAL - $Test - Maven exit $mavenExit - GUEST UI PHASE 3" }
    & $AllureCli generate "$runRoot/results" -o "$runRoot/report" --name $title
    $allureExit = $LASTEXITCODE
    [ordered]@{ runId = $runId; suite = 'suites/ui-extensions/navigation-accessibility.xml'; classes = $expected; selection = $Test;
        started = $started; completed = (Get-Date).ToString('o'); mavenExitCode = $mavenExit;
        allureExitCode = $allureExit; resultCount = $results.Count;
        statuses = @($results | Group-Object status | Select-Object Name, Count);
        report = "$runRoot/report/index.html" } | ConvertTo-Json -Depth 5 | Set-Content "$runRoot/run.json"
    Write-Output "Extension run: $runRoot"
    if ($mavenExit -ne 0) { exit $mavenExit }
    if (!$complete) { throw 'Expected exactly one actual result from each selected extension class.' }
    exit $allureExit
} finally { Pop-Location }
