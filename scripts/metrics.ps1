
param(
    [switch]$IncludeSandbox,
    [int]$Top = 10
)

$ErrorActionPreference = "Stop"

# Resolve the repository root relative to this script.
$repoRoot = Split-Path -Parent $PSScriptRoot

$roots = @(
    Join-Path $repoRoot "engine"
)

if ($IncludeSandbox) {
    $roots += Join-Path $repoRoot "sandbox"
}

# Count physical code, comment-only and blank lines.
function Measure-SourceFile {
    param([string]$Path)

    $loc = 0
    $sloc = 0
    $comments = 0
    $blank = 0

    $blockDepth = 0
    $inTripleString = $false

    foreach ($line in [System.IO.File]::ReadAllLines($Path)) {
        $loc++

        if ([string]::IsNullOrWhiteSpace($line)) {
            $blank++
            continue
        }

        $hasCode = $false
        $hasComment = $false
        $quote = ""
        $escaped = $false
        $i = 0

        while ($i -lt $line.Length) {
            $ch = $line.Substring($i, 1)

            $pair = ""
            $triple = ""

            if ($i + 1 -lt $line.Length) {
                $pair = $line.Substring($i, 2)
            }

            if ($i + 2 -lt $line.Length) {
                $triple = $line.Substring($i, 3)
            }

            # Handle nested block comments.
            if ($blockDepth -gt 0) {
                $hasComment = $true

                if ($pair -eq "/*") {
                    $blockDepth++
                    $i += 2
                }
                elseif ($pair -eq "*/") {
                    $blockDepth--
                    $i += 2
                }
                else {
                    $i++
                }

                continue
            }

            # Handle Kotlin multiline strings.
            if ($inTripleString) {
                $hasCode = $true

                if ($triple -eq '"""') {
                    $inTripleString = $false
                    $i += 3
                }
                else {
                    $i++
                }

                continue
            }

            # Handle normal strings and character literals.
            if ($quote -ne "") {
                $hasCode = $true

                if ($escaped) {
                    $escaped = $false
                }
                elseif ($ch -eq '\') {
                    $escaped = $true
                }
                elseif ($ch -eq $quote) {
                    $quote = ""
                }

                $i++
                continue
            }

            if ($triple -eq '"""') {
                $inTripleString = $true
                $hasCode = $true
                $i += 3
                continue
            }

            if ($ch -eq '"' -or $ch -eq "'") {
                $quote = $ch
                $hasCode = $true
                $i++
                continue
            }

            if ($pair -eq "//") {
                $hasComment = $true
                break
            }

            if ($pair -eq "/*") {
                $blockDepth++
                $hasComment = $true
                $i += 2
                continue
            }

            if (-not [char]::IsWhiteSpace($line[$i])) {
                $hasCode = $true
            }

            $i++
        }

        if ($hasCode) {
            $sloc++
        }
        elseif ($hasComment) {
            $comments++
        }
        else {
            $blank++
        }
    }

    return [PSCustomObject]@{
        LOC      = $loc
        SLOC     = $sloc
        Comments = $comments
        Blank    = $blank
    }
}

# Sum one metric across multiple files.
function Sum-Metric {
    param(
        [object[]]$Items,
        [string]$Property
    )

    return [int](($Items | Measure-Object -Property $Property -Sum).Sum)
}

Write-Host ""
Write-Host "STRATA CODE METRICS" -ForegroundColor Cyan
Write-Host "===================" -ForegroundColor Cyan

# Discover source files.
$files = @(
    foreach ($root in $roots) {
        if (-not (Test-Path -LiteralPath $root)) {
            throw "Source directory not found: $root"
        }

        Get-ChildItem -LiteralPath $root -Recurse -File |
            Where-Object {
                $_.Extension.ToLowerInvariant() -in @(".kt", ".kts", ".java") -and
                $_.FullName -match '[\\/]src[\\/]' -and
                $_.FullName -notmatch '[\\/](build|generated|out|\.gradle)[\\/]'
            }
    }
)

# Collect individual file metrics.
$results = @(
    foreach ($file in $files) {
        $relative = $file.FullName.Substring($repoRoot.Length).TrimStart('\', '/')
        $parts = $relative -split '[\\/]'

        $module = if ($parts[0] -eq "engine") {
            "engine/$($parts[1])"
        }
        else {
            "sandbox"
        }

        $scope = if ($relative -match '[\\/]src[\\/](test|integrationTest)[\\/]') {
            "Tests"
        }
        else {
            "Production"
        }

        $metrics = Measure-SourceFile -Path $file.FullName

        [PSCustomObject]@{
            Module   = $module
            Scope    = $scope
            File     = $relative
            LOC      = $metrics.LOC
            SLOC     = $metrics.SLOC
            Comments = $metrics.Comments
            Blank    = $metrics.Blank
        }
    }
)

if ($results.Count -eq 0) {
    Write-Host "No source files found." -ForegroundColor Yellow
    exit 0
}

# Build module and scope summaries.
$sections = @(
    $results |
        Group-Object Module, Scope |
        ForEach-Object {
            $items = @($_.Group)

            [PSCustomObject]@{
                Module   = $items[0].Module
                Scope    = $items[0].Scope
                Files    = $items.Count
                LOC      = Sum-Metric $items "LOC"
                SLOC     = Sum-Metric $items "SLOC"
                Comments = Sum-Metric $items "Comments"
                Blank    = Sum-Metric $items "Blank"
            }
        }
)

Write-Host ""
Write-Host "BY MODULE" -ForegroundColor Cyan

$sections |
    Sort-Object Module, Scope |
    Format-Table Module, Scope, Files, LOC, SLOC, Comments, Blank -AutoSize |
    Out-Host


# Print separate production/test totals.
Write-Host "TOTALS" -ForegroundColor Cyan

$totals = @(
    foreach ($scope in @("Production", "Tests", "All")) {
        $items = if ($scope -eq "All") {
            @($results)
        }
        else {
            @($results | Where-Object { $_.Scope -eq $scope })
        }

        [PSCustomObject]@{
            Scope    = $scope
            Files    = $items.Count
            LOC      = Sum-Metric $items "LOC"
            SLOC     = Sum-Metric $items "SLOC"
            Comments = Sum-Metric $items "Comments"
            Blank    = Sum-Metric $items "Blank"
        }
    }
)

$totals |
    Format-Table Scope, Files, LOC, SLOC, Comments, Blank -AutoSize |
    Out-Host

Write-Host "LARGEST FILES (BY SLOC)" -ForegroundColor Cyan

$results |
    Sort-Object SLOC -Descending |
    Select-Object -First $Top File, SLOC, LOC |
    Format-Table -AutoSize |
    Out-Host
