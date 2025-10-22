package xyz.becvar.websiteinspector.modules;

import java.util.List;
import java.io.IOException;
import java.util.ArrayList;
import java.net.HttpURLConnection;
import java.util.concurrent.Future;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import xyz.becvar.websiteinspector.core.Config;
import xyz.becvar.websiteinspector.utils.Logger;
import xyz.becvar.websiteinspector.utils.StringUtils;
import xyz.becvar.websiteinspector.utils.HttpClientManager;

/**
 * Class CatchAllDetector
 *
 * This module contains the logic for detecting catch-alls on a website
 *
 * @package xyz.becvar.websiteinspector.modules
 */
public class CatchAllDetector {

    private static final int RANDOM_TEST_COUNT = Config.RANDOM_TEST_COUNT;
    private static final int THREAD_POOL_SIZE = Config.SCANNER_THREAD_POOL_SIZE;

    /**
     * Checks the given URL for a catch-all response code
     * 
     * @param urlString The URL to check
     * 
     * @return The response code of the URL, or -1 if an error occurred
     */
    private static int checkUrl(String urlString) {
        HttpURLConnection connection = null;
        try {
            connection = HttpClientManager.getConnection(urlString);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod("HEAD");
            return connection.getResponseCode();
        } catch (IOException e) {
            return -1;
        } finally {
            if (connection != null) connection.disconnect();
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
            String randomPath = StringUtils.generateRandomString(20);
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
            String randomSubdomain = StringUtils.generateRandomString(20);
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
