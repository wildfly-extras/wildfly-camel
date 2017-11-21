/*
 * #%L
 * Wildfly Camel
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
package org.wildfly.camel.test.common.aws;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.Assert;
import org.junit.Test;

public class AWSUtilsTest {

    @Test
    public void toEpochMillis() {
        final Instant now = Instant.parse("2017-11-17T14:21:25.123Z");
        Assert.assertEquals(now.toEpochMilli(), AWSUtils.toEpochMillis("AWSUtilsTest-20171117t142125x123Z"));
        Assert.assertEquals(now.toEpochMilli() - 1, AWSUtils.toEpochMillis("AWSUtilsTest-20171117t142125x122Z"));
        Assert.assertEquals(now.toEpochMilli() - AWSUtils.HOUR,
                AWSUtils.toEpochMillis("AWSUtilsTest-20171117t132125x123Z"));
        Assert.assertEquals(now.toEpochMilli() - AWSUtils.TWO_WEEKS,
                AWSUtils.toEpochMillis("AWSUtilsTest-20171103t142125x123Z"));
    }

    @Test
    public void toTimestampedName() {
        final Instant now = Instant.parse("2017-11-17T14:21:25.123Z");
        Assert.assertEquals("AWSUtilsTest-20171117t142125x123Z",
                AWSUtils.toTimestampedName(AWSUtilsTest.class, Clock.fixed(now, ZoneId.of("UTC"))));
    }
}
