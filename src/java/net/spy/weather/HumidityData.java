/*
 * Copyright (c) 2000  Dustin Sallings <dustin@spy.net>
 *
 * $Id: HumidityData.java,v 1.2 2001/01/08 06:17:13 dustin Exp $
 */

package net.spy.weather;

import java.text.NumberFormat;

public class HumidityData extends WeatherData {
	/**
	 * Get a time and humidity data object.
	 */
	public HumidityData(int t, byte d[]) {
		super(t, d);
		if(t!=TIMEHUMDITY) {
			throw new IllegalArgumentException(
				"This is not a time and humidity data set:  "
					+ t);
		}
	}

	/**
	 * Get a printable representation of the humidity data.
	 */
	@Override
	public String toString() {
		NumberFormat nf=NumberFormat.getInstance();
		nf.setMaximumFractionDigits(2);
		String rv="Time and humidity:  Outdoor "
			+ nf.format(getOutdoorHumidity()) + "% - Indoor:  "
			+ nf.format(getIndoorHumidity()) + "%";
		return(rv);
	}

	public String toXML() {
		StringBuffer sb=new StringBuffer();

		sb.append("\t<humidity>\n");
		sb.append("\t\t<indoor>\n");
		sb.append("\t\t\t" + getIndoorHumidity() + "\n");
		sb.append("\t\t</indoor>\n");
		sb.append("\t\t<outdoor>\n");
		sb.append("\t\t\t" + getOutdoorHumidity() + "\n");
		sb.append("\t\t</outdoor>\n");
		sb.append("\t</humidity>\n");

		return(sb.toString());
	}

	/**
	 * Yeah, get the outdoor humidity.
	 */
	public double getOutdoorHumidity() {
		double d=getBCDInt(data[19]);
		return(d);
	}

	/**
	 * Get the indoor humidity.
	 */
	public double getIndoorHumidity() {
		double d=getBCDInt(data[7]);
		return(d);
	}

}
