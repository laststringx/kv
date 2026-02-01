@echo off
setlocal

REM Set JAVA_HOME to IntelliJ's JDK
set "JAVA_HOME=C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2023.3.2\jbr"
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo ========================================
echo Starting KV-DS Interactive CLI
echo ========================================
echo.

REM Compile if needed
if not exist "target\classes\com\kvds\cli\KVDSCli.class" (
    echo Compiling KV-DS...
    call mvn compile -q
    echo.
)

REM Run the CLI using Maven exec plugin (includes all dependencies)
call mvn exec:java -Dexec.mainClass="com.kvds.cli.KVDSCli" -Dexec.cleanupDaemonThreads=false -q
