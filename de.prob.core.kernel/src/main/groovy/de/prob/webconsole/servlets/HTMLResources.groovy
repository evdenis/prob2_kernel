package de.prob.webconsole.servlets

class HTMLResources {

	def static String getPredicateHTML(String sessionId) {
		return getHTML(sessionId,"Predicate Visualization","predicate.css","predicate.js")
	}

	def static String getValueVsTimeHTML(String sessionId) {
		return getHTML(sessionId,"Value vss Time","valueOverTime.css","oszilloscope.js")
	}

	/**
	 * @param sessionId (Generated by the servlet)
	 * @param stylesheet ( "MyStyleSheet.css" that is saved in folder stylesheets/visualizations )
	 * @param javascript ( "MyJavascript.js" that is saved in folder javascripts )
	 * @return String representation of the HTML for the page
	 */
	def static String getHTML(String sessionId, String title, String stylesheet, String javascript) {
		return '''<!DOCTYPE html>
<!--[if lt IE 7 ]><html class="ie ie6" lang="en"> <![endif]-->
<!--[if IE 7 ]><html class="ie ie7" lang="en"> <![endif]-->
<!--[if IE 8 ]><html class="ie ie8" lang="en"> <![endif]-->
<!--[if (gte IE 9)|!(IE)]><!-->
<html lang="en">
<!--<![endif]-->
<head>
<!-- Basic Page Needs
    ================================================== -->
<meta charset="utf-8">
<title>'''+title+'''</title>
<meta name="description" content="">
<meta name="author" content="Joy Clark">

<!-- Mobile Specific Metas
    ================================================== -->
<meta name="viewport"
	content="width=device-width, initial-scale=1, maximum-scale=1">

<!-- CSS
    ================================================== -->
<link rel="stylesheet" href="../stylesheets/skeleton.css">
<link rel="stylesheet" href="../stylesheets/layout.css">
<link rel="stylesheet" href="../stylesheets/evalb.css">
<link rel="stylesheet" href="../stylesheets/pepper.css">
<link rel="stylesheet" href="../stylesheets/visualizations/'''+stylesheet+'''"
</head>
<body onload="initialize('''+"'"+sessionId+"'"+''')">

			<div id="body"></div>

			<!-- JS
			  ================================================== -->
			<!-- <script src="http://code.jquery.com/jquery-1.7.1.min.js"></script> -->
			<script src="../javascripts/jquery-1.7.2.min.js"></script>
			<script src="../javascripts/d3.v2.min.js"></script>
			<script src="../javascripts/'''+javascript+'''"></script>

			<!-- End Document
		  ================================================== -->
		</body>
		</html>'''
	}
}
