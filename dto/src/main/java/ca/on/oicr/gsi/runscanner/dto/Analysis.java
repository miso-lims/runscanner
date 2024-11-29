package ca.on.oicr.gsi.runscanner.dto;

import java.util.HashMap;
import java.util.Map;

public abstract class Analysis<T extends WorkflowAnalysis> {
  Map<String, T> analyses = new HashMap<>();

  public T get(String workflowName) {
    return analyses.getOrDefault(workflowName, null);
  }

  public void put(String name, T t) {
    analyses.put(name, t);
  }
}
