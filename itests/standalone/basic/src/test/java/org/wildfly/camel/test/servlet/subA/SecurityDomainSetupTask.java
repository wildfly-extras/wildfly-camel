package org.wildfly.camel.test.servlet.subA;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.wildfly.camel.test.common.http.HttpRequest;
import org.wildfly.camel.test.common.utils.DMRUtils;
import org.wildfly.camel.test.common.utils.UserManager;

public class SecurityDomainSetupTask implements ServerSetupTask {

    public static final String SECURITY_DOMAIN = "camel-servlet-application-security-domain";
    private static final String APPLICATION_PASSWORD = "camel-servlet-password";
    private static final String APPLICATION_ROLE = "camel-servlet-role";
    private static final String APPLICATION_USER = "camel-servlet-user";
    private static final String HTTP_AUTH_FACTORY = "camel-servlet-http-authentication-factory";
    private static final String SECURITY_REALM = "camel-servlet-application-realm";
    private static final String USERS_PROPS = "camel-servlet-application-users.properties";
    private static final String ROLES_PROPS = "camel-servlet-application-roles.properties";
    private static final PathAddress HTTP_AUTH_FACTORY_ADDRESS;
    private static final PathAddress ELYTRON_DOMAIN_ADDRESS;
    private static final PathAddress UNDERTOW_DOMAIN_ADDRESS;
    private static final PathAddress REALM_ADDRESS;

    static {
        HTTP_AUTH_FACTORY_ADDRESS = PathAddress.parseCLIStyleAddress("/subsystem=elytron/http-authentication-factory=" + HTTP_AUTH_FACTORY);
        ELYTRON_DOMAIN_ADDRESS = PathAddress.parseCLIStyleAddress("/subsystem=elytron/security-domain=" + SECURITY_DOMAIN);
        UNDERTOW_DOMAIN_ADDRESS = PathAddress.parseCLIStyleAddress("/subsystem=undertow/application-security-domain=" + SECURITY_DOMAIN);
        REALM_ADDRESS = PathAddress.parseCLIStyleAddress("/subsystem=elytron/properties-realm=" + SECURITY_REALM);
    }

    @Override
    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        HttpRequest.post("https://localhost:8443").getResponse();

        final ModelControllerClient client = managementClient.getControllerClient();
        try (UserManager userManager = UserManager.forStandaloneCustomRealm(USERS_PROPS, ROLES_PROPS, SECURITY_REALM)) {
            userManager.addUser(APPLICATION_USER, APPLICATION_PASSWORD)
                .addRole(APPLICATION_USER, APPLICATION_ROLE);
        }

        final ModelNode addRealm = Util.createAddOperation(REALM_ADDRESS);
        final ModelNode usersProps = addRealm.get("users-properties");
        usersProps.get("path").set(USERS_PROPS);
        usersProps.get("relative-to").set("jboss.server.config.dir");
        usersProps.get("digest-realm-name").set(SECURITY_REALM);
        final ModelNode rolesProps = addRealm.get("groups-properties");
        rolesProps.get("path").set(ROLES_PROPS);
        rolesProps.get("relative-to").set("jboss.server.config.dir");

        final ModelNode addElytronDomain = Util.createAddOperation(ELYTRON_DOMAIN_ADDRESS);
        addElytronDomain.get("default-realm").set(SECURITY_REALM);
        addElytronDomain.get("permission-mapper").set("default-permission-mapper");
        final ModelNode realm = new ModelNode();
        realm.get("realm").set(SECURITY_REALM);
        realm.get("role-decoder").set("groups-to-roles");
        addElytronDomain.get("realms").setEmptyList().add(realm);

        final ModelNode addHttpAuthentication = Util.createAddOperation(HTTP_AUTH_FACTORY_ADDRESS);
        addHttpAuthentication.get("security-domain").set(SECURITY_DOMAIN);
        addHttpAuthentication.get("http-server-mechanism-factory").set("global");
        addHttpAuthentication.get("mechanism-configurations").get(0).get("mechanism-name").set("BASIC");
        addHttpAuthentication.get("mechanism-configurations").get(0).get("mechanism-realm-configurations").get(0).get("realm-name").set(SECURITY_REALM);

        final ModelNode addUndertowDomain = Util.createAddOperation(UNDERTOW_DOMAIN_ADDRESS);
        addUndertowDomain.get("http-authentication-factory").set(HTTP_AUTH_FACTORY);

        DMRUtils.batchNode()
            .addStep(addRealm)
            .addStep(addElytronDomain)
            .addStep(addHttpAuthentication)
            .addStep(addUndertowDomain)
            .execute(client)
            .assertSuccess();
    }

    @Override
    public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        try (UserManager userManager = UserManager.forStandaloneCustomRealm(USERS_PROPS, ROLES_PROPS, SECURITY_REALM)) {
            userManager.removeUser(APPLICATION_USER)
                .removeRole(APPLICATION_USER, APPLICATION_ROLE);
        }

        DMRUtils.batchNode()
            .addStep(Util.createRemoveOperation(UNDERTOW_DOMAIN_ADDRESS))
            .addStep(Util.createRemoveOperation(HTTP_AUTH_FACTORY_ADDRESS))
            .addStep(Util.createRemoveOperation(ELYTRON_DOMAIN_ADDRESS))
            .addStep(Util.createRemoveOperation(REALM_ADDRESS))
            .execute(managementClient.getControllerClient())
            .assertSuccess();
    }
}
