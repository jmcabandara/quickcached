package org.quickcached;

import java.util.Arrays;
import java.util.List;
import org.quickserver.net.server.*;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

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
			} catch(Exception e) {
				logger.log(Level.WARNING, "Error: "+e, e);
				e.printStackTrace();
			}
			return true;
		}
		return false;
	}
}
