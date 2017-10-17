/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2017 RedHat
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
package org.wildfly.camel.test.pubnub.subA;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(name = "FakePubNubSubscribeAPIServlet", urlPatterns = {"/v2/subscribe/*"})
public class FakePubNubSubscribeAPIServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.getOutputStream().print("{\"t\":{\"t\":\"14607577960932487\",\"r\":1},"
            + "\"m\":[{\"a\":\"4\",\"f\":0,\"i\":\"Publisher-A\",\"p\":{\"t\":\"14607577960925503\",\"r\":1},\"o\":"
            + "{\"t\":\"14737141991877032\",\"r\":2},\"k\":\"sub-c-4cec9f8e-01fa-11e6-8180-0619f8945a4f\","
            + "\"c\":\"subscriberChannel\",\"d\":{\"text\":\"Message\"},\"b\":\"coolChannel\"}]}");
    }
}
