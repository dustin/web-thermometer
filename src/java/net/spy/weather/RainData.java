/*
 * Copyright (c) 2000  Dustin Sallings <dustin@spy.net>
 *
 * $Id: RainData.java,v 1.4 2001/01/08 06:17:14 dustin Exp $
 */

package net.spy.weather;

import java.text.NumberFormat;

public class RainData extends WeatherData {

	/**
	 * Depth units - millimeters.
	 */
	public static final int MM=0;
	/**
	 * Depth units - inches.
	 */
	public static final int IN=1;

	protected int depth_unit=IN;

	/**
	 * Get a Rain data object.
	 */
	public RainData(int type, byte data[]) {
		super(type, data);
		if(type!=RAIN) {
			throw new Error("This is not a rain data set:  " + type);
		}
	}

	/**
	 * Get the current rate of rain.
	 */
	public String toString() {
		NumberFormat nf=NumberFormat.getInstance();
		nf.setMaximumFractionDigits(2);
		String rv="Rain:  Current rate:  " + nf.format(getRainRate());
		return(rv);
	}

	public String toXML() {
		StringBuffer sb=new StringBuffer();

		sb.append("\t<rain>\n");
		sb.append("\t\t" + getRainRate() + "\n");
		sb.append("\t</rain>\n");

		return(sb.toString());
	}

	/**
	 * Set the unit to measure rain depth.
	 */
	public void setDepthUnit(int to) {
		depth_unit=to;
	}

	/**
	 * Get the current rain rate.
	 */
	public double getRainRate() {
		int bc=getBCDInt(data[0]);
		int a=getBCDInt((int)data[1]&0xf);

		double d=(double)((a*100) + bc);

		d=convertDepth(d);
		return(d);
	}

	protected double convertDepth(double d) {
		switch(depth_unit) {
			case MM:
				break;
			case IN:
				d*=0.0393700787402;
				break;
			default:
				throw new Error("Invalid depth unit:  " + depth_unit);
		}
		return(d);
	}

}
