@echo off
set JAVA_HOME=g:\jdk1.5.0_16
set JAVA=%JAVA_HOME%\bin\java

set cp1=%JAVA_HOME%\lib\tools.jar;.;

set cp1=%cp1%;

@java -server -cp %cp1% -Xms50m -Xmx512m  -XX:CompileThreshold=1500 -Xconcurrentio -jar dist\QuickCached.jar %*
