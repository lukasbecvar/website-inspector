package xyz.becvar.websiteinspector.utils;

import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import xyz.becvar.websiteinspector.Main;

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
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", Main.USER_AGENT);

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                result.append(inputLine);
            }
            in.close();
        } catch (Exception e) {
            SystemUtils.shutdown("Error while getting HTML content from URL: " + urlString);
            return "";
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
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", Main.USER_AGENT);

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine).append("\n");
            }
            in.close();
        } catch (Exception e) {
            return null;
        }
        return content.toString();
    }
}
