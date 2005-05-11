/*
 * Copyright (c) 2000  Dustin Sallings <dustin@spy.net>
 *
 * $Id: WMReader.java,v 1.2 2000/11/19 23:56:12 dustin Exp $
 */

package net.spy.weather;

import java.io.IOException;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 * Return WeatherData objects from an InputStream
 */
public class WMReader extends InputStream {

	// Our host input stream.
	InputStream is=null;

	/**
	 * Get a WMReader object that gets its info from the passed InputStream
	 */
	public WMReader(InputStream is) {
		super();
		this.is=is;
	}

	/**
	 * Read the next WeatherData group from the input stream.
	 *
	 * @return a WeatherData object, or null if there aren't anymore.
	 */
	public WeatherData readGroup() throws IOException {

		// Get the type
		int type=read();
		int bytestoread=0;
		WeatherData wd=null;

		// Figure out how many bytes to read for each type
		switch(type) {
			case 0x8f:
				bytestoread=34;
				break;
			case 0x9f:
				bytestoread=33;
				break;
			case 0xaf:
				bytestoread=30;
				break;
			case 0xbf:
				bytestoread=13;
				break;
			case 0xcf:
				bytestoread=26;
				break;
			case 0xff:
				bytestoread=4;
				break;
		}

		if(type>=0) {
			byte array[]=new byte[bytestoread];
			// Fill our array, keep in mind we may not block properly...

			int bytesread=read(array);
			while(bytesread<bytestoread) {
				System.err.println("Need to read " + (bytestoread-bytesread)
					+ " more...");
				bytesread+=read(array, bytesread, bytestoread-bytesread);
			}

			// Get the WeatherData object for this type
			wd=getWeatherData(type, array);
		}
		return(wd);
	}

	private WeatherData getWeatherData(int type, byte array[]) {
		WeatherData wd=null;
		switch(type) {
			case WeatherData.WIND:
				wd=new WindData(type, array);
				break;
			case WeatherData.BAROMETERDEW:
				wd=new BarometerData(type, array);
				break;
			case WeatherData.TIMEHUMDITY:
				wd=new HumidityData(type, array);
				break;
			case WeatherData.TEMP:
				wd=new TemperatureData(type, array);
				break;
			case WeatherData.RAIN:
				wd=new RainData(type, array);
				break;
			default:
				wd=new WeatherData(type, array);
		}
		return(wd);
	}

	/**
	 * Return a byte from our host input stream.
	 */
	public int read() throws IOException {
		return(is.read());
	}

	/**
	 * unit test, dump the data out of a named binary capture file.
	 */
	public static void main(String args[]) throws Exception {
		FileInputStream fis=new FileInputStream(args[0]);
		WMReader wmr=new WMReader(fis);
		WeatherData wd=null;

		do {
			wd=wmr.readGroup();
			if(wd!=null) {
				System.out.println(wd.toString());
			}
		} while(wd!=null);
	}
}
