package org.quickcached;

import java.util.Arrays;
import java.util.List;
import org.quickserver.net.server.*;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.quickcached.mem.MemoryWarningSystem;

import org.quickserver.net.ServerHook;

public class PrepareHook implements ServerHook {
	private static final Logger logger = Logger.getLogger(PrepareHook.class.getName());
	
	private QuickServer quickserver;

	public String info() {
		return "Init Server Hook to setup cache.";
	}

	public void initHook(QuickServer quickserver) {
		this.quickserver = quickserver;
	}

	public boolean handleEvent(int event) {
		if(event==ServerHook.PRE_STARTUP) {
			Map config = quickserver.getConfig().getApplicationConfiguration();
			try {
				CommandHandler.init(config);
			} catch(Exception e) {
				logger.log(Level.WARNING, "Error: "+e, e);
			}

			try {
				String enableStatsReportStr = (String) config.get("ENABLE_STATS_REPORT");
				boolean enableStatsReport = false;

				if("true".equals(enableStatsReportStr)) {
					enableStatsReport = true;
				}

				String statsReportWriteIntervalStr = (String) config.get("STATS_REPORT_WRITE_INTERVAL");
				if(statsReportWriteIntervalStr!=null) {
					StatsReportGenerator.setWriteInterval(Integer.parseInt(statsReportWriteIntervalStr));
				}

				String entriesToLog  = (String) config.get("ENTRIES_TO_LOG");
				if(entriesToLog!=null) {
					List entriesToLogList = Arrays.asList(entriesToLog.split(","));
					entriesToLogList.remove(" ");
					StatsReportGenerator.setEntriesToLog(entriesToLogList);
				}

				if(enableStatsReport) {
					StatsReportGenerator.start(quickserver);
				}
				
				String gcCallOnLowMemoryPercentStr = (String) config.get("GC_CALL_ON_LOW_MEMORY_PERCENT");
				if(gcCallOnLowMemoryPercentStr!=null && gcCallOnLowMemoryPercentStr.trim().length()!=0) {
					try {
						final int gcCallOnLowMemoryPercent = (int) (
								Double.parseDouble(gcCallOnLowMemoryPercentStr) * 100);
						String poolingIntrvalMin = (String) config.get(
								"GC_CALL_ON_LOW_MEMORY_POLLING_INTERVAL_MIN");
						if(poolingIntrvalMin==null) poolingIntrvalMin = "1"; 
						final int gcCallOnLowMemoryPollingIntervalMin = Integer.parseInt(poolingIntrvalMin);
						
						Thread t = new Thread() {
							public void run() {
								logger.info("Started..");
								int gcCallOnLowMemoryPoolInterval = 
										gcCallOnLowMemoryPollingIntervalMin * 1000 * 60;
								while(true) {
									try {
										sleep(gcCallOnLowMemoryPoolInterval);
									} catch (InterruptedException ex) {
										Logger.getLogger(StatsReportGenerator.class.getName()).log(
												Level.WARNING, "Error", ex);
										break;
									}
									int permemuse = MemoryWarningSystem.getMemUsedPercentage();
									if(permemuse > gcCallOnLowMemoryPercent) {
										logger.fine("MemUsedPercentage: "+permemuse+", calling gc..");
										System.gc();
										CommandHandler.gcCalls++;
									}
								}
								logger.info("Done");
							}
						};
						t.setName("GCCallOnLowMemoryPool-Thread");
						t.setDaemon(true);
						t.start();
					} catch(Exception er) {
						logger.log(Level.WARNING, "Error: "+er, er);
						er.printStackTrace();
					}
				}
			} catch(Exception e) {
				logger.log(Level.WARNING, "Error: "+e, e);
				e.printStackTrace();
			}
			return true;
		}
		return false;
	}
}
