/*
 * #%L
 * Wildfly Camel :: Testsuite
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

package org.wildfly.camel.swarm.core.subA;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.language.HeaderExpression;
import org.springframework.stereotype.Component;

@Component
public class RouteBuilderA extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        Path path = Paths.get(System.getProperty("java.io.tmpdir"), "fileA");
        String fileurl = "file://" + path.getParent() + "?fileName=" + path.getFileName();
        from("timer://foo?delay=0&repeatCount=1")
            .setBody(new HeaderExpression(Exchange.TIMER_COUNTER))
            .transform(body().prepend("Hello "))
            .to("log:body")
            .to(fileurl);
    }
}
