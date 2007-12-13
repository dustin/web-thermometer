// Copyright (c) 2000  Dustin Sallings <dustin@spy.net>
//
// $Id: ReportBean.java,v 1.4 2003/01/21 06:28:22 dustin Exp $

package net.spy.temperature;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.spy.temperature.sp.report.AvgHour;
import net.spy.temperature.sp.report.Histogram;
import net.spy.temperature.sp.report.MaxHour;
import net.spy.temperature.sp.report.MinHour;
import net.spy.temperature.sp.report.ReportQuery;
import net.spy.temperature.sp.report.Vitality;

/**
 * Bean used for Temperature reports.
 */
public class ReportBean extends Object implements java.io.Serializable {

	// Report types
	/**
	 * Histogram report.
	 */
	public final static int HISTOGRAM=1;
	/**
	 * Hourly averages
	 */
	public final static int AVG_HOUR=2;
	/**
	 * Hourly minimums.
	 */
	public final static int MIN_HOUR=3;
	/**
	 * Hourly maximums
	 */
	public final static int MAX_HOUR=4;
	/**
	 * Hourly stuff (min, avg, and max)
	 */
	public final static int HOURLY_VITALITY=5;

	// The start and stop dates for our report
	private String start_date=null;
	private String stop_date=null;
	private Sensor sensor=null;
	private int report_num=-1;

	// Results go here
	private List<ArrayList<String>> results=null;
	private List<String> columns=null;

	public ReportBean() {
		super();
	}

	/**
	 * Returns a list of Sensor objects describing the known sensors.
	 */
	public Collection<Sensor> getSensors() throws Exception {
		return(Sensor.getSensors());
	}

	/**
	 * Set the start date for the report.
	 */
	public void setStartdate(String to) {
		this.start_date=to;
	}

	/**
	 * Set the stop date for the report.
	 */
	public void setStopdate(String to) {
		this.stop_date=to;
	}

	/**
	 * Set the sensor number we're reporting on.
	 *
	 * @exception Exception when something breaks.
	 */
	public void setSensor(int sensor_id) throws Exception {
		sensor=new Sensor(sensor_id);
	}

	/**
	 * Set the report number.
	 *
	 * @exception Exception if the report number is invalid
	 */
	public void setReport(int report_id) throws Exception {
		if(invalidReportNum(report_id)) {
			throw new Exception("Invalid report number:  " + report_id);
		}

		this.report_num=report_id;
	}

	/**
	 * Get the report results.
	 */
	public Collection<ArrayList<String>> getResults() throws Exception {
		if(results==null) {
			throw new Exception("No results exist!");
		}
		return(results);
	}

	/**
	 * Get the report result column names.
	 */
	public Collection<String> getResultColumns() throws Exception {
		if(columns==null) {
			throw new Exception("No results exist!");
		}
		return(columns);
	}

	// Is the report number valid?
	protected boolean invalidReportNum(int num) {
		return(num < 1 || num > 5);
	}

	/**
	 * Run the report.
	 *
	 * @exception Exception for various reasons
	 */
	public void runReport() throws Exception {

		if(
			start_date==null ||
			start_date==null ||
			sensor==null ||
			invalidReportNum(report_num) ) {
			throw new Exception("Missing fields.");
		}

		doReport();
	}

	// This really does the report, now that we know we have ok-ish data
	protected void doReport() throws Exception {

		ReportQuery db=null;
		TempConf conf=TempConf.getInstance();

		// Get the report's SQL
		switch(report_num) {
			case HISTOGRAM:
				db=new Histogram(conf);
				break;

			case MAX_HOUR:
				db=new MaxHour(conf);
				break;

			case AVG_HOUR:
				db=new AvgHour(conf);
				break;

			case MIN_HOUR:
				db=new MinHour(conf);
				break;

			case HOURLY_VITALITY:
				db=new Vitality(conf);
				break;

			default:
				throw new Exception("Impossible!  No such report");
		}

		db.setFromDate(start_date);
		db.setToDate(stop_date);
		db.setSensorId(sensor.getSensorID());

		ResultSet rs=db.executeQuery();
		ResultSetMetaData rmd=rs.getMetaData();

		// This is where the results go.
		results=new ArrayList<ArrayList<String>>(128);
		columns=new ArrayList<String>(128);

		// Figure out how many columns we've got
		int cols=rmd.getColumnCount();

		// Get the column names.
		for(int i=1; i<=cols; i++) {
			columns.add(rmd.getColumnName(i));
		}

		// Get the results
		while(rs.next()) {
			ArrayList<String> a=new ArrayList<String>();
			for(int i=1; i<=cols; i++) {
				a.add(rs.getString(i));
			}
			results.add(a);
		}

		rs.close();
		db.close();
	}
}
