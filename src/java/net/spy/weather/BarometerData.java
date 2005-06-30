/*
 * Copyright (c) 2000  Dustin Sallings <dustin@spy.net>
 *
 * $Id: BarometerData.java,v 1.5 2001/01/08 06:17:12 dustin Exp $
 */

package net.spy.weather;

import java.text.NumberFormat;

public class BarometerData extends WeatherData {

	/**
	 * Inches of mercury.
	 */
	public static final int INHG=0;
	/**
	 * Millimeters of mercury.
	 */
	public static final int MMHG=1;
	/**
	 * Millibars.
	 */
	public static final int MBAR=2;
	/**
	 * hPa.
	 */
	public static final int HPA=3;

	protected int baro_unit=0;

	public BarometerData(int t, byte d[]) {
		super(t, d);
		if(t!=BAROMETERDEW) {
			throw new IllegalArgumentException("Invalid type code for wind:  " + t);
		}
	}

	/**
	 * Get the weather prediction.
	 *
	 * @return one of Sunny, Cloudy, Partly cloudy, or Rain
	 */
	public String getPrediction() {
		String p=null;
		int d=data[5]&0x0f;
		switch(d) {
			case 1:
				p="Sunny";
				break;
			case 2:
				p="Cloudy";
				break;
			case 4:
				p="Partly cloudy";
				break;
			case 8:
				p="Rain";
				break;
			default:
				throw new RuntimeException("Invalid prediction:  " + d);
		}
		return(p);
	}

	public String toString() {
		NumberFormat nf=NumberFormat.getInstance();
		nf.setMaximumFractionDigits(2);
		String rv="Barometer:  "
			+ nf.format(getPressure());

		switch(getTrend()) {
			case 1:
				rv+=" falling";
				break;
			case -1:
				rv+=" rising";
				break;
		}

		rv+=" (" + getPrediction() + ")";
		rv+=" - oDew Point:  " + nf.format(getOutdoorDewPoint());
		rv+=" - iDew Point:  " + nf.format(getIndoorDewPoint());
		
		return(rv);
	}

	public String toXML() {
		StringBuffer sb=new StringBuffer();

		sb.append("\t<barometer>\n");

		sb.append("\t\t<trend>\n");
		sb.append("\t\t\t");
		switch(getTrend()) {
			case 1:
				sb.append("falling");
				break;
			case 0:
				sb.append("constant");
				break;
			case -1:
				sb.append("rising");
				break;
		}
		sb.append("\n");
		sb.append("\t\t</trend>\n");

		sb.append("\t\t<prediction>\n");
		sb.append("\t\t\t" + getPrediction() + "\n");
		sb.append("\t\t</prediction>\n");

		sb.append("\t\t<indoordew>\n");
		sb.append("\t\t\t" + getIndoorDewPoint() + "\n");
		sb.append("\t\t</indoordew>\n");

		sb.append("\t\t<outsidedew>\n");
		sb.append("\t\t\t" + getOutdoorDewPoint() + "\n");
		sb.append("\t\t</outsidedew>\n");

		sb.append("\t</barometer>\n");

		return(sb.toString());
	}

	/**
	 * Set the unit for barometric pressure.
	 */
	public void setBaroUnits(int unit) {
		this.baro_unit=unit;
	}

	/**
	 * Get the current outdoor dew point.
	 */
	public double getOutdoorDewPoint() {
		double d=getBCDInt(data[17]);
		d=convertTemp(d);
		return(d);
	}

	/**
	 * Get the current indoor dew point.
	 */
	public double getIndoorDewPoint() {
		double d=getBCDInt(data[6]);
		d=convertTemp(d);
		return(d);
	}

	/**
	 * Get the barometric pressure converted for the current units.
	 */
	public double getPressure() {

		int ab=getBCDInt(data[1]);
		int cd=getBCDInt(data[0]);

		double d=(ab*100) + cd;

		d=convertBaro(d);

		return(d);
	}

	/**
	 * Get the trend.
	 *
	 * @return -1 for falling, 0 for steady, 1 for rising.
	 */
	public int getTrend() {
		int d=(data[5]&0x70)>>4;
		int rv=0;

		switch(d) {
			case 1:
				rv=-1;
				break;
			case 2:
				rv=0;
				break;
			case 4:
				rv=1;
				break;
			default:
				throw new RuntimeException("Invalid trend returned:  " + d);
		}

		return(rv);
	}

	// Convert the barometric pressure
	protected double convertBaro(double d) {
		switch(baro_unit) {
			case INHG:
				d*=(0.029529987508);
				break;
			case MMHG:
				d*=(0.750061682704);
				break;
			case MBAR:
				break;
			case HPA:
				break;
			default:
				throw new RuntimeException("Illegal unit type:  " + baro_unit);
		}
		return(d);
	}

}
