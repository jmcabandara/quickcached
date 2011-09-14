@echo on
rem set JAVA_HOME=g:\jdk1.5.0_16
rem set JAVA=%JAVA_HOME%\bin\java
rem set cp1=%JAVA_HOME%\lib\tools.jar;.;
rem %JAVA% -cp %cp1% -server -Xms512m -Xmx512m  -XX:CompileThreshold=1500 -Xconcurrentio -jar dist\QuickCached-Server.jar %*

set JVM_OPTIONS=-XX:+UseParallelGC -Xmaxjitcodesize150m -Xss160k -XX:PermSize=128m -verbose:gc -XX:+PrintGCDateStamps -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintHeapAtGC -XX:+PrintTenuringDistribution -XX:+HeapDumpOnOutOfMemoryError -XX:+PrintClassHistogram -XX:+PrintConcurrentLocks -XX:+PrintCommandLineFlags -XX:+PrintTenuringDistribution -Xloggc:verbose_gc.log

java -server %JVM_OPTIONS% -Xms512m -Xmx512m  -XX:CompileThreshold=1500 -Xconcurrentio -jar dist\QuickCached-Server.jar %*
