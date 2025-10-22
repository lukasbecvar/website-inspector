
package xyz.becvar.websiteinspector.modules;

import xyz.becvar.websiteinspector.utils.Logger;
import xyz.becvar.websiteinspector.utils.WebsiteUtils;
import xyz.becvar.websiteinspector.core.AnalysisResult;
import xyz.becvar.websiteinspector.core.AnalysisModule;

/**
 * This class implements well-known analysis module
 */
public class WellKnownScanner implements AnalysisModule {

    @Override
    public String getName() {
        return "Well-Known Scanner";
    }

    /**
     * Runs the Well-Known analysis for the given target URL
     *
     * @param targetUrl The URL to analyze
     *
     * @return An AnalysisResult object containing the findings
     */
    @Override
    public AnalysisResult analyze(String targetUrl) {
        String securityTxtContent = getWellKnownFileContent(targetUrl, "security.txt");
        String gpcJsonContent = getWellKnownFileContent(targetUrl, "gpc.json");
        String dntPolicyContent = getWellKnownFileContent(targetUrl, "dnt-policy.txt");
        String changePasswordContent = getWellKnownFileContent(targetUrl, "change-password");
        String assetLinksContent = getWellKnownFileContent(targetUrl, "assetlinks.json");
        String appleAppSiteAssociationContent = getWellKnownFileContent(targetUrl, "apple-app-site-association");

        if (securityTxtContent == null && gpcJsonContent == null && dntPolicyContent == null && changePasswordContent == null && assetLinksContent == null && appleAppSiteAssociationContent == null) {
            return null;
        }

        return new WellKnownScannerResult(securityTxtContent, gpcJsonContent, dntPolicyContent, changePasswordContent, assetLinksContent, appleAppSiteAssociationContent);
    }

    /**
     * Analyzes a well-known file for the given URL
     *
     * @param url The URL to analyze
     * @param fileName The name of the file to analyze
     *
     * @return A string containing the content of the file
     */
    private String getWellKnownFileContent(String url, String fileName) {
        String fileUrl = url.endsWith("/") ? url + ".well-known/" + fileName : url + "/.well-known/" + fileName;
        String content = WebsiteUtils.downloadFileContent(fileUrl);
        if (content == null || content.isEmpty()) {
            return null;
        }
        return content;
    }

    /**
     * The result of the Well-Known analysis
     */
    public static class WellKnownScannerResult implements AnalysisResult {
        private final String securityTxtContent;
        private final String gpcJsonContent;
        private final String dntPolicyContent;
        private final String changePasswordContent;
        private final String assetLinksContent;
        private final String appleAppSiteAssociationContent;

        public WellKnownScannerResult(String securityTxtContent, String gpcJsonContent, String dntPolicyContent, String changePasswordContent, String assetLinksContent, String appleAppSiteAssociationContent) {
            this.securityTxtContent = securityTxtContent;
            this.gpcJsonContent = gpcJsonContent;
            this.dntPolicyContent = dntPolicyContent;
            this.changePasswordContent = changePasswordContent;
            this.assetLinksContent = assetLinksContent;
            this.appleAppSiteAssociationContent = appleAppSiteAssociationContent;
        }

        @Override
        public void print() {
            if (securityTxtContent != null) {
                Logger.printSpacer();
                Logger.log("security.txt found");
                Logger.printSpacer();
                Logger.rawLog(securityTxtContent);
            }
            if (gpcJsonContent != null) {
                Logger.printSpacer();
                Logger.log("gpc.json found");
                Logger.printSpacer();
                Logger.rawLog(gpcJsonContent);
            }
            if (dntPolicyContent != null) {
                Logger.printSpacer();
                Logger.log("dnt-policy.txt found");
                Logger.printSpacer();
                Logger.rawLog(dntPolicyContent);
            }
            if (changePasswordContent != null) {
                Logger.printSpacer();
                Logger.log("change-password found");
                Logger.printSpacer();
                Logger.rawLog(changePasswordContent);
            }
            if (assetLinksContent != null) {
                Logger.printSpacer();
                Logger.log("assetlinks.json found");
                Logger.printSpacer();
                Logger.rawLog(assetLinksContent);
            }
            if (appleAppSiteAssociationContent != null) {
                Logger.printSpacer();
                Logger.log("apple-app-site-association found");
                Logger.printSpacer();
                Logger.rawLog(appleAppSiteAssociationContent);
            }
        }
    }
}
