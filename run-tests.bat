@echo off
setlocal

REM Set JAVA_HOME to IntelliJ's bundled JDK (JetBrains Runtime)
set "JAVA_HOME=C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2023.3.2\jbr"

REM Add Java to PATH
set "PATH=%JAVA_HOME%\bin;%PATH%"

REM Display Java version
echo Checking Java version...
java -version

echo.
echo Running Maven tests...
echo.

REM Run Maven tests
call mvn clean test

echo.
echo Tests complete!
echo.

pause
