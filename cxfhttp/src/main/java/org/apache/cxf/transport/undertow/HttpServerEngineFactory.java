package org.apache.cxf.transport.undertow;

public interface HttpServerEngineFactory {

    HttpServerEngine retrieveHTTPServerEngine(int port);

    HttpServerEngine getHTTPServerEngine(String host, int port, String protocol);

    AbstractHTTPServerEngine createHTTPServerEngine(String host, int port, String protocol);

}
