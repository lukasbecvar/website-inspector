package xyz.becvar.websiteinspector.modules;

import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.concurrent.Future;
import java.util.concurrent.Executors;
import xyz.becvar.websiteinspector.Main;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import xyz.becvar.websiteinspector.core.Config;
import xyz.becvar.websiteinspector.utils.Logger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import xyz.becvar.websiteinspector.core.AnalysisModule;
import xyz.becvar.websiteinspector.core.AnalysisResult;
import xyz.becvar.websiteinspector.utils.HttpClientManager;

/**
 * Class DirectoryScanner
 *
 * This module implements the directory scan analysis
 *
 * @package xyz.becvar.websiteinspector.modules
 */
public class DirectoryScanner implements AnalysisModule {

    private static final int HTTP_TOO_MANY_REQUESTS = 429;

    private String routesFilePath;

    @Override
    public String getName() {
        return "Directory Scan";
    }

    public DirectoryScanner(String routesFilePath) {
        this.routesFilePath = routesFilePath;
    }

    private InputStream getRoutesInputStream() throws IOException {
        if (routesFilePath != null) {
            return new java.io.FileInputStream(routesFilePath);
        } else {
            return Main.class.getResourceAsStream("/routes.txt");
        }
    }

    /**
     * Runs the directory scan analysis for the given target URL
     * 
     * @param targetUrl The URL to analyze
     * 
     * @return An AnalysisResult object containing the findings
     */
    @Override
    public AnalysisResult analyze(String targetUrl) {
        Set<String> foundDirectories = new HashSet<>();
        if (!targetUrl.endsWith("/")) {
            targetUrl += "/";
        }

        ExecutorService executor = Executors.newFixedThreadPool(Config.SCANNER_THREAD_POOL_SIZE);
        List<Future<?>> futures = new ArrayList<>();

        final AtomicInteger tooManyRequests = new AtomicInteger(0);
        final AtomicBoolean stopScan = new AtomicBoolean(false);

        try (InputStream inputStream = getRoutesInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            List<String> routes = new ArrayList<>();
            String route;
            while ((route = br.readLine()) != null) {
                routes.add(route);
            }
            final int totalRoutes = routes.size();
            final AtomicInteger completedRoutes = new AtomicInteger(0);
            for (String r : routes) {
                String fullUrl = targetUrl + r;
                futures.add(executor.submit(() -> checkUrl(fullUrl, foundDirectories, totalRoutes, completedRoutes, tooManyRequests, stopScan)));
            }

        } catch (IOException e) {
            Logger.printError("Failed to read routes.txt: " + e.getMessage());
        }

        executor.shutdown();
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                Logger.printError("Failed to check route: " + e.getMessage());
            }
        }
        Logger.clearProgress();
        return new DirectoryScanResult(foundDirectories, stopScan.get(), tooManyRequests.get());
    }

    /**
     * Checks the given URL for a directory
     * 
     * @param urlString The URL to check
     * @param foundDirectories The set of found directories
     * @param total The total number of directories to check
     * @param completed The atomic integer of completed directories
     */
    private void checkUrl(String urlString, Set<String> foundDirectories, int total, AtomicInteger completed, AtomicInteger tooManyRequests, AtomicBoolean stopScan) {
        if (stopScan.get()) {
            int current = completed.incrementAndGet();
            Logger.printProgress("Scanning directories: " + current + "/" + total);
            return;
        }

        HttpURLConnection connection = null;
        try {
            connection = HttpClientManager.getConnection(urlString);
            connection.setRequestMethod("GET");
            int responseCode = connection.getResponseCode();

            if (responseCode == HTTP_TOO_MANY_REQUESTS) {
                int blockedCount = tooManyRequests.incrementAndGet();
                if (blockedCount >= Config.MAX_TOO_MANY_REQUESTS) {
                    stopScan.compareAndSet(false, true);
                }
            } else if (responseCode >= 200 && responseCode < 400) {
                synchronized (foundDirectories) {
                    foundDirectories.add(urlString);
                }
            }
        } catch (IOException e) {
            // ignore
        } finally {
            if (connection != null) connection.disconnect();
            int current = completed.incrementAndGet();
            Logger.printProgress("Scanning directories: " + current + "/" + total);
        }
    }

    /**
     * This class represents result of directory scan analysis
     */
    public static class DirectoryScanResult implements AnalysisResult {
        private final Set<String> foundDirectories;
        private final boolean throttled;
        private final int throttledCount;
        public DirectoryScanResult(Set<String> foundDirectories, boolean throttled, int throttledCount) {
            this.foundDirectories = new HashSet<>(foundDirectories);
            this.throttled = throttled;
            this.throttledCount = throttledCount;
        }

        @Override
        public void print() {
            if (!foundDirectories.isEmpty() || throttled) {
                Logger.printSpacer();
                Logger.log("Found Routes");
                Logger.printSpacer();
                foundDirectories.forEach(dir -> Logger.printSuccess("  - Found", dir));
                if (throttled) {
                    Logger.printWarning("Directory scan skipped", "HTTP 429 received " + throttledCount + " times.");
                }
            }
        }
    }
}
