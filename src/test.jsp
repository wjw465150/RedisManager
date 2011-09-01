<%@ page contentType="text/html; charset=GBK" %>
<html>
<head>
  <title>
      测试Httprequest属性
  </title>
</head>
<body bgcolor="#ffffff">
<h1>Test JSP 1</h1>
<%
  session.setMaxInactiveInterval(60);
  String sessionId = session.getId();
  String ss = "pageContext属性1";
  pageContext.setAttribute("ss", ss);
  pageContext.setAttribute("sessionId", sessionId);

  if(session.getAttribute("att1")==null) {
    session.setAttribute("att1","session_att1");
  }
  if(session.getAttribute("att2")==null) {
    session.setAttribute("att2","session_att2(第2个属性)");
  }
  if(session.getAttribute("at t3")==null) {
    java.util.Map map = new java.util.HashMap();
    map.put("1","王俊伟");
    map.put("二","王白石");
    session.setAttribute("at t3",map);
  }
%>
sessionId:${sessionId}<br><br>
pageContext属性:ss:${ss}<br>
session("att1"):<%= session.getAttribute("att1") %><br>
session("att2"):<%= session.getAttribute("att2") %><br>
session("at t3"):<%= session.getAttribute("at t3") %><br>

request属性:<br>
getAuthType( ):<%= request.getAuthType() %><br>
getProtocol( ):<%= request.getProtocol() %><br>
getMethod( ):<%= request.getMethod() %><br>
getScheme( ):<%= request.getScheme() %><br>
getContentType( ):<%= request.getContentType() %><br>
getContentLength( ):<%= request.getContentLength() %><br>
getCharacterEncoding( ):<%= request.getCharacterEncoding() %><br>
getRequestedSessionId( ):<%= request.getRequestedSessionId() %><br><br>

getContextPath( ):<%= request.getContextPath() %><br>
getServletPath( ):<%= request.getServletPath() %><br>
getPathInfo( ):<%= request.getPathInfo() %><br>
getRequestURI( ):<%= request.getRequestURI() %><br>
getRequestURL( ):<%= request.getRequestURL() %><br>
getQueryString( ):<%= request.getQueryString() %><br><br>

getRemoteAddr( ):<%= request.getRemoteAddr() %><br>
getRemoteHost( ):<%= request.getRemoteHost() %><br>
getRemoteUser( ):<%= request.getRemoteUser() %><br>
getRemotePort( ):<%= request.getRemotePort() %><br>
getServerName( ):<%= request.getServerName() %><br>
getServerPort( ):<%= request.getServerPort() %><br>

ServletContext.getRealPath("/"):<%= pageContext.getServletContext().getRealPath("/") %><br>
</body>
</html>
