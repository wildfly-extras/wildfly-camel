/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2016 RedHat
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
package org.wildfly.camel.test.olingo2.subA;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cxf.jaxrs.servlet.CXFNonSpringJaxrsServlet;
import org.apache.olingo.odata2.api.ODataServiceFactory;

@WebServlet(name = "olingo2-test",  urlPatterns = { "/MyODataSample.svc/*" }, loadOnStartup = 1, initParams = {
    @WebInitParam(name = "javax.ws.rs.Application", value = "org.apache.olingo.odata2.core.rest.app.ODataApplication"),
    @WebInitParam(name = "org.apache.olingo.odata2.service.factory", value = "org.wildfly.camel.test.olingo2.subA.MyServiceFactory")
})
public class Olingo2TestServlet extends CXFNonSpringJaxrsServlet {

    public Olingo2TestServlet() {
        this.setClassLoader(getClass().getClassLoader());
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        request.setAttribute(ODataServiceFactory.FACTORY_CLASSLOADER_LABEL, getClass().getClassLoader());
        super.doGet(request, response);
    }
}
