package ca.on.oicr.gsi.runscanner.dto.type;

public enum UltimaProcessStatus {
  PENDING,
  RUNNING,
  COMPLETE,
  FAILED;

  public static UltimaProcessStatus fromCode(int status) {
    switch (status) {
      case 0:
        return UltimaProcessStatus.PENDING;
      case 1:
        return UltimaProcessStatus.RUNNING;
      case 2:
        return UltimaProcessStatus.COMPLETE;
    }
    return UltimaProcessStatus.FAILED;
  }
}
