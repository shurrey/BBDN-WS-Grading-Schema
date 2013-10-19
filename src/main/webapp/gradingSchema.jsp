<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<%@ page import="blackboard.platform.context.*,
				 blackboard.data.course.*,
				 blackboard.data.user.*,
				 java.util.*,
				 bbdn.wsapi.gradebook.GradingSchema" %>

<%@ taglib uri="/bbNG" prefix="bbNG" %>

<bbNG:genericPage title="Get Gradebook Schema" ctxId="ctx" >
	<bbNG:pageHeader>
		<bbNG:breadcrumbBar environment="CTRL_PANEL">
			<bbNG:breadcrumb>Get Gradebook Schema</bbNG:breadcrumb>
		</bbNG:breadcrumbBar>
		<bbNG:pageTitleBar>Get Gradebook Schema</bbNG:pageTitleBar>
	</bbNG:pageHeader>
	
	<%
		GradingSchema schema = new GradingSchema( ctx.getCourseId() );
	%>
	<%=schema.doGradebookSchema()%>
	
</bbNG:genericPage>