package xyz.becvar.websiteinspector.utils;

import xyz.becvar.websiteinspector.Main;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpClientManager {

    public static HttpURLConnection getConnection(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(Main.CONNECTION_TIMEOUT * 1000);
        connection.setReadTimeout(Main.CONNECTION_TIMEOUT * 1000);
        connection.setRequestProperty("User-Agent", Main.USER_AGENT);
        return connection;
    }
}
