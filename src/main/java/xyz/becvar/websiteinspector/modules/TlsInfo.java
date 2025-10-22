package xyz.becvar.websiteinspector.modules;

import java.util.Date;
import java.util.Locale;
import java.util.Optional;
import java.io.IOException;
import javax.net.ssl.SSLSession;
import java.security.cert.Certificate;
import javax.net.ssl.HttpsURLConnection;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLPeerUnverifiedException;
import xyz.becvar.websiteinspector.utils.Logger;
import xyz.becvar.websiteinspector.core.AnalysisModule;
import xyz.becvar.websiteinspector.core.AnalysisResult;
import xyz.becvar.websiteinspector.utils.HttpClientManager;

/**
 * Class TlsInfo
 *
 * This module implements TLS/SSL analysis
 *
 * @package xyz.becvar.websiteinspector.modules
 */
public class TlsInfo implements AnalysisModule {

    @Override
    public String getName() {
        return "TLS/SSL Info";
    }

    /**
     * Runs TLS/SSL Info analysis for the given target URL
     * 
     * @param targetUrl The URL to analyze
     * 
     * @return Result object containing the findings
     */
    @Override
    public AnalysisResult analyze(String targetUrl) {
        if (!targetUrl.toLowerCase(Locale.ROOT).startsWith("https://")) {
            return null; // Skip non-HTTPS sites
        }

        HttpsURLConnection conn = null;
        try {
            conn = (HttpsURLConnection) HttpClientManager.getConnection(targetUrl);
            conn.connect();

            Optional<SSLSession> sslSessionOptional = conn.getSSLSession();
            if (sslSessionOptional.isPresent()) {
                SSLSession sslSession = sslSessionOptional.get();
                Certificate[] certs = sslSession.getPeerCertificates();

                if (certs.length > 0 && certs[0] instanceof X509Certificate) {
                    X509Certificate cert = (X509Certificate) certs[0];
                    return new TlsInfoResult(
                            cert.getIssuerDN().getName(),
                            cert.getSubjectDN().getName(),
                            cert.getNotBefore(),
                            cert.getNotAfter(),
                            cert.getSigAlgName(),
                            cert.getPublicKey().getAlgorithm(),
                            sslSession.getProtocol()
                    );
                }
            }
        } catch (SSLPeerUnverifiedException e) {
            Logger.printError("SSL peer not verified: " + e.getMessage());
        } catch (IOException e) {
            Logger.printError("Error fetching TLS/SSL info: " + e.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
        }
        return null;
    }

    /**
     * Inner class for storing and printing results
     */
    public static class TlsInfoResult implements AnalysisResult {
        private final String issuer;
        private final String subject;
        private final Date validFrom;
        private final Date validTo;
        private final String signatureAlgorithm;
        private final String publicKeyAlgorithm;
        private final String protocol;

        public TlsInfoResult(String issuer, String subject, Date validFrom, Date validTo, String signatureAlgorithm, String publicKeyAlgorithm, String protocol) {
            this.issuer = issuer;
            this.subject = subject;
            this.validFrom = (Date) validFrom.clone();
            this.validTo = (Date) validTo.clone();
            this.signatureAlgorithm = signatureAlgorithm;
            this.publicKeyAlgorithm = publicKeyAlgorithm;
            this.protocol = protocol;
        }

        @Override
        public void print() {
            // Print header
            Logger.printSpacer();
            Logger.log("TLS/SSL Certificate Info");
            Logger.printSpacer();

            // Print results
            Logger.printColoredKeyValue("Protocol", protocol);
            Logger.printColoredKeyValue("Issuer", issuer);
            Logger.printColoredKeyValue("Subject", subject);
            Logger.printColoredKeyValue("Valid From", validFrom.toString());
            Logger.printColoredKeyValue("Valid To", validTo.toString());
            Logger.printColoredKeyValue("Signature Algorithm", signatureAlgorithm);
            Logger.printColoredKeyValue("Public Key Algorithm", publicKeyAlgorithm);
        }
    }
}
