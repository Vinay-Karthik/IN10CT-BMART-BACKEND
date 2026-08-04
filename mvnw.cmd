@echo off
setlocal
set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set MAVEN_PROJECT_ROOT=%DIRNAME%
if "%MAVEN_PROJECT_ROOT:~-1%"=="\" set MAVEN_PROJECT_ROOT=%MAVEN_PROJECT_ROOT:~0,-1%
set WRAPPER_JAR="%MAVEN_PROJECT_ROOT%\.mvn\wrapper\maven-wrapper.jar"
set WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain

java "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECT_ROOT%" -classpath %WRAPPER_JAR% %WRAPPER_LAUNCHER% %*
