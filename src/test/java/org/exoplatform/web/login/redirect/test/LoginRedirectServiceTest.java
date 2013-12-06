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
package org.exoplatform.web.login.redirect.test;

import java.util.Collection;
import java.util.HashSet;

import org.exoplatform.component.test.AbstractKernelTest;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.services.security.MembershipEntry;
import org.exoplatform.web.login.redirect.LoginRedirectService;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 * @version $Revision$
 */
@ConfiguredBy({@ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/redirect-configuration.xml")})
public class LoginRedirectServiceTest extends AbstractKernelTest
{
   private IdentityRegistry identityRegistry;
   private LoginRedirectService loginRedirectService;
   
   public void setUp() throws Exception
   {
      super.setUp();
      begin();
      PortalContainer container = getContainer();
      identityRegistry = (IdentityRegistry)container.getComponentInstanceOfType(IdentityRegistry.class);
      loginRedirectService = (LoginRedirectService)container.getComponentInstanceOfType(LoginRedirectService.class);
      
      Collection<MembershipEntry> rootMemberships = new HashSet<MembershipEntry>();
      rootMemberships.add(MembershipEntry.parse("member:/organization/management/executive-board"));
      rootMemberships.add(MembershipEntry.parse("manager:/platform/administrators"));
      rootMemberships.add(MembershipEntry.parse("member:/platform/users"));
      Identity root = new Identity("root", rootMemberships);
      
      Collection<MembershipEntry> johnMemberships = new HashSet<MembershipEntry>();
      johnMemberships.add(MembershipEntry.parse("manager:/organization/management/executive-board"));
      johnMemberships.add(MembershipEntry.parse("member:/platform/administrators"));
      johnMemberships.add(MembershipEntry.parse("member:/platform/users"));
      Identity john = new Identity("john", johnMemberships);      
      
      Collection<MembershipEntry> maryMemberships = new HashSet<MembershipEntry>();
      maryMemberships.add(MembershipEntry.parse("member:/platform/guests"));
      Identity mary = new Identity("mary", maryMemberships);
      
      identityRegistry.register(root);
      identityRegistry.register(john);
      identityRegistry.register(mary);
   }
   
   protected void tearDown() throws Exception
   {      
      end();
      super.tearDown();
   }
   
   public void testLoginRedirectLocation()
   {
      assertEquals(loginRedirectService.getLoginRedirectURL("root"), "/portal/private/classic/administration/registry");
      assertEquals(loginRedirectService.getLoginRedirectURL("john"), "/portal/private/classic/organization/management");
      assertEquals(loginRedirectService.getLoginRedirectURL("mary"), "/portal/private/classic/defaultPage");
   }
   
   public void testLogoutRedirectLocation()
   {
      assertEquals(loginRedirectService.getLogoutRedirectURL("root"), "/portal/public/classic/");
      assertEquals(loginRedirectService.getLogoutRedirectURL("john"), "/portal/public/classic/sitemap");
      assertEquals(loginRedirectService.getLogoutRedirectURL("mary"), "/portal/public/classic/defaultPage");      
   }

}

