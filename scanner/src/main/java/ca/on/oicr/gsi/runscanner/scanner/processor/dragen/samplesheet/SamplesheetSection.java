package ca.on.oicr.gsi.runscanner.scanner.processor.dragen.samplesheet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "name")
@JsonSubTypes({ //
  @Type(value = SamplesheetBCLConvertSection.class, name = "BCLConvert"), //
  @Type(value = SamplesheetReadsSection.class, name = "Reads") //
}) //
@JsonIgnoreProperties(ignoreUnknown = true)
public interface SamplesheetSection {
  String getName();
}
