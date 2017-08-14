package org.wildfly.camel.test.classloading.subC;

import org.apache.camel.cdi.ImportResource;

@ImportResource("classloading/spring-camel-context.xml")
public class XmlRouteBuilder {
}
