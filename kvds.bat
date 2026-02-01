@echo off
setlocal

REM Set JAVA_HOME to IntelliJ's JDK
set "JAVA_HOME=C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2023.3.2\jbr"
set "PATH=%JAVA_HOME%\bin;%PATH%"

REM Compile if needed
if not exist "target\classes\com\kvds\cli\KVDSCli.class" (
    echo Compiling KV-DS...
    call mvn compile -q
    echo.
)

REM Run the CLI
java -cp "target/classes" com.kvds.cli.KVDSCli
