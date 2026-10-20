$ErrorActionPreference = 'Stop'
$Host.UI.RawUI.WindowTitle = 'Szachy / ASCII'
[Console]::InputEncoding = [System.Text.UTF8Encoding]::new($false)
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
$OutputEncoding = [Console]::OutputEncoding

try {
    # JLine measures the actual viewport and configures ANSI/input in the Java process.
    # RawUI dimensions can describe a different buffer, particularly in Windows Terminal.
    & @JAVA@ '-cp' @CLASSPATH@ 'pl.projekt.szachy.console.ConsoleMain' '--ansi' @OPTIONS@
    if ($LASTEXITCODE -ne 0) {
        throw "Java zakonczyla dzialanie z kodem $LASTEXITCODE."
    }
} catch {
    Write-Host $_ -ForegroundColor Red
    [void](Read-Host 'Nie udalo sie uruchomic gry. Nacisnij Enter, aby zamknac')
}
