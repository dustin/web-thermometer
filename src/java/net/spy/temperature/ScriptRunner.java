// Copyright (c) 2004  Dustin Sallings <dustin@spy.net>

package net.spy.temperature;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.util.TimerTask;

import net.spy.db.SQLRunner;
import net.spy.db.SpyDB;
import net.spy.log.Logger;
import net.spy.log.LoggerFactory;
import net.spy.util.SpyConfig;

/**
 * Nightly job to clean up the DB and stuff.
 */
public class ScriptRunner extends TimerTask {

	private String path=null;
	private SpyConfig conf=null;

	/**
	 * Get an instance of Nightly.
	 */
	public ScriptRunner(String name, String filePath) {
		super();
		this.path=filePath;
		conf=TempConf.getInstance();
	}

	// Find the named script (classloader relative)
	private InputStream findScript(String name) throws IOException {
		ClassLoader cl=getClass().getClassLoader();
		InputStream rv=cl.getResourceAsStream(name);
		if(rv==null) {
			throw new FileNotFoundException(name);
		}
		return(rv);
	}

	/** 
	 * Run the job.
	 */
	@Override
	public void run() {
		Logger logger=LoggerFactory.getLogger(getClass());
		try {
			logger.info("Running nightly script.");
			SpyDB db=new SpyDB(conf);
			Connection conn=db.getConn();
			SQLRunner sr=new SQLRunner(conn);
			InputStream is=findScript(path);
			sr.runScript(is);
			is.close();
		} catch(Exception e) {
			logger.warn("Problem running job", e);
		}
	}

}
