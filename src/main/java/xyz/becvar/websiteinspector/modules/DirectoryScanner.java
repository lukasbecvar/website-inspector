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
import java.util.concurrent.ExecutorService;
import xyz.becvar.websiteinspector.core.Config;
import xyz.becvar.websiteinspector.utils.Logger;
import java.util.concurrent.atomic.AtomicInteger;
import xyz.becvar.websiteinspector.core.AnalysisModule;
import xyz.becvar.websiteinspector.core.AnalysisResult;
import xyz.becvar.websiteinspector.utils.HttpClientManager;

/**
 * This class implements the Directory Scan analysis module
 */
public class DirectoryScanner implements AnalysisModule {

    @Override
    public String getName() {
        return "Directory Scan";
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

        try (InputStream inputStream = getClass().getResourceAsStream("/routes.txt");
             BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {

            List<String> routes = new ArrayList<>();
            String route;
            while ((route = br.readLine()) != null) {
                routes.add(route);
            }
            final int totalRoutes = routes.size();
            final AtomicInteger completedRoutes = new AtomicInteger(0);

            for (String r : routes) {
                String fullUrl = targetUrl + r;
                futures.add(executor.submit(() -> checkUrl(fullUrl, foundDirectories, totalRoutes, completedRoutes)));
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
        return new DirectoryScanResult(foundDirectories);
    }

    /**
     * Checks the given URL for a directory
     * 
     * @param urlString The URL to check
     * @param foundDirectories The set of found directories
     * @param total The total number of directories to check
     * @param completed The atomic integer of completed directories
     */
    private void checkUrl(String urlString, Set<String> foundDirectories, int total, AtomicInteger completed) {
        try {
            HttpURLConnection connection = HttpClientManager.getConnection(urlString);
            connection.setRequestMethod("GET");
            if (connection.getResponseCode() >= 200 && connection.getResponseCode() < 400) {
                synchronized (foundDirectories) {
                    foundDirectories.add(urlString);
                }
            }
        } catch (IOException e) {
            Logger.printWarning("Failed to check directory: " + urlString, e.getMessage());
        } finally {
            int current = completed.incrementAndGet();
            Logger.printProgress("Scanning directories: " + current + "/" + total);
        }
    }

    /**
     * The result of the directory scan analysis
     */
    public static class DirectoryScanResult implements AnalysisResult {
        private final Set<String> foundDirectories;

        public DirectoryScanResult(Set<String> foundDirectories) {
            this.foundDirectories = foundDirectories;
        }

        @Override
        public void print() {
            if (!foundDirectories.isEmpty()) {
                Logger.printSpacer();
                Logger.log("Found Routes");
                Logger.printSpacer();
                foundDirectories.forEach(dir -> Logger.printSuccess("  - Found", dir));
            }
        }
    }
}
