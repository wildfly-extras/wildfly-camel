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

package org.wildfly.camel.test.common;

import java.net.InetAddress;
import java.net.ServerSocket;

import org.junit.Assert;
import org.junit.Test;
import org.wildfly.camel.test.common.utils.AvailablePortFinder;

public final class AvailablePortFinderTest {

    @Test
    public void testSimple() throws Exception {
        int portA = AvailablePortFinder.getNextAvailable();
        Assert.assertTrue(portA >= AvailablePortFinder.MIN_PORT_NUMBER);
    }
    
    @Test
    public void testBoundPort() throws Exception {
        InetAddress addr = InetAddress.getLocalHost();
        int portA = AvailablePortFinder.getNextAvailable(addr);
        try (ServerSocket socketA = new ServerSocket(portA, 0, addr)) {
            Assert.assertTrue(socketA.isBound());
            int portB = AvailablePortFinder.getNextAvailable(addr);
            Assert.assertTrue(portA != portB);
        }
    }
    
    @Test
    public void testUnboundPort() throws Exception {
        InetAddress addr = InetAddress.getLocalHost();
        int portA = AvailablePortFinder.getNextAvailable(addr);
        int portB = AvailablePortFinder.getNextAvailable(addr);
        Assert.assertTrue(portA != portB);
    }
    
    @Test
    public void testUnboundPortAnyAddr() throws Exception {
        int portA = AvailablePortFinder.getNextAvailable();
        int portB = AvailablePortFinder.getNextAvailable();
        Assert.assertTrue(portA != portB);
    }
}
