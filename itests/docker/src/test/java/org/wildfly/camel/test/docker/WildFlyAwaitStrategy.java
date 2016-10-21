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
package org.wildfly.camel.test.docker;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.await.AwaitStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An AwaitStrategy for WildFly which polls container logs until it has completed startup
 */
public class WildFlyAwaitStrategy implements AwaitStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(WildFlyAwaitStrategy.class);
    private static final TimeUnit POLL_UNIT = TimeUnit.SECONDS;
    private static final int POLL_PERIOD = 5;
    private static final int POLL_ITERATIONS = 12;

    private DockerClientExecutor dockerClientExecutor;
    private Cube cube;

    public WildFlyAwaitStrategy() {
    }

    @Override
    public boolean await() {
        boolean result = false;
        int iteration = 0;

        do {
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                dockerClientExecutor.copyLog(cube.getId(), false, true, true, false, -1, baos);
                result = baos.toString().contains("WFLYSRV0025");
            } catch (IOException e) {
                LOG.debug("Failed to read container {} logs due to {}", cube.getId(), e);
            }
            if (!result) {
                iteration++;
                try {
                    POLL_UNIT.sleep(POLL_PERIOD);
                } catch (InterruptedException e) {
                    // Ignore
                }
            }
        } while (!result && iteration < POLL_ITERATIONS);

        return result;
    }

    public void setCube(Cube cube) {
        this.cube = cube;
    }

    public void setDockerClientExecutor(DockerClientExecutor dockerClientExecutor) {
        this.dockerClientExecutor = dockerClientExecutor;
    }
}
