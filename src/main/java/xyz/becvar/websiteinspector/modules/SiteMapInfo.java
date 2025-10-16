package xyz.becvar.websiteinspector.modules;

import xyz.becvar.websiteinspector.utils.Logger;
import xyz.becvar.websiteinspector.utils.WebsiteUtils;
import xyz.becvar.websiteinspector.core.AnalysisResult;
import xyz.becvar.websiteinspector.core.AnalysisModule;

/**
 * This class implements the Sitemap & Robots.txt analysis module
 */
public class SiteMapInfo implements AnalysisModule {

    @Override
    public String getName() {
        return "Sitemap & Robots.txt";
    }

    /**
     * Runs the Sitemap & Robots.txt analysis for the given target URL
     * 
     * @param targetUrl The URL to analyze
     * 
     * @return An AnalysisResult object containing the findings
     */
    @Override
    public AnalysisResult analyze(String targetUrl) {
        String robotsSummary = getRobotsTxtAnalyze(targetUrl);
        String sitemapSummary = getSitemapAnalyze(targetUrl);
        return new SiteMapInfoResult(robotsSummary, sitemapSummary);
    }

    /**
     * Analyzes the robots.txt file for the given URL
     * 
     * @param url The URL to analyze
     * 
     * @return A string containing the summary of the robots.txt file
     */
    private String getRobotsTxtAnalyze(String url) {
        String robotsUrl = url.endsWith("/") ? url + "robots.txt" : url + "/robots.txt";
        String content = WebsiteUtils.downloadFileContent(robotsUrl);
        if (content == null || content.isEmpty()) {
            return "robots.txt file not found or is empty.";
        }
        return content;
    }

    /**
     * Analyzes the sitemap.xml file for the given URL
     * 
     * @param url The URL to analyze
     * 
     * @return A string containing the summary of the sitemap.xml file
     */
    private String getSitemapAnalyze(String url) {
        String sitemapUrl = url.endsWith("/") ? url + "sitemap.xml" : url + "/sitemap.xml";
        String content = WebsiteUtils.downloadFileContent(sitemapUrl);
        if (content == null || content.isEmpty()) {
            return "sitemap.xml file not found or is empty.";
        }
        return content;
    }

    /**
     * The result of the Sitemap & Robots.txt analysis
     */
    public static class SiteMapInfoResult implements AnalysisResult {
        private final String robotsSummary;
        private final String sitemapSummary;

        public SiteMapInfoResult(String robotsSummary, String sitemapSummary) {
            this.robotsSummary = robotsSummary;
            this.sitemapSummary = sitemapSummary;
        }

        @Override
        public void print() {
            // robots.txt summary
            Logger.printSpacer();
            Logger.log("Robots.txt Summary");
            Logger.printSpacer();
            Logger.rawLog(robotsSummary);

            // sitemap.xml summary
            Logger.printSpacer();
            Logger.log("Sitemap.xml Summary");
            Logger.printSpacer();
            Logger.rawLog(sitemapSummary);
        }
    }
}
