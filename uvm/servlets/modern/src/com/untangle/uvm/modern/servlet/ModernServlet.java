/**
 * $Id$
 */
package com.untangle.uvm.modern.servlet;

import java.io.IOException;
import java.util.Random;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.untangle.uvm.UvmContextFactory;

/**
 * A servlet which will display the admin page
 *
 */
@SuppressWarnings("serial")
public class ModernServlet extends HttpServlet
{
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException
    {
        String url="/WEB-INF/jsp/index.jsp";
        ServletContext sc = getServletContext();
        RequestDispatcher rd = sc.getRequestDispatcher(url);
        req.setAttribute( "companyName", UvmContextFactory.context().brandingManager().getCompanyName() );
        req.setAttribute( "extjsTheme", UvmContextFactory.context().skinManager().getSkinInfo().getExtjsTheme() );
        req.setAttribute( "buildStamp", getServletConfig().getInitParameter("buildStamp") );
        rd.forward(req, resp);
    }

}
