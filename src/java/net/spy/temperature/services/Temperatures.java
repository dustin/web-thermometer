// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// $Id: Temperatures.java,v 1.3 2002/11/11 18:22:22 dustin Exp $

package net.spy.temperature.services;

import java.util.*;
import java.io.IOException;

import net.spy.rpc.services.Remote;

import net.spy.temperature.Sample;
import net.spy.temperature.Gatherer;

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
	public Vector listTherms() throws Exception {
		return(new Vector(gatherer.getSeen().keySet()));
	}

	/**
	 * Get all temperatures from all thermometers.
	 */
	public Hashtable getTemperatures() throws Exception {
		Hashtable ht=new Hashtable();
		for(Iterator i=gatherer.getSeen().entrySet().iterator(); i.hasNext();) {
			Map.Entry me=(Map.Entry)i.next();
			Sample s=(Sample)me.getValue();
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
