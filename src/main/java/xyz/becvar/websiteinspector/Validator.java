package xyz.becvar.websiteinspector;

import java.net.URL;
import java.net.HttpURLConnection;
import xyz.becvar.websiteinspector.utils.SystemUtils;
import xyz.becvar.websiteinspector.utils.HttpClientManager;

/**
 * This class contains utility methods for validating URLs
 */
public class Validator
{
    /**
     * Checks if the given URL is available
     * 
     * @param url The URL to check
     * 
     * @return True if the URL is available, false otherwise
     */
    public static boolean checkIsWebsiteAvailable(String url)
    {
        try {
            HttpURLConnection connection = HttpClientManager.getConnection(url);
            connection.setRequestMethod("HEAD");

            int responseCode = connection.getResponseCode();
            return (responseCode >= 200 && responseCode < 300);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Validates the given URL
     * 
     * @param url The URL to validate
     * 
     * @return The validated URL, or null if the URL is invalid
     */
    public static String validateUrl(String url)
    {
        String httpsUrl;
        String httpUrl;

        if (url == null || url.trim().isEmpty()) {
            SystemUtils.shutdown("URL is null or empty.");
            return null;
        }

        // remove last slash
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }

        // construct https and http URLs
        if (url.startsWith("https://")) {
            httpsUrl = url;
            httpUrl = "http://" + url.substring(8);
        } else if (url.startsWith("http://")) {
            httpsUrl = "https://" + url.substring(7);
            httpUrl = url;
        } else {
            httpsUrl = "https://" + url;
            httpUrl = "http://" + url;
        }

        // check if HTTPS is available
        if (checkIsWebsiteAvailable(httpsUrl)) {
            return httpsUrl;
        }

        // check if HTTP is available
        if (checkIsWebsiteAvailable(httpUrl)) {
            return httpUrl;
        } else {
            SystemUtils.shutdown("Website is not available.");
            return null;
        }
    }
}
