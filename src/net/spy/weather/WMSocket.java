/*
 * Copyright (c) 2000  Dustin Sallings <dustin@spy.net>
 *
 * $Id: WMSocket.java,v 1.3 2000/11/19 23:56:14 dustin Exp $
 */

package net.spy.weather;

import java.net.Socket;
import java.net.UnknownHostException;
import java.io.IOException;

/** 
 * Socket to listen for weather updates.
 */
public class WMSocket extends Socket {

	/** 
	 * Default port nubmer for WMSocket connections.
	 */
	public static final int DEFAULT_PORT=9753;

	private WMReader weatherstream=null;

	public WMSocket(String server)
		throws UnknownHostException, IOException {
		this(server, DEFAULT_PORT);
	}

	public WMSocket(String server, int port)
		throws UnknownHostException, IOException {
		super(server, port);
		weatherstream=new WMReader(getInputStream());
	}

	public WeatherData readGroup() throws IOException {
		return(weatherstream.readGroup());
	}

	public static void main(String args[]) throws Exception {
		WMSocket wms=new WMSocket("juan");
		while(true) {
			WeatherData wd=wms.readGroup();
			if(wd!=null) {
				System.out.println(wd.toString());
			}
			Thread.sleep(1000);
		}
	}
}
