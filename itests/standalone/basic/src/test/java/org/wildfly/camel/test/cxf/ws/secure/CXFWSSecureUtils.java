package org.wildfly.camel.test.cxf.ws.secure;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Base64;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.wildfly.camel.test.common.security.SecurityUtils;

/**
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class CXFWSSecureUtils {

    private static final String WS_MESSAGE_TEMPLATE = "<Envelope xmlns=\"http://schemas.xmlsoap.org/soap/envelope/\">"
            + "<Body>"
            + "<greet xmlns=\"http://subA.secure.ws.cxf.test.camel.wildfly.org/\">"
            + "<message xmlns=\"\">%s</message>"
            + "<name xmlns=\"\">%s</name>"
            + "</greet>"
            + "</Body>"
            + "</Envelope>";

    static void assertGreet(Path wildFlyHome, String uri, String user, String password, int responseCode,
            String responseBody) throws KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException,
            KeyStoreException, CertificateException, IOException {
        try (CloseableHttpClient httpclient = HttpClients.custom()
                .setSSLSocketFactory(SecurityUtils.createBasicSocketFactory(wildFlyHome)).build()) {
            HttpPost request = new HttpPost(uri);
            request.setHeader("Content-Type", "text/xml");
            request.setHeader("soapaction", "\"urn:greet\"");

            if (user != null) {
                String auth = user + ":" + password;
                String authHeader = "Basic "
                        + Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.ISO_8859_1));
                request.setHeader(HttpHeaders.AUTHORIZATION, authHeader);
            }

            request.setEntity(
                    new StringEntity(String.format(WS_MESSAGE_TEMPLATE, "Hi", "Joe"), StandardCharsets.UTF_8));
            try (CloseableHttpResponse response = httpclient.execute(request)) {
                final int actualCode = response.getStatusLine().getStatusCode();
                Assert.assertEquals(responseCode, actualCode);
                if (actualCode == 200) {
                    HttpEntity entity = response.getEntity();
                    String body = EntityUtils.toString(entity, StandardCharsets.UTF_8);
                    Assert.assertTrue(body.contains(responseBody));
                }
            }
        }
    }

    private CXFWSSecureUtils() {}

}
