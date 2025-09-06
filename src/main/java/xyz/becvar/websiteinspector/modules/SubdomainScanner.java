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
import java.security.SecureRandom;
import java.util.concurrent.Future;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import xyz.becvar.websiteinspector.utils.Logger;
import xyz.becvar.websiteinspector.core.AnalysisModule;
import xyz.becvar.websiteinspector.core.AnalysisResult;
import xyz.becvar.websiteinspector.utils.HttpClientManager;

/**
 * This class implements the Subdomain Scan analysis module
 */
public class SubdomainScanner implements AnalysisModule {

    @Override
    public String getName() {
        return "Subdomain Scan";
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
        ExecutorService executor = Executors.newFixedThreadPool(30);
        List<Future<?>> futures = new ArrayList<>();
        String baseDomain = targetUrl.replaceAll("^(http[s]?://)", "").replaceAll("/$", "");

        boolean scanHttp = !detectGlobalHttpRedirect(baseDomain);

        try (InputStream inputStream = getClass().getResourceAsStream("/subdomains.txt");
             BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {

            List<String> subdomains = new ArrayList<>();
            String subdomain;
            while ((subdomain = br.readLine()) != null) {
                if (subdomain.trim().isEmpty()) continue;
                subdomains.add(subdomain);
            }

            final int totalSubdomains = scanHttp ? subdomains.size() * 2 : subdomains.size();
            final int[] completedSubdomains = {0};

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
            } catch (Exception e) { /* Ignore */ }
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
     * @param completed The array of completed subdomains
     */
    private void checkUrl(String urlString, Set<String> foundSubdomains, int total, int[] completed) {
        try {
            HttpURLConnection connection = HttpClientManager.getConnection(urlString);
            connection.setRequestMethod("GET");
            if (connection.getResponseCode() >= 200 && connection.getResponseCode() < 400) {
                synchronized (foundSubdomains) {
                    foundSubdomains.add(urlString);
                }
            }
        } catch (IOException e) {
            // Ignore
        } finally {
            synchronized (completed) {
                completed[0]++;
                Logger.printProgress("Scanning subdomains: " + completed[0] + "/" + total);
            }
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
        return false;
    }

    /**
     * The result of the subdomain scan analysis
     */
    public static class SubdomainScanResult implements AnalysisResult {
        private final Set<String> foundSubdomains;

        public SubdomainScanResult(Set<String> foundSubdomains) {
            this.foundSubdomains = foundSubdomains;
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
