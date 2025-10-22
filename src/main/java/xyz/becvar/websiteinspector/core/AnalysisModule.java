package xyz.becvar.websiteinspector.core;

/**
 * Interface AnalysisModule
 *
 * Basic template for all app modules
 *
 * @package xyz.becvar.websiteinspector.core
 */
public interface AnalysisModule {

    /**
     * Get module name
     *
     * @return The name of the analysis module
     */
    String getName();

    /**
     * Runs the analysis for the given target URL
     * 
     * @param targetUrl The URL to analyze
     * 
     * @return An scan result
     */
    AnalysisResult analyze(String targetUrl);
}
