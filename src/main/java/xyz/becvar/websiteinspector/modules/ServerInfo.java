package xyz.becvar.websiteinspector.modules;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import xyz.becvar.websiteinspector.Main;
import xyz.becvar.websiteinspector.utils.Logger;
import xyz.becvar.websiteinspector.utils.WebsiteUtils;

public class ServerInfo {

    // Set of security headers to check for
    private static final Set<String> SECURITY_HEADERS = new HashSet<>(Arrays.asList(
            "strict-transport-security",
            "content-security-policy",
            "x-frame-options",
            "x-content-type-options",
            "referrer-policy",
            "permissions-policy"
    ));

    public static void printServerInfo(String url) {
        try {
            URL urlObject = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) urlObject.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setRequestProperty("User-Agent", Main.USER_AGENT);
            connection.connect();

            // Basic Info
            InetAddress address = InetAddress.getByName(urlObject.getHost());
            Logger.printColoredKeyValue("Server IP Address", address.getHostAddress());
            Logger.printColoredKeyValue("Server Type", connection.getHeaderField("Server"));
            Logger.printColoredKeyValue("CMS", detectCms(url));
            Logger.printColoredKeyValue("Protocol", urlObject.getProtocol().toUpperCase());

            Map<String, List<String>> headers = connection.getHeaderFields();

            // Cloudflare Check
            boolean isCloudflare = headers.containsKey("CF-RAY") || 
                                   (headers.containsKey("Server") && headers.get("Server").stream().anyMatch(h -> h.contains("cloudflare")));
            Logger.printColoredKeyValue("Cloudflare", isCloudflare ? "Yes" : "No");

            // --- Header Analysis ---
            Logger.printSpacer();
            Logger.log("HTTP Header Analysis");
            Logger.printSpacer();

            // Status
            Logger.printColoredKeyValue("Status", headers.get(null).get(0));

            // Analyze Security Headers
            Set<String> foundHeaders = new HashSet<>();
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                String key = entry.getKey();
                if (key != null) {
                    foundHeaders.add(key.toLowerCase());
                    if (SECURITY_HEADERS.contains(key.toLowerCase())) {
                        Logger.printSuccess(key, String.join(", ", entry.getValue()));
                    }
                }
            }

            // Report missing security headers
            for (String missingHeader : SECURITY_HEADERS) {
                if (!foundHeaders.contains(missingHeader)) {
                    Logger.printWarning(capitalizeHeader(missingHeader), "Missing");
                }
            }

            // Check for X-Powered-By
            if (foundHeaders.contains("x-powered-by")) {
                Logger.printWarning("X-Powered-By", String.join(", ", headers.get("X-Powered-By")) + " (Reveals technology, recommended to remove)");
            }

            // Print other non-security headers
            Logger.printSpacer();
            Logger.log("Other Headers");
            Logger.printSpacer();
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                String key = entry.getKey();
                if (key != null && !SECURITY_HEADERS.contains(key.toLowerCase()) && !key.equalsIgnoreCase("x-powered-by") && !key.equalsIgnoreCase("Status")) {
                    Logger.printColoredKeyValue(key, String.join(", ", entry.getValue()));
                }
            }

        } catch (IOException e) {
            Logger.printError("Error fetching server info: " + e.getMessage());
        }
    }

    private static String capitalizeHeader(String header) {
        String[] parts = header.split("-");
        StringBuilder capitalized = new StringBuilder();
        for (String part : parts) {
            capitalized.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append("-");
        }
        return capitalized.substring(0, capitalized.length() - 1);
    }

    public static String detectCms(String url) {
        String html = WebsiteUtils.getHtml(url);
        if (html == null || html.isEmpty()) return "Unknown";

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
}