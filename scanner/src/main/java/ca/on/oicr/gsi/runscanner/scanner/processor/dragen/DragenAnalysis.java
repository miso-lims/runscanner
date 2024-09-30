package ca.on.oicr.gsi.runscanner.scanner.processor.dragen;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

// Represents an entire attempt at a DRAGEN Pipeline
public class DragenAnalysis {
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

  public JsonNode toJson() {
    ObjectNode ret = mapper.createObjectNode();
    ret.set("dragen_samplesheet", samplesheet.getInfo());
    for (Entry<String, DragenWorkflowAnalysis> e : analyses.entrySet()) {
      ret.set(e.getKey(), e.getValue().toJson());
    }
    return ret;
  }
}
