<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>
<jsp:useBean id="treport" scope="session"
	class="net.spy.temperature.ReportBean" />

<html><head><title>Temperature Reports</title></head>

<body bgcolor="#fFfFfF">

<form method="POST" action="run_report.jsp">

<h1>Temperature Report Generator</h1>

<table>
<tr>
	<td>Start Date:</td><td><input name="startdate" value="yesterday"></td>
<tr>
</tr>
	<td>Stop Date:</td><td><input name="stopdate" value="today"></td>
</tr>

<tr>
	<td>Report</td>
	<td>
		<select name="report">
			<option value="1">Histogram
			<option value="2">Avg by hour
			<option value="3">Min by hour
			<option value="4">Max by hour
			<option value="5">Hourly Min,Avg,Max
		</select>
	</td>
<tr>

<tr>
	<td>Sensor</td>

	<td>
		<select name="sensor">
			<c:forEach var="s" items="${treport.sensors}">
				<option value="<c:out value='${s.sensorID}'/>">
					<c:out value='${s.name}'/>
				</option>
			</c:forEach>
		</select>
	</td>
</tr>

</table>

<br>

<input type="submit" value="Go">

</form>

</body>
</html>
