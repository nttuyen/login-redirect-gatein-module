How to redirect a user to certain page on login/logout based on his role
------------------------------------------------------------------------------------------

At this moment, this feature is not provided in GateIn out of the box. In current GateIn, redirection after login is working this way:
- When user signs through click to "Sign in" button, he is always redirected to default page of default portal after his login ( /portal/classic/ )
- When user put private URL into browser, he is redirected to login screen and then he is redirected to the requested page after login. For example:
	- user put http://localhost:8080/portal/classic/sitemap into the browser and he is redirected to login screen
	- After login, user is redirected to the requested page http://localhost:8080/portal/classic/sitemap . (This redirections is based on value of initialURL parameter, which is send from login screen with parameters for username and password.)


So here is sample project for this functionality . It can be configured to which page should be users from particular group redirected after login and which page they should be redirected after logout.
Main part of application is HTTP filter LoginRedirectFilter. Filter is able to detect login or logout of particular user. After this detection, filter is using LoginRedirectService, which is component that needs to be bind into eXo kernel for correct usage. Service is able to return correct URL for redirect based on groups of particular user. Filter then sends another HTTP request, which redirects user to requested page.
Application can be deployed into GateIn portal or EPP without need to change any other existing code on portal side. We can simply comment or uncomment particular HTTP filter and mapping in portal web.xml if we want to disable or enable this feature.

How to build and deploy the application:

1) Plugin can be compiled simply by command:

mvn clean install

2) File target/exo.portal.redirectplugin-3.2.0-SNAPSHOT.jar  needs to be added to GateIn portal into $GATEIN_HOME/server/default/deploy/gatein.ear/lib/
This JAR contains needed java classes, especially LoginRedirectFilter and LoginRedirectService.

4) Another step is to configure HTTP filter in portal web.xml . This can be done by adding Filter definition and Filter mapping into particular sections of file $GATEIN_HOME/server/default/deploy/gatein.ear/02portal.war/WEB-INF/web.xml
Assumption is that "filter" is inserted after all other filter definitions: 

   <filter>
      <filter-name>LoginRedirectFilter</filter-name>
      <filter-class>org.exoplatform.web.login.redirect.LoginRedirectFilter</filter-class>
   </filter>

And another assumption is that "filter-mapping" is inserted after all other filter-mapping definitions: 

   <filter-mapping>
      <filter-name>LoginRedirectFilter</filter-name>
      <url-pattern>/*</url-pattern>
   </filter-mapping>

5) LoginRedirectFilter is using LoginRedirectService to decide which is correct URL to redirect for users of particular group. This service needs to be configured in some of portal configuration files (for example in $GATEIN_HOME/server/default/deploy/gatein.ear/02portal.war/WEB-INF/conf/common/common-configuration.xml ). Example configuration is below. We can see that each mapping parameter has 3 nested parameters:
- groupName - name of group and membership
- loginURL  - URL to redirect after login of user from particular group is detected
- logoutURL - URL to redirect after logout of user from particular group is detected

Order of mapping elements is important because if more mappings are suitable for particular user, then algorithm will choose first found mapping. 
For example: User "john" is suitable in both groups "manager:/organization/management/executive-board" and "*:/platform/administrators" , so first mapping is chosen for him and he is redirected to "/portal/g/:organization:management:executive-board/organization/management" after his login.

There are another 2 parameters defaultLoginURL and defaultLogoutURL, which contains default URL to redirect if no suitable group mapping is found for our user. 

So example of mapping is here:

  <component>
        <key>org.exoplatform.web.login.redirect.LoginRedirectService</key>
        <type>org.exoplatform.web.login.redirect.LoginRedirectService</type>
        <init-params>
            <value-param>
                <name>defaultLoginURL</name>
                <description>login URL to redirect if group mapping can't be found for particular user</description>
                <value>/portal/classic/home</value>
            </value-param>
            <value-param>
                <name>defaultLogoutURL</name>
                <description>logout URL to redirect if group mapping can't be found for particular user</description>
                <value>/portal/classic/sitemap</value>
            </value-param>
            <object-param>
                <name>redirectMappings</name>
                <description>Mappings of groups and pages where should be
                    all users from particular group redirected.</description>
                <object type="org.exoplatform.web.login.redirect.RedirectMappingConfig">
                    <field name="redirectMappings">
                        <collection type="java.util.ArrayList"
                            item-type="org.exoplatform.web.login.redirect.MappingParam">
                            <value>
                                <object type="org.exoplatform.web.login.redirect.MappingParam">
                                    <field name="groupName">
                                        <string>manager:/organization/management/executive-board</string>
                                    </field>
                                    <field name="loginURL">
                                        <string>/portal/g/:organization:management:executive-board/organization/management</string>
                                    </field>
                                    <field name="logoutURL">
                                        <string>/portal/classic/sitemap</string>
                                    </field>
                                </object>
                            </value>
                            <value>
                                <object type="org.exoplatform.web.login.redirect.MappingParam">
                                    <field name="groupName">
                                        <string>*:/platform/administrators</string>
                                    </field>
                                    <field name="loginURL">
                                        <string>/portal/g/:platform:administrators/administration/registry</string>
                                    </field>
                                    <field name="logoutURL">
                                        <string>/portal/classic/home</string>
                                    </field>
                                </object>
                            </value>
                            <value>
                                <object type="org.exoplatform.web.login.redirect.MappingParam">
                                    <field name="groupName">
                                        <string>*:/platform/users</string>
                                    </field>
                                    <field name="loginURL">
                                        <string>/portal/classic/sitemap</string>
                                    </field>
                                    <field name="logoutURL">
                                        <string>/portal/classic/sitemap</string>
                                    </field>
                                </object>
                            </value>
                        </collection>
                    </field>
                </object>
            </object-param>
        </init-params>
  </component>
  
6) So after update of mapping configuration in common-configuration.xml and filter configuration in web.xml, we can start the portal.
We can try predefined users and we will see that:

- User "john" is member of first suitable group "manager:/organization/management/executive-board" . 
  So after his login, he will be redirected to "http://localhost:8080/portal/g/:organization:management:executive-board/organization/management" and after logout to "http://localhost:8080/portal/classic/sitemap" .
- User "root" is not in "manager:/organization/management/executive-board" but he meets second group "*:/platform/administrators". 
  So after login, he will be redirected to "http://localhost:8080/portal/g/:platform:administrators/administration/registry" and after logout to "http://localhost:8080/portal/classic/home" .
- User "mary" meets only last group   "*:/platform/users" . So she will be redirected to pages in last mapping.

NOTE: This version is working with GateIn 3.2 or EPP 5.2. For earlier versions like GateIn 3.1 or EPP 5.1, use the older version from tag http://anonsvn.jboss.org/repos/qa/portal/login-redirect-gatein-module/tags/exo.portal.redirectplugin-3.1.0/ .