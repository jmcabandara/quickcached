@echo off
set JAVA_HOME=g:\jdk1.5.0_16
set ANT_HOME=G:\apache-ant-1.8.2

set PATH=%JAVA_HOME%\bin;%PATH%
set CLASSPATH=%JAVA_HOME%\lib\tools.jar;.;

%ANT_HOME%\bin\ant %1 %2 %3 %4 %5 %6 -buildfile build.xml
