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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.services.security.MembershipEntry;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;

/**
 * This service contains mapping of groups and login/logout pages, so that it can decide where to redirect user
 * after his login or after his logout. 
 * 
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 * @version $Revision$
 */
public class LoginRedirectService
{
   private static final Logger log = LoggerFactory.getLogger(LoginRedirectService.class);
   
   private final String defaultLoginURL;
   private final String defaultLogoutURL;
   private final Map<MembershipEntry, MappingParam> mappingConfiguration;
   private final IdentityRegistry identityRegistry;
   
   // We need this map because Identity of particular user can't be obtained from IdentityRegistry when logout is detected.
   // Key is username and value is his identity.
   private ConcurrentMap<String, Identity> localIdentityRegistry = new ConcurrentHashMap<String, Identity>();
   
   public LoginRedirectService(InitParams params, IdentityRegistry identityRegistry)
   {
      this.defaultLoginURL = params.getValueParam("defaultLoginURL").getValue();
      this.defaultLogoutURL = params.getValueParam("defaultLogoutURL").getValue();
      
      RedirectMappingConfig config = (RedirectMappingConfig)params.getObjectParam("redirectMappings").getObject();
      Map<MembershipEntry, MappingParam> mappingConfigurationPom = new LinkedHashMap<MembershipEntry, MappingParam>();
      
      for (MappingParam mappingParam : config.getRedirectMappings())
      {
         MembershipEntry membership = MembershipEntry.parse(mappingParam.getGroupName());
         mappingConfigurationPom.put(membership, mappingParam);
      }
      
      mappingConfiguration = Collections.unmodifiableMap(mappingConfigurationPom); 
      this.identityRegistry = identityRegistry;
   }
   
   /**
    * Return the page where particular user should be redirected after his login.
    * 
    * @param username
    * @return page to redirect
    */
   public String getLoginRedirectURL(String username)
   {
      Identity identity = identityRegistry.getIdentity(username);
      if (identity == null)
      {
         log.warn("Can't find identity for user " + username + " from identity registry. Custom redirect afer login will be disabled.");
         return null;
      }
      
      // store Identity to local map for later use
      localIdentityRegistry.put(username, identity);
      
      for (MembershipEntry membership : mappingConfiguration.keySet())
      {         
         if (identity.isMemberOf(membership))
         {
            String resultURL = mappingConfiguration.get(membership).getLoginURL();
            
            if (log.isDebugEnabled())
            {
               log.debug("Redirecting user from group " + membership + " to page " + resultURL + ".");
            }
            return resultURL;
         }
      }               
      
      if (log.isDebugEnabled())
      {
         log.debug("Redirecting user " + username + " to default page " + defaultLoginURL);
      }
      return defaultLoginURL;              
   }
   
   /**
    * Return the page where particular user should be redirected after his logout.
    * 
    * @param username
    * @return page to redirect
    */
   public String getLogoutRedirectURL(String username)
   {
      Identity identity = localIdentityRegistry.get(username);

      // Try portal identityRegistry for case it's not in local	
      if (identity == null)
      {
         identity = identityRegistry.getIdentity(username);
      }  

      if (identity == null)
      {
         log.warn("Can't find identity for user " + username + " in local registry.");
         return defaultLogoutURL;
      }      
      
      for (MembershipEntry membership : mappingConfiguration.keySet())
      {         
         if (identity.isMemberOf(membership))
         {
            String resultURL = mappingConfiguration.get(membership).getLogoutURL();
            
            if (log.isDebugEnabled())
            {
               log.debug("Redirecting user from group " + membership + " to page " + resultURL + ".");
            }
            return resultURL;
         }
      }               
      
      if (log.isDebugEnabled())
      {
         log.debug("Redirecting user " + username + " to default page " + defaultLogoutURL);
      }
      return defaultLogoutURL;             
   }     

}

