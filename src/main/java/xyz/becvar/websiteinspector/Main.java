package xyz.becvar.websiteinspector;

import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Future;
import xyz.becvar.websiteinspector.utils.Logger;
import xyz.becvar.websiteinspector.modules.ServerInfo;
import xyz.becvar.websiteinspector.modules.SiteMapInfo;
import xyz.becvar.websiteinspector.modules.DirectoryScanner;
import xyz.becvar.websiteinspector.modules.SubdomainScanner;

public class Main
{
    // define global variables
    public static final int SCANNER_THREAD_POOL_SIZE = 30;
    public static final int CONNECTION_TIMEOUT = 3;
    public static final String APP_PREFIX = "WI";
    public static final String USER_AGENT = "website-inspector (becvar.xyz)";

    public static void main(String[] args)
    {
        String url = null;

        // init scanner instance
        Scanner scanner = new Scanner(System.in);

        if (args.length > 0) {
            url = args[0];
        } else {
            Logger.prompt("Enter URL");

            // get url from user input
            url = scanner.nextLine();
        }

        // validate url
        url = Validator.validateUrl(url);

        // scan web directories routes
        Logger.printSpacer();
        Logger.log("Directory Scan");
        Logger.printSpacer();

        // scan routes and wait for completion
        List<Future<?>> directoryFutures = DirectoryScanner.scanRoutes(url);
        Validator.waitForCompletion(directoryFutures);
        Logger.clearProgress(); // Clear progress after directory scan

        // scan subdomains
        Logger.printSpacer();
        Logger.log("Subdomain Scan");
        Logger.printSpacer();

        // scan subdomains and wait for completion
        List<Future<?>> subdomainFutures = SubdomainScanner.scanSubdomains(url);
        Validator.waitForCompletion(subdomainFutures);
        Logger.clearProgress(); // Clear progress after subdomain scan

        // print server info title header
        Logger.printSpacer();
        Logger.log("Server Info");
        Logger.printSpacer();

        // print server info
        ServerInfo.printServerInfo(url);

        // print robots.txt summary
        Logger.printSpacer();
        Logger.log("Robots.txt Summary");
        Logger.printSpacer();
        String robotsSummary = SiteMapInfo.getRobotsTxtAnalyze(url);
        Logger.rawLog(robotsSummary);

        // print sitemap.xml summary
        Logger.printSpacer();
        Logger.log("Sitemap.xml Summary");
        Logger.printSpacer();
        String sitemapSummary = SiteMapInfo.getSitemapAnalyze(url);
        Logger.rawLog(sitemapSummary);

        // print results
        Logger.printSpacer();
        Logger.log("Scan Results");
        Logger.printSpacer();

        // print results
        if (DirectoryScanner.getFoundDirectories().isEmpty() && SubdomainScanner.getFoundSubdomains().isEmpty()) {
            Logger.log("No results found.");
            return;
        } else {
            DirectoryScanner.getFoundDirectories().forEach(Logger::log);
            SubdomainScanner.getFoundSubdomains().forEach(Logger::log);
        }

        // print ending spacer
        Logger.printSpacer();

        // exit app after printing results
        System.exit(0);
    }
}
