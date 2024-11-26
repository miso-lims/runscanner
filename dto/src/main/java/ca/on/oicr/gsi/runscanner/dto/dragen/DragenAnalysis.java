package ca.on.oicr.gsi.runscanner.dto.dragen;

import ca.on.oicr.gsi.runscanner.dto.Analysis;
import ca.on.oicr.gsi.runscanner.dto.dragen.samplesheet.Samplesheet;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;

// Represents an entire attempt at a DRAGEN Pipeline
public class DragenAnalysis implements Analysis {
  Samplesheet samplesheet;
  Map<String, DragenWorkflowAnalysis> analyses = new HashMap<>();
  static ObjectMapper mapper;

  public DragenAnalysis(ObjectMapper m, Samplesheet s) {
    mapper = m;
    samplesheet = s;
  }

  public DragenWorkflowAnalysis get(String workflowName) {
    if (analyses.containsKey(workflowName)) {
      return analyses.get(workflowName);
    } else {
      return new DragenWorkflowAnalysis();
    }
  }

  public void put(String name, DragenWorkflowAnalysis dwa) {
    analyses.put(name, dwa);
  }
}
