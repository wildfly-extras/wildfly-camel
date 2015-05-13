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
package org.wildfly.camel.test.core.subA;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.camel.Converter;

@Converter
public class CustomConverter {

    @Converter
    public Customer toCustomer(Map<String, String> map) {
        return new Customer(map.get("firstName"), map.get("lastName"));
    }

    @Converter
    public Map<String, String> toMap(Customer customer) {
        Map<String, String> result = new LinkedHashMap<String, String>();
        result.put("firstName", customer.getFirstName());
        result.put("lastName", customer.getLastName());
        return Collections.unmodifiableMap(result);
    }
}
