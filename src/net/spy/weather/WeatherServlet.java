/*
 * Copyright (c) 1999 Dustin Sallings
 *
 * $Id: WeatherServlet.java,v 1.3 2001/01/08 06:17:18 dustin Exp $
 */

package net.spy.weather;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.net.*;

import javax.servlet.*;
import javax.servlet.http.*;

import net.spy.jwebkit.JWHttpServlet;

// The class
public class WeatherServlet extends JWHttpServlet {

	protected WeatherMonitor wm=null;

	// The once only init thingy.
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		String server=config.getInitParameter("server");
		if(server == null) {
			throw new ServletException("Misconfigured, need a weather server");
		}
		log("Will be listening to " + server);
		try {
			wm=new WeatherMonitor(server);
		} catch(Exception e) {
			throw new ServletException("Error getting WeatherMonitor: ", e);
		}
	}

	// The occasional destroy.
	public void destroy() {
		wm.requestStop();
		wm=null;
	}

	protected void doGetOrPost(
		HttpServletRequest request, HttpServletResponse response
	) throws ServletException, IOException {

		String rv=null;
		String format=request.getParameter("format");
		if(format==null) {
			format="plain";
		}

		if(format.equals("xml")) {
			rv=wm.getXML();
		} else {
			rv=wm.getSummary();
		}

		if(rv.length()==0) {
			// Give it another shot if we didn't get anything.
			try {
				Thread.sleep(2000);
				if(format.equals("xml")) {
					rv=wm.getXML();
				} else {
					rv=wm.getSummary();
				}
			} catch(Exception e) {
				log("Problem getting weather stuff", e);
			}
		}

		if(format.equals("xml")) {
			rv = "<?xml version=\"1.0\"?>\n" + rv;
		}

		sendPlain(rv, response);
	}

}
