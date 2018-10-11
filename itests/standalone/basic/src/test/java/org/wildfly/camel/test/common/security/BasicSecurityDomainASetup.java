package org.wildfly.camel.test.common.security;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.wildfly.camel.test.common.http.HttpRequest;
import org.wildfly.camel.test.common.utils.DMRUtils;
import org.wildfly.camel.test.common.utils.UserManager;

/**
 * Creates an Undertow {@code application-security-domain} called {@value #SECURITY_DOMAIN} backed by a custom Elytron
 * domain which in turn uses custom {@value #USERS_PROPS} and {@value #ROLES_PROPS}. Also adds some users and roles
 * therein.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class BasicSecurityDomainASetup implements ServerSetupTask {
    public static final String APPLICATION_PASSWORD = "a-password+";
    public static final String APPLICATION_PASSWORD_SUB = "a-password+Sub";
    public static final String APPLICATION_PASSWORD_REL = "a-password+Rel";
    public static final String APPLICATION_ROLE = "a-testRole";
    public static final String APPLICATION_ROLE_SUB = "a-testRoleSub";
    public static final String APPLICATION_ROLE_REL = "a-testRoleRel";
    public static final String APPLICATION_USER = "a-testUser";
    public static final String APPLICATION_USER_SUB = "a-testUserSub";
    public static final String APPLICATION_USER_REL = "a-testUserRel";
    public static final String AUTH_METHOD = "BASIC";
    private static final PathAddress HTTP_AUTH_FACTORY_ADDRESS;
    private static final PathAddress ELYTRON_DOMAIN_ADDRESS;
    private static final PathAddress UNDERTOW_DOMAIN_ADDRESS;
    private static final PathAddress REALM_ADDRESS;
    private static final String HTTPS_HOST = "https://localhost:8443";

    public static final String HTTP_AUTH_FACTORY = "basic-a-http-authentication-factory";
    public static final String SECURITY_DOMAIN = "basic-a-application-security-domain";

    private static final String SECURITY_REALM = "a-application-realm";
    private static final String USERS_PROPS = "a-application-users.properties";
    private static final String ROLES_PROPS = "a-application-roles.properties";

    static {
        HTTP_AUTH_FACTORY_ADDRESS = PathAddress
                .parseCLIStyleAddress("/subsystem=elytron/http-authentication-factory=" + HTTP_AUTH_FACTORY);
        ELYTRON_DOMAIN_ADDRESS = PathAddress
                .parseCLIStyleAddress("/subsystem=elytron/security-domain=" + SECURITY_DOMAIN);
        UNDERTOW_DOMAIN_ADDRESS = PathAddress
                .parseCLIStyleAddress("/subsystem=undertow/application-security-domain=" + SECURITY_DOMAIN);
        REALM_ADDRESS = PathAddress
                .parseCLIStyleAddress("/subsystem=elytron/properties-realm=" + SECURITY_REALM);
    }

    @Override
    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        // Force WildFly to create the default application.keystore
        HttpRequest.post(HTTPS_HOST).getResponse();
        final ModelControllerClient client = managementClient.getControllerClient();

        try (UserManager um = UserManager.forStandaloneCustomRealm(USERS_PROPS, ROLES_PROPS, SECURITY_REALM)) {
            um
                    .addUser(APPLICATION_USER, APPLICATION_PASSWORD)
                    .addRole(APPLICATION_USER, APPLICATION_ROLE)
                    .addUser(APPLICATION_USER_SUB, APPLICATION_PASSWORD_SUB)
                    .addRole(APPLICATION_USER_SUB, APPLICATION_ROLE_SUB)
                    .addUser(APPLICATION_USER_REL, APPLICATION_PASSWORD_REL)
                    .addRole(APPLICATION_USER_REL, APPLICATION_ROLE_REL)
            ;
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
        addHttpAuthentication.get("mechanism-configurations").get(0).get("mechanism-realm-configurations").get(0)
                .get("realm-name").set(SECURITY_REALM);

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
        try (UserManager um = UserManager.forStandaloneCustomRealm(USERS_PROPS, ROLES_PROPS, SECURITY_REALM)) {
            um
                    .removeUser(APPLICATION_USER)
                    .removeRole(APPLICATION_USER, APPLICATION_ROLE)
                    .removeUser(APPLICATION_USER_SUB)
                    .removeRole(APPLICATION_USER_SUB, APPLICATION_ROLE_SUB)
                    .removeUser(APPLICATION_USER_REL)
                    .removeRole(APPLICATION_USER_REL, APPLICATION_ROLE_REL)
            ;
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
