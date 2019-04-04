module ca.on.oicr.gsi.runscanner.processorapi {
  exports ca.on.oicr.gsi.runscanner.processor;

  requires transitive ca.on.oicr.gsi.runscanner.dto;
  requires java.xml;
  requires com.fasterxml.jackson.databind;
  requires com.fasterxml.jackson.datatype.jsr310;
  requires slf4j.api;
  requires com.fasterxml.jackson.annotation;

  uses ca.on.oicr.gsi.runscanner.processor.RunProcessor;
}
