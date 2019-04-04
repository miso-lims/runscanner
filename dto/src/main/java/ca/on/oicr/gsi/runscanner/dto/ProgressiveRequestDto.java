package ca.on.oicr.gsi.runscanner.dto;

/**
 * Request incremental results from a run server.
 *
 * <p>The goal of the progressive requests is to avoid sending unchanged data from a run server to a
 * client. That being said, the run server has no qualms about sending duplicate information to the
 * client and the client must deal with that.
 *
 * <p>After a request is made, the next request should use the {@link
 * #update(ProgressiveResponseDto)} method to request only the subsequent data.
 *
 * <p>Otherwise, the epoch and token value should be initialised to zero.
 */
public class ProgressiveRequestDto {
  private static final int DEFAULT_LIMIT = 100;
  private int epoch;
  private int limit = DEFAULT_LIMIT;
  private long token;

  public int getEpoch() {
    return epoch;
  }

  public int getLimit() {
    return limit < 1 ? DEFAULT_LIMIT : limit;
  }

  public long getToken() {
    return token;
  }

  public void setEpoch(int epoch) {
    this.epoch = epoch;
  }

  public void setLimit(int limit) {
    this.limit = limit;
  }

  public void setToken(long token) {
    this.token = token;
  }

  public void update(ProgressiveResponseDto response) {
    token = response.getToken();
    epoch = response.getEpoch();
  }
}
