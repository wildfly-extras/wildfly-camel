package org.wildfly.camel.test.cdi.subC;

import javax.inject.Named;

import org.apache.camel.CamelContext;

@Named
public class InjectedContextBean {
    private
    CamelContext context;
}
