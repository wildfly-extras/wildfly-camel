/*
 * #%L
 * Wildfly Camel :: Example :: Camel CDI
 * %%
 * Copyright (C) 2013 - 2014 RedHat
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
package org.wildfly.camel.examples.cxf;

import javax.ejb.Stateless;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

@WebService(serviceName="greeting", endpointInterface = "org.wildfly.camel.examples.cxf.GreetingService")
@SOAPBinding(style = SOAPBinding.Style.RPC)
public class GreetingServiceImpl {

    @WebMethod
    public String sayHello(@WebParam(name = "name") String name) {
        return "Hello " + name ;
    }

    @WebMethod
    public String sayGoodbye(@WebParam(name = "name") String name) {
        return "Goodbye " + name;
    }

}
