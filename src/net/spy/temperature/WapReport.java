// Copyright (c) 2004  Dustin Sallings <dustin@spy.net>

package net.spy.temperature;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;

import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.spy.jwebkit.JWHttpServlet;

import net.spy.temperature.sp.SummaryByDay;

/**
 * Servlet for displaying reports via WML.
 */
public class WapReport extends JWHttpServlet {

	/**
	 * Get an instance of WapReport.
	 */
	public WapReport() {
		super();
	}

	private String getWml() throws ServletException {
		Map stats=new HashMap();
		Map nameMap=new HashMap();

		try {
			SummaryByDay sbd=new SummaryByDay(new TempConf());
			ResultSet rs=sbd.executeQuery();
			while(rs.next()) {
				Stat s=new Stat(rs);
				List l=(List)stats.get(s.sn);
				if(l == null) {
					l=new ArrayList();
					stats.put(s.sn, l);
				}
				l.add(s);
				nameMap.put(s.sn, s.name);
			}
			rs.close();
			sbd.close();
		} catch(SQLException e) {
			throw new ServletException("Problem getting wml", e);
		}

		StringBuffer sb=new StringBuffer(8192);
		sb.append("<?xml version=\"1.0\"?>\n");
		sb.append("<!DOCTYPE wml PUBLIC ");
		sb.append("\"-//WAPFORUM//DTD WML 1.1//EN\" ");
		sb.append("\"http://www.wapforum.org/DTD/wml_1.1.xml\">\n");
		sb.append("<wml>\n");
		sb.append("<card id=\"home\" title=\"Therms\">\n<p>\n");

		SimpleDateFormat sdf=new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");
		sb.append(sdf.format(new java.util.Date()) + "<br/>\n");

		for(Iterator i=nameMap.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry me=(Map.Entry)i.next();
			sb.append("<a href=\"#s" + me.getKey() + "\">"
				+ me.getValue() + "</a><br/>\n");
		}

		sb.append("</p>\n</card>\n");

		MessageFormat mf=new MessageFormat("{0,number,#.#} "
			+ "{1,number,#.#} "
			+ "{2,number,#.#} "
			+ "{3,number,#.#}<br/>");

		for(Iterator i=stats.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry me=(Map.Entry)i.next();
			sb.append("<card id=\"s" + me.getKey() + "\" title=\""
				+ nameMap.get(me.getKey())
				+ "\">\n<p>min/avg/max/stddev<br/>\n");

			for(Iterator i2=((List)me.getValue()).iterator(); i2.hasNext(); ) {
				Stat s=(Stat)i2.next();
				sb.append("<b>" + s.day + "</b><br/>\n");
				Object args[]={new Float(s.min), new Float(s.avg),
					new Float(s.max), new Float(s.stddev)};
				sb.append(mf.format(args));
				sb.append("<br/>\n");
			}

			sb.append("</p></card>\n");
		}

		sb.append("</wml>\n");
		return(sb.toString());
	}

	/** 
	 * Process this request.
	 */
	protected void doGetOrPost(HttpServletRequest req,
		HttpServletResponse res) throws ServletException, IOException {

		res.setContentType("text/vnd.wap.wml");
		sendSimple(getWml(), res);
	}

	private static class Stat extends Object {
		public String sn=null;
		public String name=null;
		public String day=null;
		public float min=0.0f;
		public float avg=0.0f;
		public float stddev=0.0f;
		public float max=0.0f;

		public Stat(ResultSet rs) throws SQLException {
			super();
			sn=rs.getString("serial_num");
			name=rs.getString("name");
			day=rs.getString("day");
			min=rs.getFloat("min_temp");
			avg=rs.getFloat("avg_temp");
			stddev=rs.getFloat("stddev_temp");
			max=rs.getFloat("max_temp");
		}
	}

}
