<#
    Publish docs/en_us/*.md to a GitHub Wiki checkout.

    Why this step exists: a GitHub Wiki has NO folder hierarchy -- every page renders at
    /wiki/<page-name>, without any extension. Internal wiki links must therefore use flat
    page names, e.g. [Loot Conditions](Loot-Conditions), while the very same files inside the
    repository need relative paths, e.g. [Loot Conditions](Loot-Conditions.md), to stay
    clickable on GitHub's file view. Both forms cannot be true at once, so links are rewritten
    on publish (this matches the advice in the github-wiki-flat skill).

    Usage:
        git clone https://github.com/<owner>/<repo>.wiki.git wiki
        powershell -ExecutionPolicy Bypass -File 1.21.1/docs/publish-wiki.ps1 -Target wiki -WhatIf
        powershell -ExecutionPolicy Bypass -File 1.21.1/docs/publish-wiki.ps1 -Target wiki
        cd wiki && git add -A && git commit -m "docs: sync" && git push

    Keep this file ASCII-only: Windows PowerShell 5.1 reads a BOM-less file as ANSI, so any
    non-ASCII text here would fail to parse unless the file carried a UTF-8 BOM.
#>
[CmdletBinding(SupportsShouldProcess)]
param(
    # The wiki checkout (from `git clone <repo>.wiki.git`)
    [Parameter(Mandatory)][string]$Target,

    # Source directory; defaults to the directory holding this script (en_us)
    [string]$Source
)

$ErrorActionPreference = 'Stop'
$enc = New-Object System.Text.UTF8Encoding($false)

if (-not $Source) { $Source = Join-Path $PSScriptRoot 'en_us' }
if (-not (Test-Path -LiteralPath $Source -PathType Container)) { throw "Source directory not found: $Source" }
if (-not (Test-Path -LiteralPath $Target -PathType Container)) { throw "Target directory not found: $Target (clone <repo>.wiki.git first)" }

$targetFull = (Resolve-Path -LiteralPath $Target).Path

# Guard: refuse to write anywhere that does not look like a wiki checkout.
$gitDir = Join-Path $targetFull '.git'
$remote = ''
if (Test-Path -LiteralPath $gitDir) {
    $remote = (git -C $targetFull remote -v 2>$null | Select-Object -First 1)
    if ($remote -notmatch '\.wiki(\.git)?\b') {
        throw "Target does not look like a wiki checkout (no .wiki in its remote): $remote"
    }
}
elseif ((Get-ChildItem -LiteralPath $targetFull -Force -ErrorAction SilentlyContinue | Measure-Object).Count -gt 0) {
    throw "Target is neither a git checkout nor an empty directory: $targetFull"
}

$files = @(Get-ChildItem -LiteralPath $Source -File -Filter *.md | Sort-Object Name)
if ($files.Count -eq 0) { throw "No .md files in source directory: $Source" }

# Relative link -> flat page name:  ](Name.md) => ](Name),  ](Name.md#anchor) => ](Name#anchor)
$linkPattern = '\]\(([A-Za-z0-9._-]+)\.md(#[^)]*)?\)'
$notWikiPage = @{}

foreach ($f in $files) {
    $text = [System.IO.File]::ReadAllText($f.FullName)
    $converted = [regex]::Replace($text, $linkPattern, ']($1$2)')
    $count = ([regex]::Matches($text, $linkPattern)).Count

    # A wiki only renders .md pages; links to anything else (json, images) will 404 up there.
    foreach ($m in [regex]::Matches($converted, '\]\(([^)#]+\.(?!md\b)[A-Za-z0-9]+)\)')) {
        $notWikiPage[$m.Groups[1].Value] = $true
    }

    $dest = Join-Path $targetFull $f.Name
    if ($PSCmdlet.ShouldProcess($dest, "write (rewrite $count link(s))")) {
        [System.IO.File]::WriteAllText($dest, $converted, $enc)
    }
    Write-Host ("  {0,-28} -> {1}  ({2} link(s) rewritten)" -f $f.Name, $targetFull, $count)
}

Write-Host ""
Write-Host "Done: $($files.Count) page(s)."
if ($notWikiPage.Count -gt 0) {
    Write-Host ""
    Write-Host "[warn] these link targets are not .md and will 404 on the wiki:"
    $notWikiPage.Keys | Sort-Object | ForEach-Object { Write-Host "  $_" }
}
