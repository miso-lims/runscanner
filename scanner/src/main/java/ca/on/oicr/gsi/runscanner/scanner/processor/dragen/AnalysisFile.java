package ca.on.oicr.gsi.runscanner.scanner.processor.dragen;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

// Represents one file output by DRAGEN
public class AnalysisFile {
  private Path path;
  private String checksum;
  private long size;

  // What will be in this map will be unique to the particular DRAGEN Workflow
  private Map<String, Object> info = new HashMap<>();

  public Path getPath() {
    return path;
  }

  public void setPath(Path p) {
    this.path = p;
  }

  public String getChecksum() {
    return checksum;
  }

  public void setChecksum(String s) {
    this.checksum = s;
  }

  public long getSize() {
    return size;
  }

  public void setSize(long l) {
    this.size = l;
  }

  public Map<String, Object> getInfo() {
    return info;
  }

  public void addInfoItem(String k, Object v) {
    info.put(k, v);
  }

  public JsonNode toJson() {
    ObjectNode ret = DragenAnalysis.mapper.createObjectNode();

    ret.put("path", path.toString());
    ret.put("size", size);
    ret.put("checksum", checksum);

    ObjectNode infoBlock = DragenAnalysis.mapper.createObjectNode();
    for (Entry<String, Object> infoEntry : info.entrySet()) {
      infoBlock.put(infoEntry.getKey(), infoEntry.getValue().toString());
    }
    ret.set("info", infoBlock);

    return ret;
  }

  // TODO equals, hashcode
}
