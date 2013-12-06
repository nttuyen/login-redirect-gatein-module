/******************************************************************************
 * JBoss, a division of Red Hat                                               *
 * Copyright 2011, Red Hat Middleware, LLC, and individual                    *
 * contributors as indicated by the @authors tag. See the                     *
 * copyright.txt in the distribution for a full listing of                    *
 * individual contributors.                                                   *
 *                                                                            *
 * This is free software; you can redistribute it and/or modify it            *
 * under the terms of the GNU Lesser General Public License as                *
 * published by the Free Software Foundation; either version 2.1 of           *
 * the License, or (at your option) any later version.                        *
 *                                                                            *
 * This software is distributed in the hope that it will be useful,           *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of             *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU           *
 * Lesser General Public License for more details.                            *
 *                                                                            *
 * You should have received a copy of the GNU Lesser General Public           *
 * License along with this software; if not, write to the Free                *
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA         *
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.                   *
 ******************************************************************************/
package org.exoplatform.web.login.redirect;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.exoplatform.web.login.InitiateLoginServlet;
import org.exoplatform.web.security.security.AbstractTokenService;

import org.exoplatform.container.web.AbstractFilter;
import org.exoplatform.web.security.security.CookieTokenService;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;
import org.gatein.wci.impl.DefaultServletContainerFactory;

/**
 * Filter can be used to redirect users from different groups to different pages after login or logout of particular user.<br />
 * Filter is able to detect login or logout of particular user and redirect him to particular page. The decision about exact page to 
 * redirect is performed by LoginRedirectService, where should be exact mapping from user groups to login/logout pages.  
 * 
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 * @version $Revision$
 */
public class LoginRedirectFilter extends AbstractFilter
{
   private static final Logger log = LoggerFactory.getLogger(LoginRedirectFilter.class);

   // We need this helper registry because userId can't be obtained from HTTP session when logout is detected because session is already invalidated.
   // Key is sessionId, Value is userId (HTTP session is invalidated during logout and all attributes are cleaned but session ID is still the same)
   private ConcurrentMap<String, String> loggedUsersRegistry = new ConcurrentHashMap<String, String>();

   public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
         ServletException
   {      
      HttpServletRequest httpRequest = (HttpServletRequest) request;
      HttpServletResponse httpResponse = (HttpServletResponse) response;

      String newUserName = httpRequest.getRemoteUser();
      String oldUserName = loggedUsersRegistry.get(httpRequest.getSession().getId());

      if ((newUserName != null) && ((oldUserName == null) || (!oldUserName.equals(newUserName))))
      {
         log.debug("Detected login of user " + newUserName);

         // save new user into our local map.
         loggedUsersRegistry.put(httpRequest.getSession().getId(), newUserName);

         // Get location of user according to his roles.
         String location = getLocationAfterLogin(newUserName);

         // Redirect user to his new location if location is not null. Otherwise ignore redirection.
         if (location != null)
         {
            location = httpResponse.encodeRedirectURL(location);
            httpResponse.sendRedirect(location);
            return;
         }
      }
      else if (isLogoutRequest(httpRequest))
      {
         log.debug("Detected logout request of user " + oldUserName);
         String sessionId = httpRequest.getSession().getId();
         
         // Remove attribute from local map.
         loggedUsersRegistry.remove(sessionId);

         // TODO: find better way to fix it than logout user directly from filter
         logout(httpRequest, httpResponse);

         // Redirect user to his new location after logout if location is not null. Otherwise ignore redirection.
         String location = getLocationAfterLogout(oldUserName);
         location = httpResponse.encodeRedirectURL(location);
         httpResponse.sendRedirect(location);
         return;
      }

      // continue with filter chain for case that login or logout detection did not happen during this HTTP request
      chain.doFilter(request, response);
   }

   // Return true if logout request is in progress
   private boolean isLogoutRequest(HttpServletRequest req)
   {
      String portalComponentId = req.getParameter("portal:componentId");
      String portalAction = req.getParameter("portal:action");
      if (("UIPortal".equals(portalComponentId)) && ("Logout".equals(portalAction)))
      {
         return true;
      }
      else
      {
         return false;
      }
   }

   /**
    * Compute location for logged user according to his roles. Filter will then redirect user to new location.
    * 
    * @param loggedUser
    * @return new location where logged user will be redirected
    */
   private String getLocationAfterLogin(String loggedUser)
   {
      LoginRedirectService loginService = (LoginRedirectService)getContainer().getComponentInstanceOfType(LoginRedirectService.class);
      return loginService.getLoginRedirectURL(loggedUser);
   }

   /**
    * Compute location for logged outed user according to his roles. Filter will then redirect user to new location.
    * 
    * @param loggedUser
    * @return new location where logged user will be redirected
    */
   private String getLocationAfterLogout(String loggedUser)
   {
      LoginRedirectService loginService = (LoginRedirectService)getContainer().getComponentInstanceOfType(LoginRedirectService.class);
      return loginService.getLogoutRedirectURL(loggedUser);      
   }

   public void destroy()
   {
   }
   
   private void logout(HttpServletRequest request, HttpServletResponse response) throws ServletException
   {
      String token = getTokenCookie(request);
      if(token != null)
      {
         AbstractTokenService tokenService = AbstractTokenService.getInstance(CookieTokenService.class);
         tokenService.deleteToken(token);
      }

      DefaultServletContainerFactory.getInstance().getServletContainer().logout(request, response);

   }

   private String getTokenCookie(HttpServletRequest req)
   {
      Cookie[] cookies = req.getCookies();
      if (cookies != null)
      {
         for (Cookie cookie : cookies)
         {
            if (InitiateLoginServlet.COOKIE_NAME.equals(cookie.getName()))
            {
               return cookie.getValue();
            }
         }
      }
      return null;
   }
   
   

}
