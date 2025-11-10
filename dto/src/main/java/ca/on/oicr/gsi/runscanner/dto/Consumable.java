package ca.on.oicr.gsi.runscanner.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Objects;

/** Represents a sequencing consumable with its type and lot number. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Consumable {

  private String type;
  private String lotNumber;

  public Consumable() {}

  public Consumable(String type, String lotNumber) {
    this.type = type;
    this.lotNumber = lotNumber;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getLotNumber() {
    return lotNumber;
  }

  public void setLotNumber(String lotNumber) {
    this.lotNumber = lotNumber;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    Consumable other = (Consumable) obj;
    return Objects.equals(this.type, other.type) && Objects.equals(this.lotNumber, other.lotNumber);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, lotNumber);
  }

  @Override
  public String toString() {
    return "Consumable{" + "type='" + type + '\'' + ", lotNumber='" + lotNumber + '\'' + '}';
  }
}
