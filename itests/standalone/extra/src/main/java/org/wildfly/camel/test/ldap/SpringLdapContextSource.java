/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wildfly.camel.test.ldap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.springframework.ldap.core.support.LdapContextSource;

public class SpringLdapContextSource extends LdapContextSource {

    public SpringLdapContextSource() {
        try {
            setUrl("ldap://" + InetAddress.getLocalHost().getHostName() + ":" + getLdapPort());
        } catch (UnknownHostException ex) {
            throw new IllegalStateException(ex);
        }
    }

    static int getLdapPort() {
        try {
            File ldapPortFile = new File (System.getProperty("jboss.server.data.dir"), "ldap-port");
            try (BufferedReader fw = new BufferedReader(new FileReader(ldapPortFile))) {
                return Integer.parseInt(fw.readLine());
            }
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}