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
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

import org.junit.Assert;

public class AWSUtils {

    public static final long HOUR = 1000 * 60 * 60;
    public static final long TWO_WEEKS;
    static {
        TWO_WEEKS = HOUR * 24 * 14;
    }
    /** This strange format wants to be acceptable for all AWS names and still stay a bit readable */
    private static DateTimeFormatter STRIPPED_ISO_INSTANT = DateTimeFormatter.ofPattern("yyyyMMdd't'HHmmss'x'SSS'Z'");

    public static String toTimestampedName(Class<?> cl) {
        return toTimestampedName(cl, Clock.systemUTC());
    }

    static String toTimestampedName(Class<?> cl, Clock clock) {
        return cl.getSimpleName() + "-" + Instant.now(clock).atOffset(ZoneOffset.UTC).format(STRIPPED_ISO_INSTANT);
    }

    public static long toEpochMillis(String name) {
        final int lastDash = name.indexOf('-');
        if (lastDash < 0) {
            Assert.fail("Name '"+ name +"' should contain a dash");
        }
        final int timeStampStart =  name.indexOf('-')+1;
        TemporalAccessor temporalAccessor = STRIPPED_ISO_INSTANT.parse(name.substring(timeStampStart ));
        return LocalDateTime.from(temporalAccessor).atZone(ZoneOffset.UTC).toInstant().toEpochMilli();
    }

}
