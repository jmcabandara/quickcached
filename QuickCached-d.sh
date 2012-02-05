#!/bin/bash

exec java -server -Dappname=QC1 -Xms512m -Xmx512m -XX:CompileThreshold=1500 -XX:+UseParallelGC -XX:+UseParallelOldGC -XX:+UseParallelOldGCCompacting -XX:ParallelGCThreads=4 -jar dist/QuickCached-Server.jar $@ &
