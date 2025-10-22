package xyz.becvar.websiteinspector.modules;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.io.IOException;
import java.net.HttpURLConnection;
import xyz.becvar.websiteinspector.utils.Logger;
import xyz.becvar.websiteinspector.utils.WebsiteUtils;
import xyz.becvar.websiteinspector.core.AnalysisModule;
import xyz.becvar.websiteinspector.core.AnalysisResult;
import xyz.becvar.websiteinspector.utils.HttpClientManager;

/**
 * Class ProfilerDetector
 *
 * This module detects profilers on website
 *
 * @package xyz.becvar.websiteinspector.modules
 */
public class ProfilerDetector implements AnalysisModule {

    @Override
    public String getName() {
        return "Profiler Detector";
    }

    private static final Map<String, String> PROFILER_PATHS = new HashMap<>();
    private static final Map<String, String> PROFILER_HEADERS = new HashMap<>();
    private static final Map<String, List<String>> PROFILER_CONTENT = new HashMap<>();

    static {
        PROFILER_PATHS.put("Laravel Telescope", "/telescope");
        PROFILER_PATHS.put("Django Debug Toolbar", "/__debug__/");
        PROFILER_PATHS.put("Flask-DebugToolbar", "/__debug__/");

        // Nette Tracy profiler
        PROFILER_CONTENT.put("Nette Tracy", List.of(
                "<!-- Tracy Debug Bar -->",
                "_tracy_bar=js",
                "tracy-session"
        ));
    }

    /**
     * Analyzes the given target URL for profilers
     * 
     * @param targetUrl The target URL to analyze
     * 
     * @return A ProfilerResult object containing the found profilers
     */
    @Override
    public AnalysisResult analyze(String targetUrl) {
        List<String> foundProfilers = new ArrayList<>();

        // Check for profilers based on URL paths
        for (Map.Entry<String, String> entry : PROFILER_PATHS.entrySet()) {
            String profilerName = entry.getKey();
            String path = entry.getValue();
            String url = targetUrl + path;
            HttpURLConnection connection = null;
            try {
                connection = HttpClientManager.getConnection(url);
                connection.setInstanceFollowRedirects(true);
                connection.setRequestMethod("HEAD");
                int responseCode = connection.getResponseCode();
                if (responseCode >= 200 && responseCode < 400) {
                    if (!foundProfilers.contains(profilerName)) {
                        foundProfilers.add(profilerName);
                    }
                }
            } catch (IOException e) {
                Logger.printWarning("Failed to check for profiler: " + url, e.getMessage());
            } finally {
                if (connection != null) connection.disconnect();
            }
        }

        // Check for profilers based on headers
        try {
            HttpURLConnection connection = HttpClientManager.getConnection(targetUrl);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestMethod("GET");
            Map<String, List<String>> headers = connection.getHeaderFields();
            for (Map.Entry<String, String> entry : PROFILER_HEADERS.entrySet()) {
                String profilerName = entry.getKey();
                String header = entry.getValue();
                String[] headerParts = header.split(": ");
                String headerName = headerParts[0];
                String headerValue = headerParts[1];
                if (headers.containsKey(headerName)) {
                    for (String value : headers.get(headerName)) {
                        if (value.contains(headerValue)) {
                            if (!foundProfilers.contains(profilerName)) {
                                foundProfilers.add(profilerName);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            Logger.printWarning("Failed to check for profiler headers", e.getMessage());
        }

        // Check for profilers based on page content
        String html = WebsiteUtils.getHtml(targetUrl);
        if (html != null) {
            for (Map.Entry<String, List<String>> entry : PROFILER_CONTENT.entrySet()) {
                for (String content : entry.getValue()) {
                    if (html.contains(content)) {
                        foundProfilers.add(entry.getKey());
                    }
                }
            }
        }

        // If no profiler is found yet, check for Symfony profiler with 500 error
        if (foundProfilers.isEmpty()) {
            String url = targetUrl + "/_profiler";
            try {
                HttpURLConnection connection = HttpClientManager.getConnection(url);
                connection.setInstanceFollowRedirects(true);
                connection.setRequestMethod("HEAD");
                int responseCode = connection.getResponseCode();
                if (responseCode == 500) {
                    foundProfilers.add("Symfony Profiler " + url);
                }
            } catch (IOException e) {
                Logger.printWarning("Failed to check for Symfony profiler", e.getMessage());
            }
        }

        return new ProfilerResult(foundProfilers);
    }

    /**
     * This class represents the result of the profiler detection 
     */
    public static class ProfilerResult implements AnalysisResult {
        private final List<String> foundProfilers;
        public ProfilerResult(List<String> foundProfilers) {
            this.foundProfilers = new ArrayList<>(foundProfilers);
        }

        @Override
        public void print() {
            if (!foundProfilers.isEmpty()) {
                Logger.printSpacer();
                Logger.log("Profilers Found");
                Logger.printSpacer();
                for (String profiler : foundProfilers) {
                    Logger.printWarning(profiler, "");
                }
            }
        }
    }
}
