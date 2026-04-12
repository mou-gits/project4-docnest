Get-ChildItem -Recurse -Include *.java |
    ForEach-Object {
        "===== $($_.FullName) =====" | Out-File "all-java.txt" -Append
        Get-Content $_.FullName | Out-File "all-java.txt" -Append
        "" | Out-File "all-java.txt" -Append
    }
