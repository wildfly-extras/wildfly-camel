package org.wildfly.camel.test.common.security;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptor;
import org.apache.cxf.security.transport.TLSSessionInfo;
import org.apache.cxf.transport.http.UntrustedURLConnectionIOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wildfly.camel.test.common.utils.EnvironmentUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertPathBuilderException;
import java.security.cert.CertStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

/**
 * A CXF {@link PhaseInterceptor} doing something like client cert authentication. For testing purposes only.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class SecurityInInterceptor extends AbstractPhaseInterceptor<Message> {

    public static final String MESSAGE_NOT_MATCH = "The client certificate does not match the defined cert constraints";
    public static final String MESSAGE_NO_CLIENT_CERTIFICATE = "No client certificates were found";
    public static final String SUBJECT_DN = "CN=client, OU=jboss, O=redhat, L=Brno, C=CZ";

    private static final Logger log = LoggerFactory.getLogger(SecurityInInterceptor.class);

    private final Set<X509Certificate> trustedRootCerts;
    private final Set<X509Certificate> intermediateCerts;

    public SecurityInInterceptor(String trustStorePath, String password) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, NoSuchProviderException {
        super(Phase.PRE_STREAM);
        final Set<X509Certificate> rootCerts = new HashSet<X509Certificate>();
        final Set<X509Certificate> iCerts = new HashSet<X509Certificate>();

        final Path trustStoreAbsPath = EnvironmentUtils.getWildFlyHome().resolve(trustStorePath);

        KeyStore trustStore = KeyStore.getInstance("JKS");

        try (InputStream in = Files.newInputStream(trustStoreAbsPath)) {
            trustStore.load(in, password.toCharArray());

            Enumeration<String> aliases = trustStore.aliases();
            while (aliases.hasMoreElements()) {
                final String alias = aliases.nextElement();
                final X509Certificate cert = (X509Certificate) trustStore.getCertificate(alias);
                if (isSelfSigned(cert)) {
                    rootCerts.add(cert);
                } else {
                    iCerts.add(cert);
                }
            }
            trustedRootCerts = Collections.unmodifiableSet(rootCerts);
            intermediateCerts = Collections.unmodifiableSet(iCerts);
        }
    }

    @Override
    public void handleMessage(Message message) throws Fault {
        log.warn("SecurityInInterceptor message = " + message);
        try {
            final TLSSessionInfo tlsInfo = message.get(TLSSessionInfo.class);
            final Certificate[] certs = tlsInfo.getPeerCertificates();
            if (certs == null || certs.length == 0) {
                final Fault e = new Fault(new UntrustedURLConnectionIOException(MESSAGE_NO_CLIENT_CERTIFICATE));
                e.setStatusCode(403);
                throw e;
            } else {
                /* If the client sends more certs it is probably enough for one of them to be valid.
                 * So this loop is correct only if the client sends one which is the case in our tests. */
                for (Certificate c : certs) {
                    verifyCertificate((X509Certificate) c);
                }
            }
        } catch (Fault ex) {
            throw ex;
        } catch (CertificateException | NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException | CertPathBuilderException ex) {
            throw new Fault(ex);
        }
    }

    /**
     * Based on https://svn.apache.org/repos/asf/cxf/tags/cxf-2.4.1/distribution/src/main/release/samples/sts_issue_operation/src/main/java/demo/sts/provider/cert/CertificateVerifier.java
     *
     * @param cert
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchProviderException
     * @throws InvalidAlgorithmParameterException
     * @throws CertPathBuilderException
     */
    public void verifyCertificate(X509Certificate cert) throws CertificateException, NoSuchAlgorithmException,
            NoSuchProviderException, InvalidAlgorithmParameterException, CertPathBuilderException {
        // Prepare a set of trusted root CA certificates
        // and a set of intermediate certificates
        // Create the selector that specifies the starting certificate
        X509CertSelector selector = new X509CertSelector();
        selector.setCertificate(cert);

        // Create the trust anchors (set of root CA certificates)
        Set<TrustAnchor> trustAnchors = new HashSet<TrustAnchor>();
        for (X509Certificate trustedRootCert : trustedRootCerts) {
            trustAnchors.add(new TrustAnchor(trustedRootCert, null));
        }

        // Configure the PKIX certificate builder algorithm parameters
        PKIXBuilderParameters pkixParams = new PKIXBuilderParameters(trustAnchors, selector);

        // Disable CRL checks (this is done manually as additional step)
        pkixParams.setRevocationEnabled(false);

        // Specify a list of intermediate certificates
        CertStore intermediateCertStore = CertStore.getInstance("Collection",
                new CollectionCertStoreParameters(intermediateCerts));
        pkixParams.addCertStore(intermediateCertStore);

        // Build and verify the certification chain
        CertPathBuilder builder = CertPathBuilder.getInstance("PKIX");
        builder.build(pkixParams);
        // Attempt to build the certification chain and verify it

        // Check whether the certificate is revoked by the CRL
        // given in its CRL distribution point extension
        // CRLVerifier.verifyCertificateCRLs(cert);

        // The chain is verified.
    }

    /**
     * Checks whether given X.509 certificate is self-signed.
     *
     * @throws NoSuchProviderException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     */
    public static boolean isSelfSigned(X509Certificate cert)
            throws CertificateException, NoSuchAlgorithmException, NoSuchProviderException {
        try {
            // Try to verify certificate signature with its own public key
            PublicKey key = cert.getPublicKey();
            cert.verify(key);
            return true;
        } catch (SignatureException sigEx) {
            // Invalid signature --> not self-signed
            return false;
        } catch (InvalidKeyException keyEx) {
            // Invalid key --> not self-signed
            return false;
        }
    }

}
