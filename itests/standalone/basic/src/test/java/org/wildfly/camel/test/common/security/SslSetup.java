package org.wildfly.camel.test.common.security;

import java.net.URL;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.wildfly.camel.test.common.utils.EnvironmentUtils;
import org.wildfly.camel.test.common.utils.WildFlyCli;

/**
 * Copies the key material and runs {@code security/cli/ssl/setup.cli}.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class SslSetup implements ServerSetupTask {

    @Override
    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        SecurityUtils.copyKeyMaterial(EnvironmentUtils.getWildFlyHome());
        URL cliUrl = this.getClass().getClassLoader().getResource("security/cli/ssl/setup.cli");
        new WildFlyCli().run(cliUrl).assertSuccess();
    }

    @Override
    public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        URL cliUrl = this.getClass().getClassLoader().getResource("security/cli/ssl/tear-down.cli");
        new WildFlyCli().run(cliUrl).assertSuccess();
    }
}
