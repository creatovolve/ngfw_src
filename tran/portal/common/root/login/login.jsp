<%@ page language="java" %>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">

<html xmlns="http://www.w3.org/1999/xhtml">

<head>
<title>Metavize Portal Login</title>
</head>

<img src="<%=request.getContextPath()%>/login/Dolphin-m.jpg">

<body>
<form method="POST" action="j_security_check">
<input type="text" name="j_username">
<input type="password" name="j_password">
<input type="submit" value="login">
</form>
</body>