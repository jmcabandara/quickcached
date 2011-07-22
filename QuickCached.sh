#!/bin/bash

exec java -server -Xms512m -Xmx512m -XX:CompileThreshold=1500 -Xconcurrentio -jar dist/QuickCached-Server.jar $@
