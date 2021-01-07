package ca.on.oicr.gsi.runscanner.dto.type;

import java.util.HashMap;
import java.util.Map;

public enum IndexSequencing {
  NORMAL(1),
  I5_REVERSE_COMPLEMENT(
      3); // sequences the reverse complement of the reverse primer, rather than the primer

  private static final Map<Integer, IndexSequencing> mapBySbsConsumableVersion;

  static {
    mapBySbsConsumableVersion = new HashMap<>();
    for (IndexSequencing value : IndexSequencing.values()) {
      mapBySbsConsumableVersion.put(value.sbsConsumableVersion, value);
    }
  }

  public static IndexSequencing getBySbsConsumableVersion(Integer sbsConsumableVersion) {
    return mapBySbsConsumableVersion.get(sbsConsumableVersion);
  }

  private final int sbsConsumableVersion;

  private IndexSequencing(int sbsConsumableVersion) {
    this.sbsConsumableVersion = sbsConsumableVersion;
  }
}
