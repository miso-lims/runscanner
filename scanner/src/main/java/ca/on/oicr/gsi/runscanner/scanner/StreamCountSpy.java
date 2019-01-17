package ca.on.oicr.gsi.runscanner.scanner;

import io.prometheus.client.Gauge;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Count the number of items in a stream and report the result to Prometheus.
 *
 * <p>This is meant to be used with {@link Stream#peek}
 */
public class StreamCountSpy<T> implements Consumer<T>, AutoCloseable {
  private long count = 0;
  private final Gauge destination;

  public StreamCountSpy(Gauge destination) {
    this.destination = destination;
  }

  @Override
  public void accept(T item) {
    count++;
  }

  @Override
  public void close() throws Exception {
    destination.set(count);
  }
}
