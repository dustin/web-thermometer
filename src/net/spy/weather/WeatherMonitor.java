/*
 * Copyright (c) 2000  Dustin Sallings <dustin@spy.net/>
 *
 * $Id: WeatherMonitor.java,v 1.5 2001/01/08 06:17:17 dustin Exp $
 */

package net.spy.weather;

import java.net.*;

import net.spy.util.LoopingThread;

/**
 * This object represents a stream of data from a weather station.
 */
public class WeatherMonitor extends LoopingThread {
	private WMSocket wx200d=null;

	private WindData windData=null;
	private BarometerData barometerData=null;
	private HumidityData humidityData=null;
	private TemperatureData temperatureData=null;
	private RainData rainData=null;

	private String server=null;
	private int port=-1;

	public WeatherMonitor(String hostname, int port) throws Exception {
		super("Weather Monitor");
		this.server=hostname;
		this.port=port;

		wx200d=new WMSocket(hostname, port);
		setName("Weather Monitor");
		setDaemon(true);
		setMsPerLoop(1000);
		start();

	}

	public WeatherMonitor(String hostname) throws Exception {
		this(hostname, WMSocket.DEFAULT_PORT);
	}

	/**
	 * Get the last seen Rain Data.
	 */
	public RainData getRainData() {
		return(rainData);
	}

	/**
	 * Get the last seen Temperature Data.
	 */
	public TemperatureData getTemperatureData() {
		return(temperatureData);
	}

	/**
	 * Get the last seen Humidity Data.
	 */
	public HumidityData getHumidityData() {
		return(humidityData);
	}

	/**
	 * Get the last seen Barometer Data.
	 */
	public BarometerData getBarometerData() {
		return(barometerData);
	}

	/**
	 * Get the last seen Wind Data.
	 */
	public WindData getWindData() {
		return(windData);
	}

	/**
	 * Get a textual weather summary.
	 */
	public String getSummary() {
		StringBuffer rv=new StringBuffer();

		if(windData!=null) {
			rv.append(windData + "\n");
		}
		if(barometerData!=null) {
			rv.append(barometerData+"\n");
		}
		if(humidityData!=null) {
			rv.append(humidityData+"\n");
		}
		if(temperatureData!=null) {
			rv.append(temperatureData+"\n");
		}
		if(rainData!=null) {
			rv.append(rainData+"\n");
		}
		return(rv.toString());
	}

	/**
	 * Get the weather data as XML.
	 */
	public String getXML() {
		StringBuffer rv=new StringBuffer();

		rv.append("<weather>\n");

		if(windData!=null) {
			rv.append(windData.toXML());
		}

		if(barometerData!=null) {
			rv.append(barometerData.toXML());
		}

		if(humidityData!=null) {
			rv.append(humidityData.toXML());
		}

		if(temperatureData!=null) {
			rv.append(temperatureData.toXML());
		}

		if(rainData!=null) {
			rv.append(rainData.toXML());
		}

		rv.append("</weather>\n");

		return(rv.toString());
	}

	protected void runLoop() {
		try {
			WeatherData wd=wx200d.readGroup();

			// Figure out how to store that.
			if(wd instanceof WindData) {
				windData=(WindData)wd;
			} else if(wd instanceof BarometerData) {
				barometerData=(BarometerData)wd;
			} else if(wd instanceof HumidityData) {
				humidityData=(HumidityData)wd;
			} else if(wd instanceof TemperatureData) {
				temperatureData=(TemperatureData)wd;
			} else if(wd instanceof RainData) {
				rainData=(RainData)wd;
			}
		} catch(Exception e) {
			// If there was an error, reinitialize our connection to
			// the wx200d
			try {
				if(port>0) {
					wx200d=new WMSocket(server, port);
				} else {
					wx200d=new WMSocket(server);
				}
			} catch(Exception e2) {
				System.err.println(
					"Error reinitializing wx200d connection");
			}
		}
	}

	public static void main(String args[]) throws Exception {
		WeatherMonitor wm=new WeatherMonitor("juan", 9753);
		while(true) {
			Thread.sleep(1000);
			System.out.println(wm.getSummary());
		}
	}
}
