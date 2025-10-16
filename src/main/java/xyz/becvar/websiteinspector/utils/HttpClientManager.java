package xyz.becvar.websiteinspector.utils;

import java.net.URL;
import java.io.IOException;
import java.net.HttpURLConnection;
import xyz.becvar.websiteinspector.core.Config;

/**
 * This class manages the HttpURLConnection
 */
public class HttpClientManager {

    /**
     * Creates a new HttpURLConnection for the given URL
     * 
     * @param urlString The URL to connect to
     * 
     * @return The HttpURLConnection
     * 
     * @throws IOException If an error occurs while creating the connection
     */
    public static HttpURLConnection getConnection(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(Config.CONNECTION_TIMEOUT * 1000);
        connection.setReadTimeout(Config.CONNECTION_TIMEOUT * 1000);
        connection.setRequestProperty("User-Agent", Config.USER_AGENT);
        return connection;
    }
}
