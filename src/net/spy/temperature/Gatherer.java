// Copyright (c) 2002  Dustin Sallings <dustin@spy.net>
//
// $Id: Gatherer.java,v 1.5 2003/06/28 23:18:19 dustin Exp $

package net.spy.temperature;

import java.io.IOException;

import java.util.Collections;
import java.util.Map;
import java.util.List;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.MissingResourceException;
import java.util.StringTokenizer;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import net.spy.SpyThread;

/**
 * Gather temperatures from a multicast socket.
 */
public class Gatherer extends SpyThread {

	// Maximum number of milliseconds we'll keep a record that hasn't been
	// updated
	private static final long MAX_AGE=900000;
	// Maximum number of historical records we'll keep
	private static final int MAX_HISTORY=120;

	private static Gatherer instance=null;

	private MulticastSocket s=null;
	private InetAddress group=null;
	private int port=-1;
	private boolean running=true;

	private int updates=0;
	// The map of names to current samples
	private Map seen=null;
	// To hold historical samples
	private Map history=null;
	private ResourceBundle serials=null;

	/**
	 * Get an instance of Gatherer.
	 */
	private Gatherer(InetAddress group, int port) throws IOException {
		super();

		this.group=group;
		this.port=port;

		getLogger().info("Initializing gatherer on " + group + ":" + port);
		seen=Collections.synchronizedMap(new TreeMap());
		history=Collections.synchronizedMap(new TreeMap());
		// Serial number -> name mapping
		serials=ResourceBundle.getBundle("net.spy.temperature.therms");

		s=new MulticastSocket(port);
		s.joinGroup(group);

		setName("Temperature Gatherer");
		setDaemon(true);
		start();
	}

	/** 
	 * Get the singleton Gatherer instance.
	 */
	public static synchronized Gatherer getInstance() throws IOException {
		if(instance == null) {
			InetAddress ia=InetAddress.getByName("225.0.0.37");
			int port=6789;
			instance=new Gatherer(ia, port);
		}
		return(instance);
	}

	/** 
	 * String me.
	 */
	public String toString() {
		StringBuffer sb=new StringBuffer(128);

		sb.append(super.toString());
		sb.append(" - has read ");
		sb.append(updates);
		sb.append(" updates, currently tracking ");
		sb.append(seen.size());
		sb.append(" thermometers");

		return(sb.toString());
	}

	/** 
	 * Shut down the socket.
	 */
	public void stopRunning() {
		running=false;

		try {
			s.leaveGroup(group);
			s.close();
		} catch(IOException e) {
			getLogger().warn("Problem leaving multicast group.", e);
		}
	}

	/** 
	 * Get a read-only view of the current readings.
	 * 
	 * @return an unmodifiable view of the seen map
	 */
	public Map getSeen() {
		return(Collections.unmodifiableMap(seen));
	}

	/** 
	 * Get the current value for the named thermometer.
	 * 
	 * @param name name of the thermometer
	 * @return the value, or null if there's no mapping or the value is
	 * unavailable
	 */
	public Double getSeen(String name) {
		Double rv=null;
		Sample s=(Sample)seen.get(name);

		if(s != null) {
			if(s.age() > MAX_AGE) {
				getLogger().warn("Reading for "
					+ name + " is too old, removing");
				seen.remove(name);
			} else {
				// This one is of a reasonable vintage, return it
				rv=s.getSample();
			}
		}

		return(rv);
	}

	/** 
	 * Get the recent history of readings for the given thermometer.
	 * 
	 * @param name the name of the thermometer
	 * @return an unmodifiable List of Sample objects
	 */
	public List getHistory(String name) {
		List rv=Collections.EMPTY_LIST;
		List hist=(List)history.get(name);
		if(hist != null) {
			rv=Collections.unmodifiableList(hist);
		}
		return(rv);
	}

	private void process(DatagramPacket recv) throws IOException {
		byte data[]=recv.getData();
		byte tmp[]=new byte[recv.getLength()];
		System.arraycopy(data, 0, tmp, 0, tmp.length);
		String entry=new String(tmp); 

		StringTokenizer st = new StringTokenizer(entry, "\t");
		if(st.countTokens() < 3) {
			throw new IOException("This message doesn't make sense:  " + entry);
		}
		String date_str = st.nextToken();
		String serial = st.nextToken();
		String sample_str = st.nextToken();
		
		Double sample_val=new Double(sample_str);

		// The key for the storage
		String key=null;

		// figure out if there's a key mapping
		try {
			key=serials.getString(serial);
			if(getLogger().isDebugEnabled()) {
				getLogger().debug("Got sample for " + key + " " + sample_val);
			}
		} catch(MissingResourceException e) {
			// Only log this if we haven't seen it before
			if(!seen.containsKey(serial)) {
				getLogger().warn("Unknown serial number seen:  " + serial, e);
			}
			key=serial;
		}

		// See if we need to update an existing key, or add a new one
		Sample sample=(Sample)seen.get(key);
		if(sample==null) {
			sample=new Sample(key);
			seen.put(key, sample);
		}
		sample.setSample(sample_val);

		// Also store the history
		LinkedList readingHistory=(LinkedList)history.get(key);
		if(readingHistory == null) {
			readingHistory=new LinkedList();
			history.put(key, readingHistory);
		}
		Sample tmps=new Sample(key, sample_val);
		readingHistory.addFirst(tmps);
		if(readingHistory.size() > MAX_HISTORY) {
			readingHistory.removeLast();
		}

		updates++;
	}

	// Clean up any old entries
	private void cleanup() {
		for(Iterator i=seen.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry me=(Map.Entry)i.next();

			Sample s=(Sample)me.getValue();
			if(s.age() > MAX_AGE) {
				getLogger().warn("Removing old entry:  " + s);
				i.remove();
				// Remove the history of this one, too.
				history.remove(me.getKey());
			}
		}
	}

	/** 
	 * Watch for incoming temperature updates and record them.
	 */
	public void run() {
		while(running) {
			try {
				byte data[]=new byte[1500];
				DatagramPacket recv = new DatagramPacket(data, data.length);
				s.receive(recv);
				process(recv);

				cleanup();

			} catch(IOException e) {
				getLogger().error("Exception processing temperature packet", e);
			} catch(Throwable t) {
				getLogger().fatal("UNEXPECTED ERROR IN GATHERER", t);
			}
		}
	}

}
