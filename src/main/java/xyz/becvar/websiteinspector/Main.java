package xyz.becvar.websiteinspector;

import xyz.becvar.websiteinspector.core.AnalysisModule;
import xyz.becvar.websiteinspector.core.AnalysisResult;
import xyz.becvar.websiteinspector.modules.*;
import xyz.becvar.websiteinspector.utils.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.stream.Collectors;

public class Main {
    public static final int SCANNER_THREAD_POOL_SIZE = 30;
    public static final int CONNECTION_TIMEOUT = 3;
    public static final String APP_PREFIX = "WI";
    public static final String USER_AGENT = "website-inspector (becvar.xyz)";

    public static void main(String[] args) {

        List<String> argsList = new ArrayList<>(Arrays.asList(args));
        final boolean isFileLoggingEnabled = !argsList.contains("--no-file-log");
        argsList.remove("--no-file-log");

        String initialUrl = getUrl(argsList.toArray(new String[0]));
        String validatedUrl = Validator.validateUrl(initialUrl);

        if (validatedUrl == null) {
            Logger.printError("URL could not be validated. Exiting.");
            return;
        }

        final String finalUrl = validatedUrl;

        try {
            // Initialize file logging if enabled
            if (isFileLoggingEnabled) {
                String domain = new java.net.URL(finalUrl).getHost();
                Logger.initFileLogging(domain);
            }

            List<AnalysisModule> modules = new ArrayList<>();
            modules.add(new ServerInfo());
            modules.add(new SiteMapInfo());

            boolean pathCatchAll = CatchAllDetector.isPathCatchAllActive(finalUrl);
            if (!pathCatchAll) {
                modules.add(new DirectoryScanner());
            }

            boolean subdomainCatchAll = CatchAllDetector.isSubdomainCatchAllActive(finalUrl);
            if (!subdomainCatchAll) {
                modules.add(new SubdomainScanner());
            }

            // --- Run Analysis ---
            Logger.logStatus("Analysis modules prepared. Starting scan...");
            List<AnalysisResult> results = modules.stream()
                    .map(module -> {
                        Logger.logStatus("Running Module: " + module.getName());
                        return module.analyze(finalUrl);
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            // --- Print Final Report ---
            Logger.logStatus("--- FINAL ANALYSIS REPORT ---");

            // Print in desired order
            printResult(results, ServerInfo.ServerInfoResult.class);
            printResult(results, DirectoryScanner.DirectoryScanResult.class);
            printResult(results, SubdomainScanner.SubdomainScanResult.class);
            printResult(results, SiteMapInfo.SiteMapInfoResult.class);

            if (pathCatchAll) {
                Logger.printWarning("Path Catch-All Detected", "Directory scan was skipped.");
            }
            if (subdomainCatchAll) {
                Logger.printWarning("Subdomain Catch-All Detected", "Subdomain scan was skipped.");
            }
            Logger.printSpacer();

        } catch (Exception e) {
            Logger.printError("An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
        } finally {
            Logger.closeFileLogging();
            System.exit(0);
        }
    }

    private static void printResult(List<AnalysisResult> results, Class<?> resultType) {
        results.stream()
                .filter(resultType::isInstance)
                .findFirst()
                .ifPresent(AnalysisResult::print);
    }

    private static String getUrl(String[] args) {
        if (args.length > 0) {
            return args[0];
        }
        Logger.prompt("Enter URL");
        Scanner scanner = new Scanner(System.in);
        return scanner.nextLine();
    }
}
