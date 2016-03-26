/*
 * #%L
 * Wildfly Camel :: Example :: Camel Mail
 * %%
 * Copyright (C) 2013 - 2015 RedHat
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
package org.wildfly.camel.examples.mail;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.ProducerTemplate;

@SuppressWarnings("serial")
@WebServlet(name = "SendMailServlet", urlPatterns = {"/send/*"}, loadOnStartup = 1)
public class MailSendServlet extends HttpServlet {

  @Inject
  private CamelContext camelContext;

  @Override
  protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
    request.getRequestDispatcher("/index.jsp").forward(request, response);
  }

  @Override
  protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
    Map<String, Object> headers = new HashMap<>();

    Enumeration<String> parameterNames = request.getParameterNames();
    while(parameterNames.hasMoreElements()) {
      String parameterName = parameterNames.nextElement();
      String parameterValue = request.getParameter(parameterName);
      headers.put(parameterName, parameterValue);
    }

    try {
      ProducerTemplate producer = camelContext.createProducerTemplate();
      producer.sendBodyAndHeaders("direct:sendmail", request.getParameter("message"), headers);
      request.getRequestDispatcher("/success.jsp").forward(request, response);
    } catch (CamelExecutionException e) {
      request.setAttribute("error", e);
      request.getRequestDispatcher("/error.jsp").forward(request, response);
    }
  }
}
