package ca.on.oicr.gsi.runscanner.scanner;

import io.prometheus.metrics.instrumentation.jvm.JvmMetrics;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationContextEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.stereotype.Component;

@Component
public class ScannerAppListener implements ApplicationListener<ApplicationContextEvent> {

  @Override
  public void onApplicationEvent(ApplicationContextEvent event) {
    if (event instanceof ContextStartedEvent || event instanceof ContextRefreshedEvent) {
      JvmMetrics.builder().register();
      event.getApplicationContext().getBean(Scheduler.class).start();
    } else if (event instanceof ContextStoppedEvent) {
      event.getApplicationContext().getBean(Scheduler.class).stop();
    }
  }
}
