package org.wildfly.camel.test.common.security;

import java.net.URL;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.wildfly.camel.test.common.utils.EnvironmentUtils;
import org.wildfly.camel.test.common.utils.UserManager;
import org.wildfly.camel.test.common.utils.WildFlyCli;

/**
 * Creates an Undertow {@code application-security-domain} called {@value #SECURITY_DOMAIN} and links it with the
 * default Elytron {@code ApplicationDomain} for authorization. Also adds some roles to
 * {@code application-roles.properties}.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class ClientCertSecurityDomainSetup implements ServerSetupTask {
    public static final String APPLICATION_ROLE = "testRole";
    public static final String SECURITY_DOMAIN = "client-cert-security-domain";
    public static final String AUTH_METHOD = "CLIENT-CERT";
    public static final String CLIENT_ALIAS = "client";
    public static final String TRUSTSTORE_PASSWORD = "123456";

    @Override
    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        SecurityUtils.copyKeyMaterial(EnvironmentUtils.getWildFlyHome());
        try (UserManager um = UserManager.forStandaloneApplicationRealm()) {
            um.addRole(CLIENT_ALIAS, APPLICATION_ROLE);
        }
        URL cliUrl = this.getClass().getClassLoader().getResource("security/cli/client-cert/setup.cli");
        WildFlyCli.run(cliUrl).assertSuccess();
    }

    @Override
    public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        try (UserManager um = UserManager.forStandaloneApplicationRealm()) {
            um.removeRole(CLIENT_ALIAS, APPLICATION_ROLE);
        }
        URL cliUrl = this.getClass().getClassLoader().getResource("security/cli/client-cert/tear-down.cli");
        WildFlyCli.run(cliUrl).assertSuccess();
    }
}
