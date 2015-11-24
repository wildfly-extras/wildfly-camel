package org.wildfly.camel.test.compatibility.subB;

import java.util.concurrent.CountDownLatch;

import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named("counterBean")
public class SomeBean {

    public static CountDownLatch latch = new CountDownLatch(3);

    public String someMethod(String body) {
        latch.countDown();
        return "Remaining count: " + latch.getCount();
    }

}
