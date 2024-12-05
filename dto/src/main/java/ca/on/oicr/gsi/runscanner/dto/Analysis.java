package ca.on.oicr.gsi.runscanner.dto;

import ca.on.oicr.gsi.runscanner.dto.dragen.DragenAnalysis;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.util.LinkedList;
import java.util.List;

// Represents one attempt at an analysis suite.
@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "suite")
@JsonSubTypes({ //
  @Type(value = DragenAnalysis.class, name = "DRAGEN") //
}) //
public abstract class Analysis<T extends WorkflowAnalysis> {
  private List<T> analyses = new LinkedList<>();
  private final int attempt;

  protected Analysis(int attempt) {
    this.attempt = attempt;
  }

  public T get(String workflowName) {
    return analyses
        .stream()
        .filter(a -> a.getWorkflowName().equals(workflowName))
        .findFirst()
        .orElse(null);
  }

  public void put(T t) {
    analyses.add(t);
  }

  public int getAttempt() {
    return attempt;
  }
}
