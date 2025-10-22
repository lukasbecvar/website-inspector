package xyz.becvar.websiteinspector.modules;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.io.IOException;
import java.net.HttpURLConnection;
import xyz.becvar.websiteinspector.utils.Logger;
import xyz.becvar.websiteinspector.core.AnalysisModule;
import xyz.becvar.websiteinspector.core.AnalysisResult;
import xyz.becvar.websiteinspector.utils.HttpClientManager;

/**
 * Class AdminPanelDetector
 *
 * This module detects system panels on a website
 *
 * @package xyz.becvar.websiteinspector.modules
 */
public class AdminPanelDetector implements AnalysisModule {

    @Override
    public String getName() {
        return "Admin Panel Detector";
    }

    /**
     * Admin systems definitions
     */
    private static final Map<String, String[]> ADMIN_PANELS = new HashMap<>();
    static {
        ADMIN_PANELS.put("phpMyAdmin", new String[]{"/phpmyadmin/", "/pma/", "/myadmin/", "/dbadmin/"});
        ADMIN_PANELS.put("Adminer", new String[]{"/adminer.php", "/adminer/"});
        ADMIN_PANELS.put("Redis Commander", new String[]{"/redis-commander/"});
        ADMIN_PANELS.put("Mongo Express", new String[]{"/mongo-express/"});
        ADMIN_PANELS.put("phpPgAdmin", new String[]{"/phppgadmin/"});
        ADMIN_PANELS.put("Jenkins", new String[]{"/jenkins/login"});
        ADMIN_PANELS.put("Joomla", new String[]{"/administrator/"});
        ADMIN_PANELS.put("GitLab", new String[]{"/users/sign_in"});
        ADMIN_PANELS.put("WordPress", new String[]{"/wp-admin/"});
        ADMIN_PANELS.put("Drupal", new String[]{"/user/login"});
        ADMIN_PANELS.put("pgAdmin", new String[]{"/pgadmin/"});
        ADMIN_PANELS.put("Prometheus", new String[]{"/graph"});
        ADMIN_PANELS.put("cPanel", new String[]{"/cpanel"});
        ADMIN_PANELS.put("Kibana", new String[]{"/kibana/"});
    }

    /**
     * Analyzes the given target URL for admin panels
     * 
     * @param targetUrl The target URL to analyze
     * 
     * @return List of found panels
     */
    @Override
    public AnalysisResult analyze(String targetUrl) {
        List<String> foundPanels = new ArrayList<>();

        if (!CatchAllDetector.isPathCatchAllActive(targetUrl)) {
            // Check for panels based on URL paths
            for (Map.Entry<String, String[]> entry : ADMIN_PANELS.entrySet()) {
                String panelName = entry.getKey();
                for (String path : entry.getValue()) {
                    String url = targetUrl.replaceAll("/+$", "") + path;
                    HttpURLConnection connection = null;
                    try {
                        connection = HttpClientManager.getConnection(url);
                        connection.setRequestMethod("HEAD");
                        int responseCode = connection.getResponseCode();
                        if (responseCode >= 200 && responseCode < 300) {
                            foundPanels.add(panelName + ": " + url);
                        }
                    } catch (IOException e) {
                        Logger.printWarning("Failed to check for admin panel: " + url, e.getMessage());
                    } finally {
                        if (connection != null) {
                            connection.disconnect();
                        }
                    }
                }
            }
        }

        return new AdminPanelResult(foundPanels);
    }

    /**
     * This class represents the result of the admin panel detection
     */
    public static class AdminPanelResult implements AnalysisResult {
        private final List<String> foundPanels;
        public AdminPanelResult(List<String> foundPanels) {
            this.foundPanels = new ArrayList<>(foundPanels);
        }

        @Override
        public void print() {
            if (!foundPanels.isEmpty()) {
                Logger.printSpacer();
                Logger.log("Admin Panels Found");
                Logger.printSpacer();
                for (String panel : foundPanels) {
                    Logger.printSuccess(panel, "");
                }
            }
        }
    }
}
