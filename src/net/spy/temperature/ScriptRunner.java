// Copyright (c) 2004  Dustin Sallings <dustin@spy.net>

package net.spy.temperature;

import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.InputStream;

import java.util.Date;

import java.sql.SQLException;
import java.sql.Connection;

import net.spy.util.SpyConfig;
import net.spy.cron.Job;
import net.spy.cron.TimeIncrement;
import net.spy.db.SpyDB;
import net.spy.db.SQLRunner;

/**
 * Nightly job to clean up the DB and stuff.
 */
public class ScriptRunner extends Job {

	private String path=null;
	private SpyConfig conf=null;

	/**
	 * Get an instance of Nightly.
	 */
	public ScriptRunner(String name, Date d, TimeIncrement ti, String path) {
		super(name, d, ti);
		this.path=path;
		conf=new TempConf();
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
	public void runJob() {
		try {
			getLogger().info("Running nightly script.");
			SpyDB db=new SpyDB(conf);
			Connection conn=db.getConn();
			SQLRunner sr=new SQLRunner(conn);
			InputStream is=findScript(path);
			sr.runScript(is);
			is.close();
		} catch(Exception e) {
			getLogger().warn("Problem running job", e);
		}
	}

}
