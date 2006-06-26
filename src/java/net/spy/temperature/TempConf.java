// Copyright (c) 2000  SPY Internetworking <dustin@spy.net>

package net.spy.temperature;

import java.io.File;

import net.spy.util.SpyConfig;

public class TempConf extends SpyConfig {

	private static TempConf instance=null;

	public static TempConf getInstance() {
		if(instance == null) {
			instance=new TempConf();
		}
		return(instance);
	}

	private File configs[]={
		new File("/afs/spy.net/misc/web/etc/temperature.conf"),
		new File("/data/web/etc/temperature.conf"),
		new File("/usr/local/etc/temperature.conf"),
	};

	private TempConf() {
		super(); // Thanks for asking
		loadConfig(configs);
	}
}