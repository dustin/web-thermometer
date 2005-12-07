// Copyright (c) 2000  SPY Internetworking <dustin@spy.net>

package net.spy.temperature;

import java.io.File;

import net.spy.util.SpyConfig;

public class TempConf extends SpyConfig {

	private File configs[]={
		new File("/afs/spy.net/misc/web/etc/temperature.conf"),
		new File("/data/web/etc/temperature.conf"),
		new File("/usr/local/etc/temperature.conf"),
		new File("temperature.conf"),
		new File("/tmp/temperature.conf")
	};

	public TempConf() {
		super(); // Thanks for asking
		loadConfig(configs);
	}
}
