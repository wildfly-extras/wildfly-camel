/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2018 RedHat
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package org.wildfly.camel.test.cxf.ws.subA;

import javax.jws.WebService;
import javax.security.auth.Subject;
import javax.security.jacc.PolicyContext;
import javax.security.jacc.PolicyContextException;

import org.jboss.logging.Logger;
import org.jboss.security.SecurityConstants;
import org.apache.cxf.interceptor.InInterceptors;
import org.jboss.ws.api.annotation.EndpointConfig;
/**
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
@WebService(
        serviceName = "EndpointService",
        portName = "EndpointPort",
        targetNamespace = "http://wildfly.camel.test.cxf",
        endpointInterface = "org.wildfly.camel.test.common.types.Endpoint",
        wsdlLocation = "WEB-INF/EndpointService.wsdl"
)
@EndpointConfig(configFile = "WEB-INF/jaxws-endpoint-config.xml", configName = "UsernameTokenEndpointImplConfig")
@InInterceptors(interceptors = {
      "org.jboss.wsf.stack.cxf.security.authentication.SubjectCreatingPolicyInterceptor",
      "org.wildfly.camel.test.cxf.ws.subA.POJOEndpointAuthorizationInterceptor"}
)
public class UsernameTokenEndpointImpl {

    private static Logger log = Logger.getLogger(UsernameTokenEndpointImpl.class);

    public String echo(String input) {
        log.info("echo: " + input);
        Subject subject = null;
        try {
            subject = (Subject) PolicyContext.getContext(SecurityConstants.SUBJECT_CONTEXT_KEY);

            if (subject == null) {
                //throw new SecurityException("Could not locate policy context");
            } else {
                log.warn("Subject:" + subject + " Principal::" + subject.getPrincipals());
            }

        } catch (final PolicyContextException e) {
            throw new RuntimeException(e);
        }
        return "Hello " + input;
    }
}
