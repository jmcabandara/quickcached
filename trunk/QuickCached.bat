@echo off
title QuickCached %*

rem set JAVA_HOME=d:\jdk1.6.0_23
rem set JAVA=%JAVA_HOME%\bin\java
rem set cp1=%JAVA_HOME%\lib\tools.jar;.;

rem All EXTRA_OPTS: -XX:+UseParallelGC -XX:+UseStringCache -XX:+AggressiveOpts
rem set EXTRA_OPTS=-XX:+UseParallelGC -XX:+UseStringCache -XX:+AggressiveOpts

rem %JAVA% -cp %cp1% -server -Xms512m -Xmx512m %EXTRA_OPTS% -XX:CompileThreshold=1500 -Xconcurrentio -jar dist\QuickCached-Server.jar %*
java -server -Xms512m -Xmx512m %EXTRA_OPTS% -XX:CompileThreshold=1500 -Xconcurrentio -jar dist\QuickCached-Server.jar %*
