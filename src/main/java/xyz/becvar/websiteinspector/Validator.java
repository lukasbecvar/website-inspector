package xyz.becvar.websiteinspector;

import java.net.URL;
import java.util.List;
import java.net.HttpURLConnection;
import java.util.concurrent.Future;
import xyz.becvar.websiteinspector.utils.SystemUtils;

public class Validator
{
    public static boolean checkIsWebsiteAvailable(String url)
    {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            // set custom user agent
            connection.setRequestProperty("User-Agent", Main.USER_AGENT);

            int responseCode = connection.getResponseCode();
            return (responseCode >= 200 && responseCode < 300);
        } catch (Exception e) {
            return false;
        }
    }

    public static String validateUrl(String url)
    {
        String httpsUrl;
        String httpUrl;

        if (url == null || url.trim().isEmpty()) {
            SystemUtils.shutdown("URL is null or empty.");
        }

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

    public static void waitForCompletion(List<Future<?>> futures) {
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
