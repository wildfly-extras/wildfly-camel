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
package org.wildfly.camel.test.xmpp.subA;

import java.io.InputStream;
import java.util.Arrays;

import org.apache.vysper.mina.TCPEndpoint;
import org.apache.vysper.storage.StorageProviderRegistry;
import org.apache.vysper.storage.inmemory.MemoryStorageProviderRegistry;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.authorization.AccountManagement;
import org.apache.vysper.xmpp.authorization.Anonymous;
import org.apache.vysper.xmpp.authorization.SASLMechanism;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.MUCModule;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Conference;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.RoomType;
import org.apache.vysper.xmpp.server.XMPPServer;

public class EmbeddedXMPPServer {

    private final XMPPServer server;

    public EmbeddedXMPPServer(int port) throws Exception {

        StorageProviderRegistry providerRegistry = new MemoryStorageProviderRegistry();
        AccountManagement accountManagement = (AccountManagement) providerRegistry.retrieve(AccountManagement.class);

        Entity consumer = EntityImpl.parseUnchecked("consumer@apache.camel");
        accountManagement.addUser(consumer, "secret");

        Entity producer = EntityImpl.parseUnchecked("producer@apache.camel");
        accountManagement.addUser(producer, "secret");

        TCPEndpoint endpoint = new TCPEndpoint();
        endpoint.setPort(port);

        InputStream stream = EmbeddedXMPPServer.class.getResourceAsStream("/xmpp/server.jks");

        server = new XMPPServer("apache.camel");
        server.setStorageProviderRegistry(providerRegistry);
        server.addEndpoint(endpoint);
        server.setTLSCertificateInfo(stream, "secret");
        server.setSASLMechanisms(Arrays.asList(new SASLMechanism[]{new Anonymous()}));
    }

    public void start() throws Exception {
        server.start();

        Conference conference = new Conference("Camel XMPP Test Conference");
        conference.createRoom(EntityImpl.parseUnchecked("anon@apache.camel"), "anon", RoomType.FullyAnonymous);
        conference.createRoom(EntityImpl.parseUnchecked("test@apache.camel"), "test", RoomType.Public);
        server.addModule(new MUCModule("conference", conference));
    }

    public void stop() {
        server.stop();
    }
}
