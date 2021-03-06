/*
 * Copyright (c) 2000  Dustin Sallings <dustin@spy.net>
 *
 * $Id: TemperatureData.java,v 1.3 2001/01/08 06:17:16 dustin Exp $
 */

package net.spy.weather;

import java.text.NumberFormat;

public class TemperatureData extends WeatherData {
	/**
	 * Get a temperature data object.
	 */
	public TemperatureData(int t, byte d[]) {
		super(t, d);
		if(t != TEMP) {
			throw new IllegalArgumentException(
				"This is not a temperature data set:  "
					+ t);
		}
	}

	@Override
	public String toString() {
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMaximumFractionDigits(2);
		String rv = "Temperature:  Outdoor "
			+ nf.format(getOutdoorTemp()) + " - Indoor:  "
			+ nf.format(getIndoorTemp());
		return (rv);
	}

	public String toXML() {
		StringBuffer sb = new StringBuffer();

		sb.append("\t<temperature>\n");
		sb.append("\t\t<indoor>\n");
		sb.append("\t\t\t"
			+ getIndoorTemp() + "\n");
		sb.append("\t\t</indoor>\n");
		sb.append("\t\t<outdoor>\n");
		sb.append("\t\t\t"
			+ getOutdoorTemp() + "\n");
		sb.append("\t\t</outdoor>\n");
		sb.append("\t</temperature>\n");

		return (sb.toString());
	}

	/**
	 * Get the outdoor temperature.
	 */
	public double getOutdoorTemp() {
		double bc = getBCDInt(data[15]);
		double a = (data[16] & 0x7);
		double d = (a * 10)
			+ (bc / 10);

		if(((data[16]) & 0x8) != 0) {
			d = 0 - d;
		}

		d = convertTemp(d);

		return (d);
	}

	/**
	 * Get the indoor temperature.
	 */
	public double getIndoorTemp() {
		double bc = getBCDInt(data[0]);
		double a = (data[1] & 0x7);
		double d = (a * 10)
			+ (bc / 10);

		if(((data[1]) & 0x8) != 0) {
			d = 0 - d;
		}

		d = convertTemp(d);

		return (d);
	}

}
