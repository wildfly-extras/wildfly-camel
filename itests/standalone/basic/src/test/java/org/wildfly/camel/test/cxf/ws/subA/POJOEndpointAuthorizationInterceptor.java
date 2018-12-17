package org.wildfly.camel.test.cxf.ws.subA;

import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.security.SimpleAuthorizingInterceptor;
import org.apache.cxf.message.Message;
import org.jboss.logging.Logger;

public class POJOEndpointAuthorizationInterceptor extends SimpleAuthorizingInterceptor {
    private static Logger log = Logger.getLogger(POJOEndpointAuthorizationInterceptor.class);

    public POJOEndpointAuthorizationInterceptor() {
        super();
        readRoles();
    }

    @Override
    public void handleMessage(Message message) throws Fault {
        log.warn("POJOEndpointAuthorizationInterceptor.handleMessage()");
        super.handleMessage(message);
    }

    private void readRoles() {
        // just an example, this might read from a configuration file or such
        Map<String, String> roles = new HashMap<String, String>();
        roles.put("echo", "CXFRole");
        setMethodRolesMap(roles);
    }

}
