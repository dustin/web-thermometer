// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// $Id: Temperatures.java,v 1.3 2002/11/11 18:22:22 dustin Exp $

package net.spy.temperature.services;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import net.spy.rpc.services.Remote;
import net.spy.temperature.Gatherer;
import net.spy.temperature.Sample;

/**
 * XML RPC Interface to temperature gettin'
 */
public class Temperatures extends Remote {

	private Gatherer gatherer=null;

	/**
	 * Get an instance of Temperatures.
	 */
	public Temperatures() throws IOException {
		super();
		gatherer=Gatherer.getInstance();
	}

	/**
	 * List all known thermometers.
	 */
	public Vector<String> listTherms() throws Exception {
		return(new Vector<String>(gatherer.getSeen().keySet()));
	}

	/**
	 * Get all temperatures from all thermometers.
	 */
	public Hashtable<String, Double> getTemperatures() throws Exception {
		Hashtable<String, Double> ht=new Hashtable<String, Double>();
		for(Map.Entry<String, Sample> me : gatherer.getSeen().entrySet()) {
			Sample s=me.getValue();
			ht.put(me.getKey(), s.getSample());
		}
		return(ht);
	}

	/**
	 * Get a specific temperature from a specific thermometer.
	 */
	public Double getTemperature(String therm) throws Exception {
		return(gatherer.getSeen(therm));
	}

}
