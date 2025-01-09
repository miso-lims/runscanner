package ca.on.oicr.gsi.runscanner.dto;

import ca.on.oicr.gsi.runscanner.dto.dragen.DragenAnalysis;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

// Represents one attempt at an analysis suite.
@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "suite")
@JsonSubTypes({ //
  @Type(value = DragenAnalysis.class, name = "DRAGEN") //
}) //
@JsonIgnoreProperties(ignoreUnknown = true)
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

  public List<T> getAnalyses() {
    return analyses;
  }

  public String toString() {
    return "Analysis [analyses=" + analyses + ", attempt=" + attempt + "]";
  }

  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    Analysis<T> other = (Analysis<T>) obj;

    return Objects.equals(this.analyses, other.analyses)
        && Objects.equals(this.attempt, other.attempt);
  }

  public int hashCode() {
    return Objects.hash(this.analyses, this.attempt);
  }
}
