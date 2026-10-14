[CmdletBinding()]
param()

$requiredVariables = @(
    "BAKU_ELECTRONICS_LOGIN",
    "BAKU_ELECTRONICS_PASSWORD"
)

$missingVariables = @(
    $requiredVariables | Where-Object {
        [string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($_, "Process"))
    }
)

if ($missingVariables.Count -gt 0) {
    Write-Error ("Missing required process environment variable(s): " + ($missingVariables -join ", ") + ". " +
        "Set them in this PowerShell process before running authenticated tests. Values are never printed.")
    exit 1
}

Write-Output "Credential variables are present in the current PowerShell process. Values were not displayed."
exit 0
