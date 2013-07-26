/*
 * #%L
 * Wildfly Camel Testsuite
 * %%
 * Copyright (C) 2013 JBoss by Red Hat
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 2.1 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

package org.wildfly.camel.test.cxf.subA;

import javax.jws.WebService;

import org.jboss.logging.Logger;

/**
 * A simple web service endpoint
 *
 * @author thomas.diesler@jboss.com
 * @since 28-Aug-2012
 */
@WebService(serviceName="EndpointService", portName="EndpointPort", targetNamespace="http://wildfly.camel.test.cxf", endpointInterface = "org.wildfly.camel.test.cxf.subA.Endpoint")
public class EndpointImpl {

   private static Logger log = Logger.getLogger(EndpointImpl.class);

   public String echo(String input) {
      log.info("echo: " + input);
      return "Hello " + input;
   }
}
