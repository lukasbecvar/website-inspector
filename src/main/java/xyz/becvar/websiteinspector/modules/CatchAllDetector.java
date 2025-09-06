package xyz.becvar.websiteinspector.modules;

import java.net.URL;
import java.util.List;
import java.util.ArrayList;
import java.security.SecureRandom;
import java.net.HttpURLConnection;
import java.util.concurrent.Future;
import java.util.concurrent.Executors;
import xyz.becvar.websiteinspector.Main;
import java.util.concurrent.ExecutorService;
import xyz.becvar.websiteinspector.utils.Logger;

/**
 * This class contains the logic for detecting catch-alls on a website
 */
public class CatchAllDetector {

    private static final int THREAD_POOL_SIZE = Main.SCANNER_THREAD_POOL_SIZE;
    private static final int RANDOM_TEST_COUNT = 10;
    private static final int RANDOM_STRING_LENGTH = 20;
    private static final String ALPHANUMERIC = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom random = new SecureRandom();

    /**
     * Generates a random string of the given length
     * 
     * @param length The length of the string to generate
     * 
     * @return The generated string
     */
    private static String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHANUMERIC.charAt(random.nextInt(ALPHANUMERIC.length())));
        }
        return sb.toString();
    }

    /**
     * Checks the given URL for a catch-all response code
     * 
     * @param urlString The URL to check
     * 
     * @return The response code of the URL, or -1 if an error occurred
     */
    private static int checkUrl(String urlString) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(urlString).openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(Main.CONNECTION_TIMEOUT * 1000);
            connection.setReadTimeout(Main.CONNECTION_TIMEOUT * 1000);
            connection.setRequestProperty("User-Agent", Main.USER_AGENT);
            return connection.getResponseCode();
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Analyzes the given list of response codes and determines if a catch-all response code is present
     * 
     * @param responseCodes The list of response codes to analyze
     * @param type The type of catch-all to analyze (e.g. "Path" or "Subdomain")
     * 
     * @return True if a catch-all response code is present, false otherwise
     */
    private static boolean analyzeCodes(List<Integer> responseCodes, String type) {
        if (responseCodes.isEmpty() || responseCodes.contains(-1) || responseCodes.get(0) == -1) {
            return false;
        }

        int firstCode = responseCodes.get(0);
        boolean isCatchAllCode = (firstCode >= 200 && firstCode < 300) || firstCode == HttpURLConnection.HTTP_MOVED_PERM;

        if (!isCatchAllCode) {
            return false;
        }

        for (int code : responseCodes) {
            if (code != firstCode) {
                return false;
            }
        }

        Logger.logStatus(type + " catch-all detected. Server responds with " + firstCode + " to all random requests.");
        return true;
    }

    /**
     * Runs the catch-all detection for the given base URL
     * 
     * @param baseUrl The base URL to run the detection on
     * 
     * @return True if the catch-all is active, false otherwise
     */
    public static boolean isPathCatchAllActive(String baseUrl) {
        Logger.logStatus("Running path catch-all detection...");
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        List<Future<Integer>> futures = new ArrayList<>();

        for (int i = 0; i < RANDOM_TEST_COUNT; i++) {
            String randomPath = generateRandomString(RANDOM_STRING_LENGTH);
            String urlPath = baseUrl + "/" + randomPath;
            futures.add(executor.submit(() -> checkUrl(urlPath)));
        }

        executor.shutdown();

        List<Integer> responseCodes = new ArrayList<>();
        for (Future<Integer> future : futures) {
            try {
                responseCodes.add(future.get());
            } catch (Exception e) {
                responseCodes.add(-1);
            }
        }
        return analyzeCodes(responseCodes, "Path");
    }

    /**
     * Runs the catch-all detection for the given base URL
     * 
     * @param baseUrl The base URL to run the detection on
     * 
     * @return True if the catch-all is active, false otherwise
     */
    public static boolean isSubdomainCatchAllActive(String baseUrl) {
        Logger.logStatus("Running subdomain catch-all detection...");
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        List<Future<Integer>> futures = new ArrayList<>();
        String baseDomain = baseUrl.replaceAll("^(http[s]?://)", "").replaceAll("/$", "");
        String protocol = baseUrl.startsWith("https://") ? "https://" : "http://";

        for (int i = 0; i < RANDOM_TEST_COUNT; i++) {
            String randomSubdomain = generateRandomString(RANDOM_STRING_LENGTH);
            String urlSubdomain = protocol + randomSubdomain + "." + baseDomain;
            futures.add(executor.submit(() -> checkUrl(urlSubdomain)));
        }

        executor.shutdown();

        List<Integer> responseCodes = new ArrayList<>();
        for (Future<Integer> future : futures) {
            try {
                responseCodes.add(future.get());
            } catch (Exception e) {
                responseCodes.add(-1);
            }
        }
        return analyzeCodes(responseCodes, "Subdomain");
    }
}
