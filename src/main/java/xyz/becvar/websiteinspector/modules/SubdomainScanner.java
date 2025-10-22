package xyz.becvar.websiteinspector.modules;

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
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import xyz.becvar.websiteinspector.core.Config;
import xyz.becvar.websiteinspector.utils.Logger;
import java.util.concurrent.atomic.AtomicInteger;
import xyz.becvar.websiteinspector.utils.StringUtils;
import xyz.becvar.websiteinspector.core.AnalysisModule;
import xyz.becvar.websiteinspector.core.AnalysisResult;
import xyz.becvar.websiteinspector.utils.HttpClientManager;

/**
 * Class SubdomainScanner
 *
 * This module implements the subdomain scan analysis
 *
 * @package xyz.becvar.websiteinspector.modules
 */
public class SubdomainScanner implements AnalysisModule {

    private String subdomainsFilePath;

    @Override
    public String getName() {
        return "Subdomain Scan";
    }

    public SubdomainScanner(String subdomainsFilePath) {
        this.subdomainsFilePath = subdomainsFilePath;
    }

    private InputStream getSubdomainsInputStream() throws IOException {
        if (subdomainsFilePath != null) {
            return new java.io.FileInputStream(subdomainsFilePath);
        } else {
            return Main.class.getResourceAsStream("/subdomains.txt");
        }
    }

    /**
     * Runs the subdomain scan analysis for the given target URL
     * 
     * @param targetUrl The URL to analyze
     * 
     * @return An AnalysisResult object containing the findings
     */
    @Override
    public AnalysisResult analyze(String targetUrl) {
        Set<String> foundSubdomains = new HashSet<>();
        ExecutorService executor = Executors.newFixedThreadPool(Config.SCANNER_THREAD_POOL_SIZE);
        List<Future<?>> futures = new ArrayList<>();
        String baseDomain = targetUrl.replaceAll("^(http[s]?://)", "").replaceAll("/$", "");

        boolean scanHttp = !detectGlobalHttpRedirect(baseDomain);

        try (InputStream inputStream = getSubdomainsInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            List<String> subdomains = new ArrayList<>();
            String subdomain;
            while ((subdomain = br.readLine()) != null) {
                if (subdomain.trim().isEmpty()) continue;
                subdomains.add(subdomain);
            }

            final int totalSubdomains = scanHttp ? subdomains.size() * 2 : subdomains.size();
            final AtomicInteger completedSubdomains = new AtomicInteger(0);

            for (String s : subdomains) {
                if (scanHttp) {
                    futures.add(executor.submit(() -> checkUrl("http://" + s + "." + baseDomain, foundSubdomains, totalSubdomains, completedSubdomains)));
                }
                futures.add(executor.submit(() -> checkUrl("https://" + s + "." + baseDomain, foundSubdomains, totalSubdomains, completedSubdomains)));
            }

        } catch (IOException e) {
            Logger.printError("Failed to read subdomains.txt: " + e.getMessage());
        }

        executor.shutdown();
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                Logger.printError("Failed to check subdomain: " + e.getMessage());
            }
        }
        Logger.clearProgress();
        return new SubdomainScanResult(foundSubdomains);
    }

    /**
     * Checks the given URL for a subdomain redirect
     * 
     * @param urlString The URL to check
     * @param foundSubdomains The set of found subdomains
     * @param total The total number of subdomains to check
     * @param completed The atomic integer of completed subdomains
     */
    private void checkUrl(String urlString, Set<String> foundSubdomains, int total, AtomicInteger completed) {
        HttpURLConnection connection = null;
        try {
            connection = HttpClientManager.getConnection(urlString);
            connection.setRequestMethod("GET");
            if (connection.getResponseCode() >= 200 && connection.getResponseCode() < 400) {
                synchronized (foundSubdomains) {
                    foundSubdomains.add(urlString);
                }
            }
        } catch (IOException ignored) {
        } finally {
            if (connection != null) connection.disconnect();
            int current = completed.incrementAndGet();
            Logger.printProgress("Scanning subdomains: " + current + "/" + total);
        }
    }

    /**
     * Detects if the base domain redirects to a global HTTP URL
     * 
     * @param baseDomain The base domain to check
     * 
     * @return True if the base domain redirects to a global HTTP URL, false otherwise
     */
    private boolean detectGlobalHttpRedirect(String baseDomain) {
        Logger.logStatus("Checking for global HTTP -> HTTPS redirect...");
        for (int i = 0; i < 5; i++) {
            String randomSub = StringUtils.generateRandomString(10);
            String testUrl = "http://" + randomSub + "." + baseDomain;
            String expectedLocation = "https://" + randomSub + "." + baseDomain;

            HttpURLConnection connection = null;
            try {
                connection = HttpClientManager.getConnection(testUrl);
                connection.setInstanceFollowRedirects(false);
                connection.setRequestMethod("HEAD");

                int responseCode = connection.getResponseCode();
                String locationHeader = connection.getHeaderField("Location");

                if (responseCode != HttpURLConnection.HTTP_MOVED_PERM || locationHeader == null || !locationHeader.startsWith(expectedLocation)) {
                    Logger.logStatus("No global redirect detected.");
                    return false;
                }
            } catch (IOException e) {
                Logger.logStatus("No global redirect detected (request failed).");
                return false;
            } finally {
                if (connection != null) connection.disconnect();
            }
        }
        Logger.logStatus("Global HTTP -> HTTPS redirect detected. Scanning HTTPS only.");
        return true;
    }

    /**
     * The result of the subdomain scan analysis
     */
    public static class SubdomainScanResult implements AnalysisResult {
        private final Set<String> foundSubdomains;
        public SubdomainScanResult(Set<String> foundSubdomains) {
            this.foundSubdomains = new HashSet<>(foundSubdomains);
        }

        @Override
        public void print() {
            if (!foundSubdomains.isEmpty()) {
                Logger.printSpacer();
                Logger.log("Found Subdomains");
                Logger.printSpacer();
                foundSubdomains.forEach(sub -> Logger.printSuccess("  - Found", sub));
            }
        }
    }
}
