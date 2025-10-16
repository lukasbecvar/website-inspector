package xyz.becvar.websiteinspector;

import java.util.List;
import java.util.Arrays;
import java.util.Objects;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.nio.charset.StandardCharsets;
import xyz.becvar.websiteinspector.modules.*;
import xyz.becvar.websiteinspector.OutputFormat;
import xyz.becvar.websiteinspector.utils.Logger;
import xyz.becvar.websiteinspector.HelpComponent;
import xyz.becvar.websiteinspector.core.AnalysisResult;
import xyz.becvar.websiteinspector.core.AnalysisModule;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * This is the main class of the website-inspector application
 */
public class Main {

    /**
     * The main method of the application
     *
     * @param args The command-line arguments
     */
    @SuppressFBWarnings("REC_CATCH_EXCEPTION")
    public static void main(String[] args) {
        
        // help print trigger
        for (String arg : args) {
            if (arg.equalsIgnoreCase("-h") || arg.equalsIgnoreCase("--help")) {
                HelpComponent.printHelp();
                return;
            }
        }

        // Parse command-line arguments
        List<String> argsList = new ArrayList<>(Arrays.asList(args));

        // Extract URL first
        String initialUrl = null;
        int urlIndex = -1;
        for (int i = 0; i < argsList.size(); i++) {
            if (!argsList.get(i).startsWith("--")) {
                initialUrl = argsList.get(i);
                urlIndex = i;
                break;
            }
        }

        if (initialUrl != null) {
            argsList.remove(urlIndex);
        } else {
            Logger.prompt("Enter URL");
            Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);
            initialUrl = scanner.nextLine();
        }

        final boolean isFileLoggingEnabled = !argsList.contains("--no-file-log");
        argsList.remove("--no-file-log");

        OutputFormat outputFormat = OutputFormat.NORMAL;
        String outputArg = argsList.stream()
            .filter(arg -> arg.startsWith("--output="))
            .findFirst()
            .orElse(null);

        if (outputArg != null) {
            String format = outputArg.split("=")[1];
            try {
                outputFormat = OutputFormat.valueOf(format.toUpperCase(java.util.Locale.ROOT));
            } catch (IllegalArgumentException e) {
                Logger.printError("Invalid output format: " + format + ". Using NORMAL.");
            }
            argsList.remove(outputArg);
        }
        Logger.setOutputFormat(outputFormat);

        String subdomainsFilePath = null;
        String routesFilePath = null;

        // Parse --subdomains-file argument
        String subdomainsFileArg = argsList.stream()
            .filter(arg -> arg.startsWith("--subdomains-file="))
            .findFirst()
            .orElse(null);
        if (subdomainsFileArg != null) {
            subdomainsFilePath = subdomainsFileArg.split("=")[1];
            argsList.remove(subdomainsFileArg);
            if (outputFormat == OutputFormat.NORMAL) {
                Logger.logStatus("Using custom subdomains wordlist: " + subdomainsFilePath);
            }
        } else {
            if (outputFormat == OutputFormat.NORMAL) {
                Logger.logStatus("Using default subdomains wordlist.");
            }
        }

        // Parse --routes-file argument
        String routesFileArg = argsList.stream()
            .filter(arg -> arg.startsWith("--routes-file="))
            .findFirst()
            .orElse(null);
        if (routesFileArg != null) {
            routesFilePath = routesFileArg.split("=")[1];
            argsList.remove(routesFileArg);
            if (outputFormat == OutputFormat.NORMAL) {
                Logger.logStatus("Using custom routes wordlist: " + routesFilePath);
            }
        } else {
            if (outputFormat == OutputFormat.NORMAL) {
                Logger.logStatus("Using default routes wordlist.");
            }
        }

        // Validate URL
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
                modules.add(new DirectoryScanner(routesFilePath));
            }

            // run subdomain scan module
            boolean subdomainCatchAll = CatchAllDetector.isSubdomainCatchAllActive(finalUrl);
            if (!subdomainCatchAll) {
                modules.add(new SubdomainScanner(subdomainsFilePath));
            }

            // --- Run analysis ---
            Logger.logStatus("Analysis modules prepared. Starting scan...");
            List<AnalysisResult> results = modules.stream()
                    .map(module -> {
                        Logger.logStatus("Running Module: " + module.getName());
                        AnalysisResult result = module.analyze(finalUrl);
                        Logger.addAnalysisResult(result);
                        return result;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            // --- Print final report ---
            if (outputFormat == OutputFormat.NORMAL) {
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
            } else if (outputFormat == OutputFormat.JSON) {
                Logger.printJsonReport();
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
}
