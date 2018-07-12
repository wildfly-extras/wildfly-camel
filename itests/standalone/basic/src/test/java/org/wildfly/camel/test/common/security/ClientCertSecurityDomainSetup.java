package org.wildfly.camel.test.common.security;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
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
    static final Path WILDFLY_HOME = Paths.get(System.getProperty("jbossHome"));
    public static final String APPLICATION_ROLE = "testRole";
    public static final String SECURITY_DOMAIN = "client-cert-security-domain";
    public static final String AUTH_METHOD = "CLIENT-CERT";
    public static final String CLIENT_ALIAS = "client";
    public static final String TRUSTSTORE_PASSWORD = "123456";

    private final WildFlyCli wildFlyCli = new WildFlyCli(WILDFLY_HOME);

    @Override
    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        SecurityUtils.copyKeyMaterial(WILDFLY_HOME);
        try (UserManager um = UserManager.forStandaloneApplicationRealm(WILDFLY_HOME)) {
            um.addRole(CLIENT_ALIAS, APPLICATION_ROLE);
        }
        URL cliUrl = this.getClass().getClassLoader().getResource("security/cli/client-cert/setup.cli");
        wildFlyCli.run(cliUrl, "--timeout=15000").assertSuccess();
    }

    @Override
    public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        try (UserManager um = UserManager.forStandaloneApplicationRealm(WILDFLY_HOME)) {
            um.removeRole(CLIENT_ALIAS, APPLICATION_ROLE);
        }
        URL cliUrl = this.getClass().getClassLoader().getResource("security/cli/client-cert/tear-down.cli");
        wildFlyCli.run(cliUrl, "--timeout=15000").assertSuccess();
    }
}
