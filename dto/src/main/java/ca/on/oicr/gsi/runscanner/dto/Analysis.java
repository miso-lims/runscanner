package ca.on.oicr.gsi.runscanner.dto;

import ca.on.oicr.gsi.runscanner.dto.dragen.DragenAnalysis;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.util.HashMap;
import java.util.Map;

@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "suite")
@JsonSubTypes({ //
  @Type(value = DragenAnalysis.class, name = "DRAGEN") //
}) //
public abstract class Analysis<T extends WorkflowAnalysis> {
  Map<String, T> analyses = new HashMap<>();

  public T get(String workflowName) {
    return analyses.getOrDefault(workflowName, null);
  }

  public void put(String name, T t) {
    analyses.put(name, t);
  }
}
