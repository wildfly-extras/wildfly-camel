package org.wildfly.camel.test.script.subA;

import org.apache.camel.Exchange;
import org.apache.camel.language.groovy.GroovyShellFactory;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;

import groovy.lang.GroovyShell;

public class CustomGroovyShellFactory implements GroovyShellFactory {

    public GroovyShell createGroovyShell(Exchange exchange) {
        ImportCustomizer importCustomizer = new ImportCustomizer();
        importCustomizer.addStaticStars("com.example.Utils");
        CompilerConfiguration configuration = new CompilerConfiguration();
        configuration.addCompilationCustomizers(importCustomizer);
        return new GroovyShell(configuration);
    }

}