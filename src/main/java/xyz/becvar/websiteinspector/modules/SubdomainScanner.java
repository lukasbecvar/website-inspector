package xyz.becvar.websiteinspector.modules;

import java.net.URL;
import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.util.ArrayList;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.concurrent.Future;
import java.util.concurrent.Executors;
import xyz.becvar.websiteinspector.Main;
import java.util.concurrent.ExecutorService;
import xyz.becvar.websiteinspector.utils.Logger;

public class SubdomainScanner
{
    private static final int THREAD_POOL_SIZE = Main.SCANNER_THREAD_POOL_SIZE;
    private static Set<String> foundSubdomains = new HashSet<>();
    private static int totalSubdomains = 0;
    private static int completedSubdomains = 0;

    public static List<Future<?>> scanSubdomains(String baseUrl)
    {
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        List<Future<?>> futures = new ArrayList<>();
        String baseDomain = baseUrl.replaceAll("^(http[s]?://)", "");
        baseDomain = baseDomain.replaceAll("/$", "");

        try (InputStream inputStream = Main.class.getResourceAsStream("/subdomains.txt");
             BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {

            List<String> subdomains = new ArrayList<>();
            String subdomain;
            while ((subdomain = br.readLine()) != null) {
                if (subdomain.trim().isEmpty()) {
                    continue;
                }
                subdomains.add(subdomain);
            }
            totalSubdomains = subdomains.size() * 2; // *2 because we check both http and https

            for (String s : subdomains) {
                String httpUrl = "http://" + s + "." + baseDomain;
                String httpsUrl = "https://" + s + "." + baseDomain;

                futures.add(executor.submit(() -> checkUrl(httpUrl)));
                futures.add(executor.submit(() -> checkUrl(httpsUrl)));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        executor.shutdown();

        return futures;
    }

    public static void checkUrl(String urlString)
    {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(Main.CONNECTION_TIMEOUT * 1000);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", Main.USER_AGENT);

            int responseCode = connection.getResponseCode();
            Thread.sleep(50); // Introduce a small delay

            synchronized (SubdomainScanner.class) {
                completedSubdomains++;
                Logger.printProgress("Scanning subdomains: " + completedSubdomains + "/" + totalSubdomains + " (" + String.format("%.2f", (double) completedSubdomains / totalSubdomains * 100) + "%)");
            }

            if (responseCode == HttpURLConnection.HTTP_OK) {
                synchronized (foundSubdomains) {
                    foundSubdomains.add(urlString);
                }
                Logger.log("Subdomain found: " + urlString);
            } else {
                Logger.log("Subdomain not found: " + urlString + " (Response Code: " + responseCode + ")");
            }

        } catch (IOException e) {
            synchronized (SubdomainScanner.class) {
                completedSubdomains++;
                Logger.printProgress("Scanning subdomains: " + completedSubdomains + "/" + totalSubdomains + " (" + String.format("%.2f", (double) completedSubdomains / totalSubdomains * 100) + "%)");
            }
            if (e instanceof java.net.UnknownHostException) {
                Logger.log("Subdomain does not exist: " + urlString);
            } else {
                Logger.log("Error checking subdomain: " + urlString + " -> " + e.getMessage());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Logger.log("Thread interrupted: " + urlString);
        }
    }

    public static Set<String> getFoundSubdomains()
    {
        return foundSubdomains;
    }
}
