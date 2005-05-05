/*
 * Copyright (c) 1999 Dustin Sallings
 *
 * $Id: HouseServlet.java,v 1.1 2003/06/28 23:19:01 dustin Exp $
 */

package net.spy.temperature;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.spy.util.SpyConfig;
import net.spy.util.SpyUtil;

import java.awt.Image;
import java.awt.Color;
import java.awt.Graphics;

import net.spy.png.PngServlet;
import net.spy.png.StupidImageObserver;
import net.spy.png.ImageLoader;

// The class
public class HouseServlet extends PngServlet { 

	// The base house image.
	private Image baseImage=null;
	private boolean imageLoaded=false;

	// The house config file path
	private String houseConfig=null;

	// The once only init thingy.
	public void init(ServletConfig config) throws ServletException {
		super.init(config);

		// Find the house config
		houseConfig=config.getInitParameter("houseConfig");
		if(houseConfig.startsWith("/WEB-INF")) {
			houseConfig=getServletContext().getRealPath(houseConfig);
		}
		log("Using the following config file:  " + houseConfig);

		String bi=config.getInitParameter("baseImage");

		// If the base image begins with a /, look up the real path
		if(bi.startsWith("/")) {
			try {
				URL biurl=config.getServletContext().getResource(bi);
				if(biurl!=null) {
					bi=biurl.toString();
				}
			} catch(MalformedURLException me) {
				me.printStackTrace();
			}
		}

		try {
			getBaseImage(new URL(bi));
		} catch(MalformedURLException e) {
			throw new ServletException("Error making URL out of " + bi, e);
		} catch(IOException e) {
			throw new ServletException("Error getting image", e);
		}
	}

	// Do a GET request
	public void doGet (
		HttpServletRequest request, HttpServletResponse response
	) throws ServletException, IOException {
		try {
			writeImage(request, response, getHouseImage());
		} catch(Exception e) {
			e.printStackTrace();
			throw new ServletException("Error getting image", e);
		}
	}

	private Color getFillColor(SpyConfig conf, String name, double reading) {
		Color rv=null;

		int min=conf.getInt(name + ".min", 0);
		int max=conf.getInt(name + ".max", 100);
		double normal=((double)min) + (((double)(max-min)) / 2.0);
		double maxDistance = (double)(max - min);

		if(reading < min) {
			rv=Color.BLUE;
		} else if(reading > max) {
			rv=Color.RED;
		} else {
			// Dynamically calculate a color.
			Color baseColor=(reading > normal ? Color.RED : Color.BLUE);
			double distance=Math.abs(reading - normal);
			float distancePercent=(float)(distance / maxDistance);

			// Figure out the HSB of the base color
			float parts[]=Color.RGBtoHSB(baseColor.getRed(),
				baseColor.getGreen(), baseColor.getBlue(), null);
			// Brightness should range from 90-100
			float brightness=0.9f + (distancePercent/10.0f);
			rv=Color.getHSBColor(parts[0], distancePercent, brightness);
			/*
			log("Reading for " + name + " " + reading
				+ " " + min + " - " + max
				+ " normal: " + normal
				+ " distance: " + distance + " percent:  " + distancePercent
				+ " colorHSB(" + parts[0] + ","
				+ distancePercent + "," + parts[1] + ")");
			*/
		}
		return(rv);
	}

	// Graphical representation of the image.
	private Image getHouseImage() throws Exception {
		Image img=createImage(307, 223);
		Graphics g=img.getGraphics();
		g.drawImage(baseImage, 0, 0, new StupidImageObserver());

		ServletConfig sconf=getServletConfig();

		// Get the temperature servlet
		String tempUrl=sconf.getInitParameter("tempServlet");

		// The description of what we're drawing.
		SpyConfig conf=new SpyConfig(new File(houseConfig));

		// Find all the things we need to colorize
		String things[]=SpyUtil.split(" ", conf.get("colorize", ""));
		for(int i=0; i<things.length; i++) {
			int x, y, w, h;

			String rstring=null;

			x=conf.getInt(things[i] + ".rect.x", 0);
			y=conf.getInt(things[i] + ".rect.y", 0);
			w=conf.getInt(things[i] + ".rect.w", 0);
			h=conf.getInt(things[i] + ".rect.h", 0);
			// Default color is white
			g.setColor(Color.WHITE);

			try {
				Double dreading=Temperature.gatherer.getSeen(things[i]);
				double reading=dreading.doubleValue();
				rstring="" + reading;

				// Draw a black border
				g.setColor(Color.BLACK);
				g.fillRect(x-1, y-1, w+2, h+2);
				g.setColor(Color.WHITE);

				// Set the color based on the temperature reading.
				Color fillColor=getFillColor(conf, things[i], reading);
				g.setColor(fillColor);

				// Stick the color all up in there.
				g.fillRect(x, y, w, h);
			} catch(Exception e) {
				e.printStackTrace();
				rstring="??.??";
			}
			// Put the reading in there.
			g.setColor(Color.BLACK);
			int stringx=conf.getInt(things[i] + ".reading.x", (x+(w/2)-18));
			int stringy=conf.getInt(things[i] + ".reading.y", (y+(h/2)+4));
			g.drawString(rstring, stringx, stringy);
		}

		return(img);
	}

	private void getBaseImage(URL url) throws IOException {
		ImageLoader il=new ImageLoader(url);
		baseImage=il.getImage();
	}
}
