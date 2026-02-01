@echo off
setlocal

REM Set JAVA_HOME to IntelliJ's JDK
set "JAVA_HOME=C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2023.3.2\jbr"
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo ========================================
echo Running KV-DS Quick Demo
echo ========================================
echo.

REM Compile the demo
echo Compiling demo...
call mvn test-compile -q

REM Run the demo
echo.
echo Running demo...
echo.

java -cp "target/classes;target/test-classes" com.kvds.demo.QuickDemo

echo.
echo ========================================
echo Demo complete!
echo ========================================
pause
