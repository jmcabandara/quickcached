package org.quickcached;

import org.quickserver.net.server.*;

import java.util.Map;

import org.quickserver.net.ServerHook;

public class PrepareHook implements ServerHook {
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
			CommandHandler.init(config);
			return true;
		}
		return false;
	}
}
