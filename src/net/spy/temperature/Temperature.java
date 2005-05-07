/*
 * Copyright (c) 1999 Dustin Sallings
 *
 * $Id: Temperature.java,v 1.22 2003/06/28 23:18:20 dustin Exp $
 */

package net.spy.temperature;

import java.io.IOException;
import java.io.PrintWriter;

import java.net.URL;
import java.net.InetAddress;
import java.net.MalformedURLException;

import java.util.Calendar;
import java.util.Map;
import java.util.Iterator;

import java.text.SimpleDateFormat;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.awt.Graphics;

import net.spy.cache.SpyCache;
import net.spy.png.PngServlet;
import net.spy.png.StupidImageObserver;
import net.spy.png.ImageLoader;

import net.spy.cron.Cron;
import net.spy.cron.JobQueue;
import net.spy.cron.TimeIncrement;

// The class
public class Temperature extends PngServlet {

	// Nightly SQL script
	private static final String NIGHTLY_SQL="net/spy/temperature/nightly.sql";

	// shared gatherer instance
	static Gatherer gatherer=null;

	private SpyCache cache=null;

	private Cron cron=null;

	// Image stuff.
	private Image baseImage=null;
	private Color black=null;
	private Color white=null;
	private Font font=null;

	// The once only init thingy.
	public void init(ServletConfig config) throws ServletException {
		super.init(config);

		try {
			gatherer=Gatherer.getInstance();
		} catch(IOException e) {
			throw new ServletException("Could not initialize the gatherer.",e);
		}

		try {
			log("Getting the base image.");
			getBaseImage();
			log("Got the base image.");
			cache=SpyCache.getInstance();

		} catch(Exception e) {
			throw new ServletException("Error getting base image", e);
		}

		// Initializing image stuff.
		black=new Color(0, 0, 0);
		white=new Color(255, 255, 255);
		font=new Font("SanSerif", Font.PLAIN, 10);

		// Set up the cron.
		cron=new Cron(new JobQueue());

		setupJobs();
	}

	private void setupJobs() {
		TimeIncrement ti=new TimeIncrement();
		// Do this every day...
		ti.setField(Calendar.DAY_OF_MONTH);
		ti.setIncrement(1);
		// ...at the same time
		Calendar cal=Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, 2);
		cal.set(Calendar.MINUTE, 3);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		// Add a day so it happens tomorrow.  This wil cause us to skip a day
		// if we deploy between midnight and 2:03.  Woo.
		cal.add(Calendar.DAY_OF_MONTH, 1);

		log("Queueing nightly job starting at " + cal.getTime());
		cron.getJobQueue().addJob(new ScriptRunner("nightly rollup",
			cal.getTime(), ti, NIGHTLY_SQL));
	}

	/** 
	 * Shut down the gatherer.
	 */
	public void destroy() {
		log("Shutting down gatherer.");
		gatherer.stopRunning();
		cron.shutdown();
		super.destroy();
	}

	private boolean hasOption(HttpServletRequest request, String op) {
		String pathInfo = request.getPathInfo();
		boolean rv=request.getParameter(op) != null
			|| (pathInfo != null && pathInfo.indexOf(op) >= 0);
		return(rv);
	}

	// Do a GET request
	public void doGetOrPost (
		HttpServletRequest request, HttpServletResponse response
	) throws ServletException, IOException {

		String which=request.getParameter("temp");
		String therm=request.getParameter("therm");
		String out="";
		if(which==null) {
			// If there's no which, check for a therm
			if(therm!=null) {
				// Show the graphical representation of the temperature
				try {
					writeImage(request, response, getTherm(therm));
				} catch(Exception e) {
					throw new ServletException("Error sending image", e);
				}
			} else {
				// If there's no therm, and no out, list the temps
				// If readings is provided, show the readings with the
				// thermometers

				String encodings=request.getHeader("Accept");
				// If the wml parameter is given, or we figure out the
				// browser supports it
				if( hasOption(request, "wml") || 
					(encodings!=null && encodings.indexOf("text/vnd.wap.wml")>0)
					) {

					// WML support, let's do the WML page.
					out=getWML();
					log("Sending wml response (" + encodings + ")");
					sendResponse(out, "text/vnd.wap.wml", response);
				} else if(hasOption(request, "xml")) {
					out=getXML();
					log("Sending xml response");
					sendResponse(out, "text/xml", response);
				} else {
					log("Sending plain response (" + encodings + ")");
					// No WML support, do the HTML things.
					if(request.getParameter("readings") != null) {
						out=listReadings();
						sendPlain(out, response);
					} else {
						out=listTemps();
						sendPlain(out, response);
					}
				}
			}
		} else {
			// Show the non-graphical representation of the temperature
			out=getTemp(which);
			sendPlain(out, response);
		}
	}

	private String listTemps() {
		String ret="";

		for(Iterator i=gatherer.getSeen().keySet().iterator(); i.hasNext();) {
			ret+=i.next() + "\n";
		}

		return(ret);
	}

	private String listReadings() {
		String ret="";

		for(Iterator i=gatherer.getSeen().entrySet().iterator(); i.hasNext();) {
			Map.Entry me=(Map.Entry)i.next();
			ret+=me.getKey() + "=" + me.getValue() + "\n";
		}

		return(ret);
	}

	private String getWML() {
		StringBuffer sb=new StringBuffer(64);

		SimpleDateFormat sdf=new SimpleDateFormat("yyyyMMdd HHmmss");

		sb.append("<?xml version=\"1.0\"?>\n");
		sb.append("<!DOCTYPE wml PUBLIC ");
		sb.append("\"-//WAPFORUM//DTD WML 1.1//EN\" ");
		sb.append("\"http://www.wapforum.org/DTD/wml_1.1.xml\">\n");
		sb.append("<wml>\n");
		sb.append("<card title=\"Temperatures\">\n");
		sb.append("<p>");
		sb.append(sdf.format(new java.util.Date()));
		sb.append("</p>\n");
		sb.append("<p>\n");
		// Add the contents
		for(Iterator i=gatherer.getSeen().entrySet().iterator(); i.hasNext();) {
			Map.Entry me=(Map.Entry)i.next();
			sb.append(me.getKey());
			sb.append("=");
			Sample s=(Sample)me.getValue();
			sb.append(s.getSample());
			sb.append("<br/>\n");
		}
		sb.append("</p>\n");
		sb.append("</card>\n");
		sb.append("</wml>");

		return(sb.toString());
	}

	private String getXML() {
		StringBuffer sb=new StringBuffer(64);

		sb.append("<?xml version=\"1.0\"?>\n");
		sb.append("<therms>\n");
		// Add the contents
		for(Iterator i=gatherer.getSeen().entrySet().iterator(); i.hasNext();) {
			Map.Entry me=(Map.Entry)i.next();
			sb.append("<therm id=\"");
			sb.append(me.getKey());
			sb.append("\">");
			Sample s=(Sample)me.getValue();
			sb.append(s.getSample());
			sb.append("</therm>\n");
		}
		sb.append("</therms>");

		return(sb.toString());
	}

	private String getTemp(String which)
		throws ServletException {

		Double dv=gatherer.getSeen(which);
		if(dv == null) {
			throw new ServletException("No value for " + which);
		}
		double t=dv.doubleValue();
		int temptmp=(int)(t*100.0);
		t=(double)temptmp/100;
		return("" + t);
	}

	private void sendResponse(String o, String type, HttpServletResponse res)
		throws IOException {
		res.setContentType(type);
		sendSimple(o, res);
	}

	// Graphical representation of the image.
	private Image getTherm(String which)
		throws ServletException {

		int width=133;
		int height=132;

		// Stuff we need to calculate the temperature
		double x2, y2;
		int trans;
		double rad, angle;
		double temp=getTempD(which);

		// Create an image the size we want
		Image image = createImage(width, height);

		// Get a graphics object to draw on the image
		Graphics g = image.getGraphics();
		// Prepare the drawing stuff.
		g.setFont(font);
		g.setColor(black);

		// Draw the base thermometer
		g.drawImage(baseImage, 0, 0, new StupidImageObserver());

		// Translate the angle because we're a little crooked
		trans=-90;

		// Calculate the angle based on the temperature
		angle=(temp*1.8)+trans;
		// Calculate the angle in radians
		rad=( (angle/360) * 2 * 3.14159265358979d );
		// Find the extra points
		x2=Math.sin(rad)*39;
		y2=Math.cos(rad)*39;
		// Negate the y, we're upside-down
		y2=-y2;
		// Move over to the starting points.
		x2+=66; y2+=65;
		// Draw the line.
		g.drawLine(66, 65,  (int)x2, (int)y2);
		// Draw the temperature
		g.drawString("" + temp, 52, 82);

		return(image);
	}

	// Get the temperature as a double (For the image)
	private double getTempD(String which)
		throws ServletException {

		Double d=gatherer.getSeen(which);
		if(d == null) {
			throw new ServletException("No value for " + which);
		}
		double rv = d.doubleValue();
		return(rv);
	}

	// Get the base image loaded
	private Image getBaseImage() throws Exception {
		if(baseImage==null) {
			String therm=getServletConfig().getInitParameter("baseImage");
			URL url=null;

			// Try to get this locally first.
			try {
				url=getServletContext().getResource(therm);
			} catch(MalformedURLException me) {
				// Nothing, it will stay null, and the next thing will be
				// tried.
			}
			// If not, try to make a URL out of it.
			if(url==null) {
				url=new URL(therm);
			}
			log("Getting image (" + url + ")");

			ImageLoader il=new ImageLoader(url);
			baseImage=il.getImage();
		}
		return(baseImage);
	}
}
