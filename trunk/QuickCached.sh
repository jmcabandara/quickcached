#!/bin/bash

java -server -Xms50m -Xmx512m -XX:CompileThreshold=1500 -Xconcurrentio -jar dist/QuickCached.jar $@
