@echo off
echo Setting JAVA_HOME...
set "JAVA_HOME=C:\Users\LapMart\.jdks\openjdk-24.0.2+12-54"
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo Verifying Java version...
java -version

echo Starting Maven build...
call mvnw.cmd clean compile spring-boot:run

pause
