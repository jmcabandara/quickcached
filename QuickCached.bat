@echo off
title QuickCached %*

rem set JAVA_HOME=d:\jdk1.6.0_23
rem set JAVA=%JAVA_HOME%\bin\java
rem set cp1=-cp %JAVA_HOME%\lib\tools.jar;.;

rem set EXTRA_OPTS=-XX:+UseParallelGC -XX:+UseStringCache -XX:+AggressiveOpts
set JVM_OPTIONS=-Xmaxjitcodesize150m -Xss160k -XX:PermSize=128m -Xconcurrentio -XX:+UseParallelGC -XX:CompileThreshold=1500

rem %JAVA% %cp1% -server -Xms512m -Xmx512m %JVM_OPTIONS% %EXTRA_OPTS% -jar dist\QuickCached-Server.jar %*
java -server -Xms512m -Xmx512m %JVM_OPTIONS% %EXTRA_OPTS% -jar dist\QuickCached-Server.jar %*
