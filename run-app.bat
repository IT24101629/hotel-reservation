@echo off
set JAVA_HOME=C:\Users\LapMart\.jdks\openjdk-24.0.2+12-54
set PATH=%JAVA_HOME%\bin;%PATH%
call mvnw.cmd clean spring-boot:run
