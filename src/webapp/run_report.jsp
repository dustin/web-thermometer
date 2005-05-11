<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>
<jsp:useBean id="treport" scope="session"
	class="net.spy.temperature.ReportBean" />

<jsp:setProperty name="treport" property="*" />

<% treport.runReport(); %>

<html><head><title>Temperature Report</title></head>

<body bgcolor="#fFfFfF">

<table border="1">
	<tr>
		<c:forEach var="col" items="${treport.resultColumns}">
			<th><c:out value="${col}"/></th>
		</c:forEach>
	</tr>
	
	<c:forEach var="row" items="${treport.results}">
		<tr>
			<c:forEach var="col" items="${row}">
				<td><c:out value="${col}"/></td>
			</c:forEach>
		</tr>
	</c:forEach>
</table>

</body>
</html>
