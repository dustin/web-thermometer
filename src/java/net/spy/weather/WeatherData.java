/*
 * Copyright (c) 2000  Dustin Sallings <dustin@spy.net>
 *
 * $Id: WeatherData.java,v 1.1 2000/11/19 12:06:46 dustin Exp $
 */

package net.spy.weather;

import java.util.HashMap;
import java.util.Map;

public class WeatherData extends Object {

	/**
	 * Type for time and humidity.
	 */
	public static final int TIMEHUMDITY = 0x8f;

	/**
	 * Type for temperature.
	 */
	public static final int TEMP = 0x9f;

	/**
	 * Type for barometric pressure and dew point.
	 */
	public static final int BAROMETERDEW = 0xaf;

	/**
	 * Type for rain.
	 */
	public static final int RAIN = 0xbf;

	/**
	 * Type for wind.
	 */
	public static final int WIND = 0xcf;

	/**
	 * Type for time only.
	 */
	public static final int TIME = 0xff;

	// Data types
	protected static Map types = null;

	/**
	 * Celsius unit for temperature.
	 */
	public static final int CELSIUS = 0;

	/**
	 * Fahrenheit unit for temperature.
	 */
	public static final int FAHRENHEIT = 1;

	// This data type
	protected int type = -1;

	// This data
	protected byte data[] = null;

	// temperature units
	protected int temp_unit = FAHRENHEIT;

	public WeatherData(int t, byte d[]) {
		super();
		this.type = t;
		this.data = d;
		initDataTypes();
	}

	public String toString() {
		String type_str = (String) types.get(new Integer(type));
		return (type_str);
	}

	// Binary coded decimal decoder.
	protected int getBCDInt(byte b) {
		return (getBCDInt((int) b));
	}

	// Binary coded decimal decoder.
	protected int getBCDInt(int i) {
		int rv = 0;
		rv = (((i & 0xf0) >> 4) * 10)
			+ (i & 0x0f);
		return (rv);
	}

	protected static synchronized void initDataTypes() {
		types = new HashMap();

		types.put(new Integer(0x8f), "Time and Humidity");
		types.put(new Integer(0x9f), "Temperature");
		types.put(new Integer(0xaf), "Barometer and Dew Point");
		types.put(new Integer(0xbf), "Rain");
		types.put(new Integer(0xcf), "Wind");
		types.put(new Integer(0xff), "Time");
	}

	/**
	 * Set the temperature unit.
	 */
	public void setTempUnits(int unit) {
		this.temp_unit = unit;
	}

	/**
	 * Convert the temperature.
	 */
	protected double convertTemp(double d) {
		switch(temp_unit) {
			case CELSIUS:
				break;
			case FAHRENHEIT:
				d = (32.0 + (1.8 * d));
				break;
			default:
				throw new RuntimeException("Illegal temperature format:  "
					+ temp_unit);
		}
		return (d);
	}

}
