@echo off
rem set JAVA_HOME=g:\jdk1.5.0_16
rem set JAVA=%JAVA_HOME%\bin\java
rem set cp1=%JAVA_HOME%\lib\tools.jar;.;
rem %JAVA% -cp %cp1% -server -Xms512m -Xmx512m  -XX:CompileThreshold=1500 -Xconcurrentio -jar dist\QuickCached.jar %*

java -server -Xms512m -Xmx512m  -XX:CompileThreshold=1500 -Xconcurrentio -jar dist\QuickCached.jar %*
