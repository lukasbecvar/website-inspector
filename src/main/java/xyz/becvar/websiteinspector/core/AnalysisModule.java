package xyz.becvar.websiteinspector.core;

public interface AnalysisModule {

    /**
     * @return The name of the analysis module
     */
    String getName();

    /**
     * Runs the analysis for the given target URL
     * 
     * @param targetUrl The URL to analyze
     * 
     * @return An AnalysisResult object containing the findings
     */
    AnalysisResult analyze(String targetUrl);
}
