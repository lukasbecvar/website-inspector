package xyz.becvar.websiteinspector.modules;

import xyz.becvar.websiteinspector.core.AnalysisModule;
import xyz.becvar.websiteinspector.core.AnalysisResult;
import xyz.becvar.websiteinspector.utils.Logger;
import xyz.becvar.websiteinspector.utils.WebsiteUtils;

public class SiteMapInfo implements AnalysisModule {

    @Override
    public String getName() {
        return "Sitemap & Robots.txt";
    }

    @Override
    public AnalysisResult analyze(String targetUrl) {
        String robotsSummary = getRobotsTxtAnalyze(targetUrl);
        String sitemapSummary = getSitemapAnalyze(targetUrl);
        return new SiteMapInfoResult(robotsSummary, sitemapSummary);
    }

    private String getRobotsTxtAnalyze(String url) {
        String robotsUrl = url.endsWith("/") ? url + "robots.txt" : url + "/robots.txt";
        String content = WebsiteUtils.downloadFileContent(robotsUrl);
        if (content == null || content.isEmpty()) {
            return "robots.txt file not found or is empty.";
        }
        // ... parsing logic ...
        return content; // Simplified for brevity
    }

    private String getSitemapAnalyze(String url) {
        String sitemapUrl = url.endsWith("/") ? url + "sitemap.xml" : url + "/sitemap.xml";
        String content = WebsiteUtils.downloadFileContent(sitemapUrl);
        if (content == null || content.isEmpty()) {
            return "sitemap.xml file not found or is empty.";
        }
        // ... parsing logic ...
        return content; // Simplified for brevity
    }

    public static class SiteMapInfoResult implements AnalysisResult {
        private final String robotsSummary;
        private final String sitemapSummary;

        public SiteMapInfoResult(String robotsSummary, String sitemapSummary) {
            this.robotsSummary = robotsSummary;
            this.sitemapSummary = sitemapSummary;
        }

        @Override
        public void print() {
            Logger.printSpacer();
            Logger.log("Robots.txt Summary");
            Logger.printSpacer();
            Logger.rawLog(robotsSummary);

            Logger.printSpacer();
            Logger.log("Sitemap.xml Summary");
            Logger.printSpacer();
            Logger.rawLog(sitemapSummary);
        }
    }
}
