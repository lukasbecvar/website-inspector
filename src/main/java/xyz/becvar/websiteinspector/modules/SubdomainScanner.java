package xyz.becvar.websiteinspector.modules;

import java.net.URL;
import java.security.SecureRandom;
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
    private static final int REDIRECT_TEST_COUNT = 5;
    private static final String ALPHANUMERIC = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom random = new SecureRandom();
    private static Set<String> foundSubdomains = new HashSet<>();
    private static int totalSubdomains = 0;
    private static int completedSubdomains = 0;

    private static String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHANUMERIC.charAt(random.nextInt(ALPHANUMERIC.length())));
        }
        return sb.toString();
    }

    private static boolean detectGlobalHttpRedirect(String baseDomain) {
        Logger.log("Checking for global HTTP -> HTTPS redirect...");
        for (int i = 0; i < REDIRECT_TEST_COUNT; i++) {
            String randomSub = generateRandomString(10);
            String testUrl = "http://" + randomSub + "." + baseDomain;
            String expectedLocation = "https://" + randomSub + "." + baseDomain;

            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(testUrl).openConnection();
                connection.setInstanceFollowRedirects(false);
                connection.setRequestMethod("HEAD");
                connection.setConnectTimeout(Main.CONNECTION_TIMEOUT * 1000);
                connection.setRequestProperty("User-Agent", Main.USER_AGENT);

                int responseCode = connection.getResponseCode();
                String locationHeader = connection.getHeaderField("Location");

                // If we get anything other than a 301 to the correct https location, assume no global redirect
                if (responseCode != HttpURLConnection.HTTP_MOVED_PERM || locationHeader == null || !locationHeader.startsWith(expectedLocation)) {
                    Logger.log("No global redirect detected.");
                    return false;
                }
            } catch (IOException e) {
                // If any request fails, we can't be sure, so assume no global redirect
                Logger.log("No global redirect detected (request failed).");
                return false;
            }
        }

        Logger.log("Global HTTP -> HTTPS redirect detected. Scanning HTTPS only.");
        return true;
    }

    public static List<Future<?>> scanSubdomains(String baseUrl)
    {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        List<Future<?>> futures = new ArrayList<>();
        String baseDomain = baseUrl.replaceAll("^(http[s]?://)", "").replaceAll("/$", "");

        boolean httpOnly = !detectGlobalHttpRedirect(baseDomain);

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

            if (httpOnly) {
                totalSubdomains = subdomains.size() * 2;
            } else {
                totalSubdomains = subdomains.size();
            }

            for (String s : subdomains) {
                if (httpOnly) {
                    String httpUrl = "http://" + s + "." + baseDomain;
                    futures.add(executor.submit(() -> checkUrl(httpUrl)));
                }
                String httpsUrl = "https://" + s + "." + baseDomain;
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

            if (responseCode >= 200 && responseCode < 400) {
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
