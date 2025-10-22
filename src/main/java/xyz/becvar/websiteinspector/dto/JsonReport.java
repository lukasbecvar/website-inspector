package xyz.becvar.websiteinspector.dto;

import java.util.List;
import java.util.ArrayList;
import xyz.becvar.websiteinspector.core.AnalysisResult;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Class JsonReport
 *
 * JSON report for the scan results
 *
 * @package xyz.becvar.websiteinspector.dto
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
