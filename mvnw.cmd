@echo off
REM Maven Wrapper for Windows

where mvn >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    mvn %*
    goto :end
)

if exist "%USERPROFILE%\apache-maven-3.9.6\bin\mvn.cmd" (
    "%USERPROFILE%\apache-maven-3.9.6\bin\mvn.cmd" %*
    goto :end
)

if exist "C:\Program Files\apache-maven-3.9.6\bin\mvn.cmd" (
    "C:\Program Files\apache-maven-3.9.6\bin\mvn.cmd" %*
    goto :end
)

echo Maven not found. Please install Maven or add it to PATH.
echo Download: https://maven.apache.org/download.cgi
exit /b 1

:end
