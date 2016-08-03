package org.apache.cxf.transport.undertow;

import java.net.URL;

import org.apache.cxf.configuration.jsse.TLSServerParameters;

public interface HttpServerEngine {

    TLSServerParameters getTlsServerParameters();

    String getProtocol();

    String getHost();

    int getPort();

    void addServant(URL nurl, UndertowHTTPHandler handler);

    void removeServant(URL nurl);

}