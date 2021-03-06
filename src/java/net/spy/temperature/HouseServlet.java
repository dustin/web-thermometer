/*
 * Copyright (c) 1999 Dustin Sallings
 *
 * $Id: HouseServlet.java,v 1.1 2003/06/28 23:19:01 dustin Exp $
 */

package net.spy.temperature;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.spy.png.ImageLoader;
import net.spy.png.PngServlet;
import net.spy.png.StupidImageObserver;
import net.spy.util.SpyConfig;
import net.spy.util.SpyUtil;

// The class
public class HouseServlet extends PngServlet { 

	// The base house image.
	private Image baseImage=null;
	// The house config file path
	private SpyConfig houseConfig=null;

	// The once only init thingy.
	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);

		// Find the house config
		String houseConfigPath=getContextRelativeFilePathParameter(
			"houseConfig", "/WEB-INF/house.conf");
		log("Using the following config file:  " + houseConfigPath);
		houseConfig=new SpyConfig(new File(houseConfigPath));

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
	@Override
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

	private Color getFillColor(String name, double reading, float relevance) {
		Color rv=null;

		int min=houseConfig.getInt(name + ".min", 0);
		int max=houseConfig.getInt(name + ".max", 100);
		double normal=min + ((max-min) / 2.0);
		double maxDistance = (max - min);

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
			// Brightness should range from 90-100 depending on the distance
			// from the normal temperature and the distance from the
			// thermometer.
			// As you get further from normal *or* further from thermometer,
			// the color should become brighter.
			float brightness=0.9f + Math.max(
				(distancePercent/10.0f),
				((1.0f-relevance)/10.0f));
			rv=Color.getHSBColor(parts[0], relevance*distancePercent,
				brightness);
		}
		return(rv);
	}

	private void drawPoint(Graphics g, int x, int y, Color c) {
		g.setColor(c);
		g.fillRect(x, y, 1, 1);
	}

	private void fillGradient(String which, Graphics g,
		double reading, int x, int y, int w, int h) {

		// Look in the following locations:
		//  1) which.maxRelevantDistance
		//  2) maxRelevantDistance
		//  3) default to 60
		double maxRelevantDistance = houseConfig.getInt(
			which + ".maxRelevantDistance",
			houseConfig.getInt("maxRelevantDistance", 60));

		// Get the thermometer x and y (defaulting to the center)
		int tx=houseConfig.getInt(which + ".therm.x", x+(w/2));
		int ty=houseConfig.getInt(which + ".therm.y", y+(h/2));

		for(int i=0; i<w; i++) {
			for(int j=0; j<h; j++) {
				// i,j is the point offset within the rect
				// px,py is the absolute point within the diagram
				int px=x+i;
				int py=y+j;
				double xd = (px-tx);
				double yd = (py-ty);
				double distance=Math.sqrt(xd*xd + yd*yd);

				// The relevance of the thermometer on this pixel is inversely
				// proportional to the distance from the thermometer 
				double relevance=1d-(distance / maxRelevantDistance);
				if(relevance < 0) {
					relevance = 0;
				}

				// Transform the color
				Color theColor=getFillColor(which, reading, (float)relevance);
				drawPoint(g, px, py, theColor);
			}
		}
	}

	private void drawLabel(Graphics g, String which, String label,
		int x, int y, int w, int h) {
		g.setColor(Color.BLACK);
		int stringx=houseConfig.getInt(which + ".reading.x", (x+(w/2)-18));
		int stringy=houseConfig.getInt(which + ".reading.y", (y+((h-20)/2)+4));
		// log("Labeling " + label + " at " + stringx + "," + stringy);
		g.drawString(label, stringx, stringy);
	}

	private void drawSparks(Graphics g, double low, double high,
		int x, int y, int w, int h, List<Double> vals) {

		int numPoints=vals.size();
		if(numPoints > w) {
			numPoints=w;
		}

		int xpoints[]=new int[numPoints];
		int ypoints[]=new int[numPoints];

		// Just get the ones we're going to be using.
		List<Double> someVals=vals.subList(0, numPoints);

		int pos=numPoints-1;
		for(Double reading : someVals) {
			float val=reading.floatValue();
			// Calculate x point
			xpoints[pos]=x+pos;
			// Calculate y point
			float heightPercent=(float)(val-low)/(float)(high-low);
			ypoints[pos]=(y+h) - (int)(h * heightPercent);
			if(ypoints[pos] > y+h) {
				log("y point " + ypoints[pos] + " exceeded maximum value "
					+ (y+h));
				ypoints[pos]=y+h;
			}
			if(ypoints[pos] < y) {
				log("y point " + ypoints[pos] + " fell below minimum value "
					+ y);
				ypoints[pos]=y;
			}
			// log("y value for " + xpoints[pos] + " is " + ypoints[pos]);
			// One less point we gotta worry about
			pos--;
		}

		// log("Drawing " + numPoints + " points");
		g.drawPolyline(xpoints, ypoints, numPoints);
	}

	// Draw a sparkline within the room box
	private void drawSparkline(Graphics g, String which,
		int x, int y, int w, int h) {

		// Default spark y is 20 more than the configured label location
		int sparkw=houseConfig.getInt(which + ".spark.w", w);
		int sparkh=houseConfig.getInt(which + ".spark.h", 20);
		int sparkx=houseConfig.getInt(which + ".spark.x", x);
		int sparky=houseConfig.getInt(which + ".spark.y", (y+h)-sparkh);

		// Get the readings and convert them to plain Doubles
		List<Sample> sampleList=Temperature.gatherer.getHistory(which);
		double high=Double.MIN_VALUE;
		double low=Double.MAX_VALUE;
		List<Double> readings=new ArrayList<Double>(sampleList.size());
		for(Sample s : sampleList) {
			Double reading=s.getSample();
			double val=reading.doubleValue();
			if(val > high) {
				high=val;
			}
			if(val < low) {
				low=val;
			}
			readings.add(reading);
		}
		// If I have at least two points, plot them.
		if(readings.size() > 1) {
			g.setColor(Color.GRAY);
			// Make sure there's at least a half a degree for each pixel
			if((high-low) < sparkh) {
				double avg=high-((high-low)/2);
				low=avg+(sparkh/2);
				high=avg-(sparkh/2);
			}
			drawSparks(g, low, high, sparkx, sparky, sparkw, sparkh, readings);
		}
	}

	// Graphical representation of the image.
	private Image getHouseImage() throws Exception {
		Image img=createImage(
			houseConfig.getInt("house.w", 272),
			houseConfig.getInt("house.h", 193));
		Graphics g=img.getGraphics();
		g.drawImage(baseImage, 0, 0, new StupidImageObserver());

		// Find all the things we need to colorize
		String things[]=SpyUtil.split(" ", houseConfig.get("colorize", ""));
		for(String which : things) {
			int x, y, w, h;

			String rstring=null;

			x=houseConfig.getInt(which + ".rect.x", 0);
			y=houseConfig.getInt(which + ".rect.y", 0);
			w=houseConfig.getInt(which + ".rect.w", 0);
			h=houseConfig.getInt(which + ".rect.h", 0);

			// Draw a black border around a white box
			g.setColor(Color.BLACK);
			g.fillRect(x-1, y-1, w+2, h+2);
			g.setColor(Color.WHITE);
			g.fillRect(x, y, w, h);

			try {
				Double dreading=Temperature.gatherer.getSeen(which);
				double reading=dreading.doubleValue();
				rstring="" + reading;

				// Set the color based on the temperature reading.
				fillGradient(which, g, reading, x, y, w, h);
			} catch(Exception e) {
				e.printStackTrace();
				rstring="??.??";
			}
			// Draw the reading label
			drawLabel(g, which, rstring, x, y, w, h);
			drawSparkline(g, which, x, y, w, h);
		}

		return(img);
	}

	private void getBaseImage(URL url) throws IOException {
		ImageLoader il=new ImageLoader(url);
		baseImage=il.getImage();
	}
}
