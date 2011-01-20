@echo off
set JAVA_HOME=g:\jdk1.5.0_16
set JAVA=%JAVA_HOME%\bin\java

set cp=%JAVA_HOME%\lib\tools.jar;.\lib\ant.jar;
%JAVA% -classpath "%CP%" -Dant.home=lib org.apache.tools.ant.Main %1 %2 %3 %4 %5 %6 -buildfile build.xml
