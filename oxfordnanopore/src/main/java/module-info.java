import ca.on.oicr.gsi.runscanner.oxfordnanopore.MinionProcessor;
import ca.on.oicr.gsi.runscanner.oxfordnanopore.PromethionProcessor;
import ca.on.oicr.gsi.runscanner.processor.RunProcessor;

module ca.on.oicr.gsi.runscanner.oxfordnanopore {
  requires ca.on.oicr.gsi.runscanner.processorapi;
  requires jhdf5;
  requires com.fasterxml.jackson.databind;

  provides RunProcessor.Builder with
      MinionProcessor,
      PromethionProcessor;

  exports ca.on.oicr.gsi.runscanner.oxfordnanopore;
}
