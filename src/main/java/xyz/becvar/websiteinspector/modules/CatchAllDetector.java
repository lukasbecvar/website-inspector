package xyz.becvar.websiteinspector.modules;

import xyz.becvar.websiteinspector.Main;
import xyz.becvar.websiteinspector.utils.Logger;

import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class CatchAllDetector {

    private static final int THREAD_POOL_SIZE = Main.SCANNER_THREAD_POOL_SIZE;
    private static final int RANDOM_TEST_COUNT = 10;
    private static final int RANDOM_STRING_LENGTH = 20;
    private static final String ALPHANUMERIC = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom random = new SecureRandom();

    private static String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHANUMERIC.charAt(random.nextInt(ALPHANUMERIC.length())));
        }
        return sb.toString();
    }

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
