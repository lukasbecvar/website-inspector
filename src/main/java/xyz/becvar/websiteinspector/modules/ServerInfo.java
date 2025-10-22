package xyz.becvar.websiteinspector.modules;

import java.util.*;
import java.net.URL;
import java.io.IOException;
import java.net.InetAddress;
import java.net.HttpURLConnection;
import xyz.becvar.websiteinspector.utils.Logger;
import xyz.becvar.websiteinspector.utils.WebsiteUtils;
import xyz.becvar.websiteinspector.core.AnalysisResult;
import xyz.becvar.websiteinspector.core.AnalysisModule;
import xyz.becvar.websiteinspector.utils.HttpClientManager;

/**
 * Class ServerInfo
 *
 * This module implements server info analysis
 *
 * @package xyz.becvar.websiteinspector.modules
 */
public class ServerInfo implements AnalysisModule {

    @Override
    public String getName() {
        return "Server Info";
    }

    /**
     * Runs the server info analysis for the given target URL
     * 
     * @param targetUrl The URL to analyze
     * 
     * @return An AnalysisResult object containing the findings, or null on critical error
     */
    @Override
    public AnalysisResult analyze(String targetUrl) {
        HttpURLConnection connection = null;
        try {
            URL urlObject = new URL(targetUrl);
            connection = HttpClientManager.getConnection(targetUrl);
            connection.setRequestMethod("HEAD");
            connection.connect();

            String ipAddress = InetAddress.getByName(urlObject.getHost()).getHostAddress();
            String serverType = connection.getHeaderField("Server");
            String cms = detectCms(targetUrl);
            String protocol = urlObject.getProtocol().toUpperCase(Locale.ROOT);
            Map<String, List<String>> headers = connection.getHeaderFields();

            return new ServerInfoResult(ipAddress, serverType, cms, protocol, headers);

        } catch (IOException e) {
            Logger.printError("Error fetching server info: " + e.getMessage());
            return null; // Return null or an ErrorResult object
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    /**
     * Detects the CMS (Content Management System) based on the HTML content
     * 
     * @param url The URL to analyze
     * 
     * @return The detected CMS, or "Unknown" if none is detected
     */
    private String detectCms(String url) {
        String html = WebsiteUtils.getHtml(url);
        if (html == null || html.isEmpty()) return "Unknown";

        // check for cms specific strings
        if (html.contains("wp-content") || html.contains("WordPress")) return "WordPress";
        if (html.contains("Joomla")) return "Joomla";
        if (html.contains("Drupal")) return "Drupal";
        if (html.contains("Magento")) return "Magento";
        if (html.contains("TYPO3")) return "Typo3";
        if (html.contains("PrestaShop")) return "PrestaShop";
        if (html.contains("Shopify")) return "Shopify";
        if (html.contains("Squarespace")) return "Squarespace";
        if (html.contains("X-Wix-Meta-Site")) return "Wix";

        return "Unknown";
    }

    /**
     * Inner class for storing and printing results
     */
    public static class ServerInfoResult implements AnalysisResult {
        private static final Set<String> SECURITY_HEADERS = new HashSet<>(Arrays.asList(
            "cross-origin-embedder-policy",
            "x-content-type-options", "referrer-policy", "permissions-policy",
            "strict-transport-security", "content-security-policy", "x-frame-options",
            "x-xss-protection", "cross-origin-opener-policy", "cross-origin-resource-policy"
        ));

        private final String ipAddress;
        private final String serverType;
        private final String cms;
        private final String protocol;
        private final Map<String, List<String>> headers;

        public ServerInfoResult(String ipAddress, String serverType, String cms, String protocol, Map<String, List<String>> headers) {
            this.ipAddress = ipAddress;
            this.serverType = serverType;
            this.cms = cms;
            this.protocol = protocol;
            this.headers = new HashMap<>(headers);
        }

        @Override
        public void print() {
            Logger.printSpacer();
            Logger.log("Server Info");
            Logger.printSpacer();

            Logger.printColoredKeyValue("Server IP Address", ipAddress);
            Logger.printColoredKeyValue("Server Type", serverType);
            Logger.printColoredKeyValue("CMS", cms);
            Logger.printColoredKeyValue("Protocol", protocol);

            boolean isCloudflare = headers.containsKey("CF-RAY") || (headers.containsKey("Server") && headers.get("Server").stream().anyMatch(h -> h.contains("cloudflare")));
            Logger.printColoredKeyValue("Cloudflare", isCloudflare ? "Yes" : "No");

            Logger.printSpacer();
            Logger.log("HTTP Header Analysis");
            Logger.printSpacer();

            if (headers.containsKey(null) && headers.get(null) != null && !headers.get(null).isEmpty()) {
                Logger.printColoredKeyValue("Status", headers.get(null).get(0));
            } else {
                Logger.printColoredKeyValue("Status", "Unknown");
            }

            Set<String> foundHeaders = new HashSet<>();
            headers.keySet().stream().filter(Objects::nonNull).forEach(key -> foundHeaders.add(key.toLowerCase(Locale.ROOT)));

            SECURITY_HEADERS.forEach(securityHeader -> {
                if (foundHeaders.contains(securityHeader)) {
                    // Find the original header key to get the value, ignoring case
                    for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                        if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(securityHeader)) {
                            Logger.printSuccess(capitalizeHeader(securityHeader), String.join(", ", entry.getValue()));
                            break;
                        }
                    }
                } else {
                    Logger.printWarning(capitalizeHeader(securityHeader), "Missing");
                }
            });

            if (foundHeaders.contains("x-powered-by")) {
                for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                    if (entry.getKey() != null && entry.getKey().equalsIgnoreCase("x-powered-by")) {
                        Logger.printWarning("X-Powered-By", String.join(", ", entry.getValue()) + " (Reveals technology, recommended to remove)");
                        break;
                    }
                }
            }

            // Print other non-security headers
            Logger.printSpacer();
            Logger.log("Other Headers");
            Logger.printSpacer();
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                String key = entry.getKey();
                if (key != null && !SECURITY_HEADERS.contains(key.toLowerCase(Locale.ROOT)) && !key.equalsIgnoreCase("x-powered-by") && !key.equalsIgnoreCase("Status")) {
                    Logger.printColoredKeyValue(key, String.join(", ", entry.getValue()));
                }
            }
        }

        private String capitalizeHeader(String header) {
            String[] parts = header.split("-");
            StringBuilder capitalized = new StringBuilder();
            for (String part : parts) {
                capitalized.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append("-");
            }
            return capitalized.substring(0, capitalized.length() - 1);
        }
    }
}
