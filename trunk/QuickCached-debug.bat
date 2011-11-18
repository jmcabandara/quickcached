@echo off
rem set JAVA_HOME=g:\jdk1.5.0_16
rem set JAVA=%JAVA_HOME%\bin\java
rem set cp1=%JAVA_HOME%\lib\tools.jar;.;

set GC_OPTIONS=-verbose:gc -Xloggc:verbose_gc.log -XX:+PrintGCDetails -XX:+PrintHeapAtGC -XX:+HeapDumpOnOutOfMemoryError -XX:+PrintClassHistogram   -XX:+PrintCommandLineFlags -XX:+PrintConcurrentLocks -XX:+PrintGCTimeStamps 
rem -XX:+PrintGCDateStamps -XX:+PrintTenuringDistribution  

set JVM_OPTIONS=-Xmaxjitcodesize150m -Xss160k -XX:PermSize=128m -Xconcurrentio -XX:+UseParallelGC -Xconcurrentio -XX:CompileThreshold=1500


rem %JAVA% -server %JVM_OPTIONS% %GC_OPTIONS% -Xms512m -Xmx512m  -jar dist\QuickCached-Server.jar %*

java -server %JVM_OPTIONS% %GC_OPTIONS% -Xms512m -Xmx512m  -jar dist\QuickCached-Server.jar %*
