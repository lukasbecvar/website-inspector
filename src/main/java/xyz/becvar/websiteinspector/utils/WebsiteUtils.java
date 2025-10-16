package xyz.becvar.websiteinspector.utils;

import java.net.URL;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import xyz.becvar.websiteinspector.Main;
import java.nio.charset.StandardCharsets;

/**
 * This class contains utility methods for working with websites
 */
public class WebsiteUtils
{
    /**
     * Gets the HTML content of the given URL
     * 
     * @param urlString The URL to get the HTML content from
     * 
     * @return The HTML content of the URL
     */
    public static String getHtml(String urlString)
    {
        StringBuilder result = new StringBuilder();
        try {
            HttpURLConnection conn = HttpClientManager.getConnection(urlString);
            conn.setRequestMethod("GET");

            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    result.append(inputLine);
                }
            }
        } catch (IOException e) {
            // Return null on any error, let the caller handle it
            return null;
        }
        return result.toString();
    }

    /**
     * Downloads the file content of the given URL
     * 
     * @param urlString The URL to download the file content from
     * 
     * @return The file content of the URL
     */
    public static String downloadFileContent(String urlString)
    {
        StringBuilder content = new StringBuilder();
        try {
            HttpURLConnection conn = HttpClientManager.getConnection(urlString);
            conn.setRequestMethod("GET");

            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine).append("\n");
                }
            }
        } catch (IOException e) {
            return null;
        }
        return content.toString();
    }
}
