/*
 * Copyright (c) 2000  Dustin Sallings <dustin@spy.net>
 *
 * $Id: WindData.java,v 1.4 2001/01/08 06:17:20 dustin Exp $
 */

package net.spy.weather;

import java.text.NumberFormat;

public class WindData extends WeatherData {

	/**
	 * Miles per hour
	 */
	public static final int MPH=0;
	/**
	 * Knots
	 */
	public static final int KNOTS=1;
	/**
	 * Meters per second.
	 */
	public static final int MPS=2;
	/**
	 * Kilometers per hour.
	 */
	public static final int KPH=3;

	protected int speed_unit=MPH;

	public WindData(int type, byte data[]) {
		super(type, data);
		if(type!=WIND) {
			throw new Error("Invalid type code for wind:  " + type);
		}
	}

	/**
	 * Set the wind speed unit.
	 */
	public void setWindSpeedUnit(int towhat) {
		speed_unit=towhat;
	}

	public String toString() {
		NumberFormat nf=NumberFormat.getInstance();
		nf.setMaximumFractionDigits(2);
		return("Wind speed:  " + nf.format(getWindSpeed()));
	}

	public String toXML() {
		StringBuffer sb=new StringBuffer();

		sb.append("\t<wind>\n");
		sb.append("\t\t" + getWindSpeed() + "\n");
		sb.append("\t</wind>\n");

		return(sb.toString());
	}

	/**
	 * Get the wind speed converted to the current unit.
	 */
	public double getWindSpeed() {
		double bc=(double)getBCDInt(data[0]);
		double a=(double)getBCDInt((int)data[1]&0xf);

		double d=(a*10)+(bc/10.0);

		return(convertSpeed(d));
	}

	protected double convertSpeed(double d) {
		switch(speed_unit) {
			case MPH:
				d*=2.23693629205;
				break;
			case KNOTS:
				d*=1.94384449244;
				break;
			case MPS:
				break;
			case KPH:
				d*=3.6;
				break;
		}
		return(d);
	}
}
