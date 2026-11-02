[CmdletBinding()]
param(
    [string]$JavaHome = 'C:/Program Files/Java/jdk-23',
    [string]$AllureCli = '',
    [switch]$CachedDependencies
)
$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
$expected = @('SearchSubmissionConsistencyTest', 'SearchBrowserHistoryTest',
    'MultipleFiltersCombinationTest', 'IndividualFilterRemovalTest', 'ProductPriceDescendingSortTest',
    'FilterSortPaginationPersistenceTest', 'ProductImageGalleryTest', 'ProductBreadcrumbNavigationTest') |
    ForEach-Object { "az.bakuelectronics.automation.ui.$_" }
$settings = New-Object System.Xml.XmlReaderSettings
$settings.DtdProcessing = [System.Xml.DtdProcessing]::Ignore
$settings.XmlResolver = $null
$reader = [System.Xml.XmlReader]::Create((Join-Path $repo 'testng-ui-extension.xml'), $settings)
try { $suite = New-Object System.Xml.XmlDocument; $suite.Load($reader) } finally { $reader.Dispose() }
$selected = @($suite.SelectNodes('//class') | ForEach-Object { $_.GetAttribute('name') })
if ($selected.Count -ne 8 -or @(Compare-Object $selected $expected).Count -ne 0 -or $suite.SelectNodes('//package').Count) {
    throw 'Extension suite must select only the eight approved guest classes explicitly.'
}
if (!(Test-Path -LiteralPath (Join-Path $JavaHome 'bin/java.exe'))) { throw 'Java 23 runtime missing.' }
if (!$AllureCli) { $AllureCli = Join-Path $repo '.allure/allure-2.39.0/bin/allure.bat' }
if (!(Test-Path -LiteralPath $AllureCli)) { throw 'Supply the installed Allure CLI path with -AllureCli.' }
$env:JAVA_HOME = $JavaHome
$env:Path = "$(Join-Path $JavaHome 'bin');$env:Path"
$runId = (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [guid]::NewGuid().ToString('N').Substring(0, 8)
$runRoot = Join-Path $repo "target/allure/ui-extension/$runId"
New-Item -ItemType Directory -Path "$runRoot/results" | Out-Null
$arguments = @('--batch-mode', '--no-transfer-progress', '-Daether.syncContext.named.factory=rwlock-local',
    '-Dsurefire.suiteXmlFiles=testng-ui-extension.xml', '-Dtest.retry.max=0', '-Dsurefire.rerunFailingTestsCount=0',
    "-Dallure.results.directory=$runRoot/results", "-Dtest.reports.directory=$runRoot/surefire", 'test')
if ($CachedDependencies) { $arguments = @('--offline') + $arguments }
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
    $complete = $results.Count -eq 8 -and @($reportedClasses | Sort-Object -Unique).Count -eq 8
    $title = if ($mavenExit -eq 0 -and $complete) { 'GUEST UI EXTENSION PHASE 1 - 8 READ-ONLY TESTS' }
        else { 'GUEST UI EXTENSION PHASE 1 - FAILED OR INCOMPLETE RUN' }
    & $AllureCli generate "$runRoot/results" -o "$runRoot/report" --name $title
    $allureExit = $LASTEXITCODE
    [ordered]@{ runId = $runId; suite = 'testng-ui-extension.xml'; classes = $expected;
        started = $started; completed = (Get-Date).ToString('o'); mavenExitCode = $mavenExit;
        allureExitCode = $allureExit; resultCount = $results.Count;
        statuses = @($results | Group-Object status | Select-Object Name, Count);
        report = "$runRoot/report/index.html" } | ConvertTo-Json -Depth 5 | Set-Content "$runRoot/run.json"
    Write-Output "Extension run: $runRoot"
    if ($mavenExit -ne 0) { exit $mavenExit }
    if (!$complete) { throw 'Expected exactly one actual result from each of the eight extension classes.' }
    exit $allureExit
} finally { Pop-Location }
