package xyz.becvar.websiteinspector.dto;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import xyz.becvar.websiteinspector.core.AnalysisResult;

import java.util.ArrayList;
import java.util.List;

/**
 * A JSON report for the scan results.
 */
@SuppressFBWarnings("URF_UNREAD_FIELD")
public class JsonReport {
    private final long scanDurationMs;
    private final List<AnalysisResult> results;

    public JsonReport(long scanDurationMs, List<AnalysisResult> results) {
        this.scanDurationMs = scanDurationMs;
        this.results = new ArrayList<>(results);
    }
}