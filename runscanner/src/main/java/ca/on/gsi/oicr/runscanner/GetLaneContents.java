package ca.on.gsi.oicr.runscanner;

import java.util.Optional;

public interface GetLaneContents {
  Optional<String> getLaneContents(int lane);
}
