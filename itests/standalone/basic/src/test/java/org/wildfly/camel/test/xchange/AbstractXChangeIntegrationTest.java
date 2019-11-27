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
package org.wildfly.camel.test.xchange;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URL;
import java.net.URLConnection;

import org.apache.camel.CamelContext;
import org.apache.camel.component.xchange.XChange;
import org.apache.camel.component.xchange.XChangeComponent;
import org.wildfly.camel.utils.IllegalStateAssertion;

abstract class AbstractXChangeIntegrationTest {

    boolean checkAPIConnection() throws IOException {
        try {
            URL url = new URL("https://api.binance.com");
            URLConnection con = url.openConnection();
            con.connect();
            return true;
        } catch (ConnectException ex) {
            // If the connection to the Binance API endpoint is refused we skip all further testing
            // e.g. Redhat Jenkins Env
            if (ex.getMessage().contains("Connection refused"))
                return false;
            else
                throw ex;
        } catch (IOException ex) {
            throw ex;
        }
    }

    boolean hasAPICredentials(CamelContext camelctx) {
        XChangeComponent component = camelctx.getComponent("xchange", XChangeComponent.class);
        XChange xchange = component.getXChange();
        IllegalStateAssertion.assertNotNull(xchange, "XChange not created");
        return xchange.getExchangeSpecification().getApiKey() != null;
    }
}
