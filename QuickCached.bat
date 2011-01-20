@echo off
set JAVA_HOME=d:\jdk1.5.0_16
set JAVA=%JAVA_HOME%\bin\java

set cp1=%JAVA_HOME%\lib\tools.jar;.;

set cp1=%cp1%;.\lib\whirlycache-1.0.1.jar;.\lib\concurrent.jar;;.\lib\QuickServer.jar;.\lib\log4j.jar

@java -server -cp %cp1% -Xms50m -Xmx512m -Dnet.spy.log.LoggerImpl=net.spy.memcached.compat.log.SunLogger -XX:CompileThreshold=1500 -Xconcurrentio org.quickserver.net.server.QuickServer -load conf\QuickCached.xml
