#!/bin/bash

cp1=./lib/whirlycache-1.0.1.jar:./lib/concurrent.jar:./lib/QuickServer.jar:./lib/log4j.jar
java -server -cp $cp1 -Xms50m -Xmx512m -XX:CompileThreshold=1500 -Dnet.spy.log.LoggerImpl=net.spy.memcached.compat.log.SunLogger -Xconcurrentio org.quickserver.net.server.QuickServer -load conf/QuickCached.xml
