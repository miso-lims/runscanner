package ca.on.oicr.gsi.runscanner.dto.type;

public enum UltimaProcessStatus {
  PENDING,
  RUNNING,
  COMPLETE,
  UNKNOWN,
  FAILED;

  public static UltimaProcessStatus fromCode(int status) {

    if (status >= 3) {
      return UltimaProcessStatus.FAILED;
    }

    return switch (status) {
      case 0 -> UltimaProcessStatus.PENDING;
      case 1 -> UltimaProcessStatus.RUNNING;
      case 2 -> UltimaProcessStatus.COMPLETE;
      default -> UltimaProcessStatus.UNKNOWN;
    };
  }
}
