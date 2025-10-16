package xyz.becvar.websiteinspector;

import java.util.List;
import java.util.Arrays;
import java.util.Objects;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.nio.charset.StandardCharsets;
import xyz.becvar.websiteinspector.modules.*;
import xyz.becvar.websiteinspector.utils.Logger;
import xyz.becvar.websiteinspector.core.AnalysisResult;
import xyz.becvar.websiteinspector.core.AnalysisModule;

/**
 * This is the main class of the website-inspector application
 */
public class Main {

    /**
     * The main method of the application
     * 
     * @param args The command-line arguments
     */
    public static void main(String[] args) {

        // Parse command-line arguments
        List<String> argsList = new ArrayList<>(Arrays.asList(args));
        final boolean isFileLoggingEnabled = !argsList.contains("--no-file-log");
        argsList.remove("--no-file-log");

        // Validate URL
        String initialUrl = getUrl(argsList.toArray(new String[0]));
        String validatedUrl = null;
        try {
            validatedUrl = Validator.validateUrl(initialUrl);
        } catch (RuntimeException e) {
            return; // Exit if validation fails
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
            modules.add(new TlsInfo());
            modules.add(new SiteMapInfo());
            modules.add(new AdminPanelDetector());
            modules.add(new ProfilerDetector());

            // run directory scan module
            boolean pathCatchAll = CatchAllDetector.isPathCatchAllActive(finalUrl);
            if (!pathCatchAll) {
                modules.add(new DirectoryScanner());
            }

            // run subdomain scan module
            boolean subdomainCatchAll = CatchAllDetector.isSubdomainCatchAllActive(finalUrl);
            if (!subdomainCatchAll) {
                modules.add(new SubdomainScanner());
            }

            // --- Run analysis ---
            Logger.logStatus("Analysis modules prepared. Starting scan...");
            List<AnalysisResult> results = modules.stream()
                    .map(module -> {
                        Logger.logStatus("Running Module: " + module.getName());
                        return module.analyze(finalUrl);
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            // --- Print final report ---
            System.out.println("\n");
            Logger.logStatus("--- FINAL ANALYSIS REPORT ---");

            // Print in desired order
            printResult(results, ServerInfo.ServerInfoResult.class);
            printResult(results, TlsInfo.TlsInfoResult.class);
            printResult(results, SiteMapInfo.SiteMapInfoResult.class);
            printResult(results, AdminPanelDetector.AdminPanelResult.class);
            printResult(results, ProfilerDetector.ProfilerResult.class);
            printResult(results, DirectoryScanner.DirectoryScanResult.class);
            printResult(results, SubdomainScanner.SubdomainScanResult.class);

            Logger.printSpacer();
            if (pathCatchAll) {
                Logger.printWarning("Path Catch-All Detected", "Directory scan was skipped.");
            }
            if (subdomainCatchAll) {
                Logger.printWarning("Subdomain Catch-All Detected", "Subdomain scan was skipped.");
            }

        } catch (Exception e) {
            Logger.printError("An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
        } finally {
            Logger.closeFileLogging();
        }
    }

    /**
     * Prints the result of the analysis
     * 
     * @param results The results to print
     * @param resultType The type of result to print
     */
    private static void printResult(List<AnalysisResult> results, Class<?> resultType) {
        results.stream().filter(resultType::isInstance).findFirst().ifPresent(AnalysisResult::print);
    }

    /**
     * Gets the URL from the command-line arguments
     * 
     * @param args The command-line arguments
     * 
     * @return The URL
     */
    private static String getUrl(String[] args) {
        if (args.length > 0) {
            return args[0];
        }
        Logger.prompt("Enter URL");
        Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);
        return scanner.nextLine();
    }
}
