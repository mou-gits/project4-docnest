$folders = @(
    "docnest-client",
    "docnest-server",
    "docnest-shared"
)

foreach ($folder in $folders) {
    $path = Join-Path -Path (Get-Location) -ChildPath $folder
    $outfile = "all-java-$folder.txt"

    # Clear old file if exists
    if (Test-Path $outfile) { Remove-Item $outfile }

    Get-ChildItem -Path $path -Recurse -Include *.java |
        ForEach-Object {
            "===== $($_.FullName) =====" | Out-File $outfile -Append
            Get-Content $_.FullName | Out-File $outfile -Append
            "" | Out-File $outfile -Append
        }
}
