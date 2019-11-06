/*
 * #%L
 * Wildfly Camel :: Subsystem
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

package org.wildfly.extension.camel;

import javax.naming.Context;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.JndiRegistry;

/**
 * The default WildFly {@link CamelContext}.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 18-May-2013
 */
public class WildFlyCamelContext extends DefaultCamelContext {

    private volatile Context namingContext;

    public Context getNamingContext() {
        return namingContext;
    }

    public void setNamingContext(Context namingContext) {
        this.namingContext = namingContext;
        setRegistry(new JndiRegistry(namingContext));
    }
}
