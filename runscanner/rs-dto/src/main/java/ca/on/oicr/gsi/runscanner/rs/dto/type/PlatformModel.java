/*
 * Copyright (c) 2012. The Genome Analysis Centre, Norwich, UK
 * MISO project contacts: Robert Davey @ TGAC
 * *********************************************************************
 *
 * This file is part of MISO.
 *
 * MISO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MISO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MISO.  If not, see <http://www.gnu.org/licenses/>.
 *
 * *********************************************************************
 */

package ca.on.oicr.gsi.runscanner.rs.dto.type;

import java.io.Serializable;
/**
 * A Platform describes metadata about potentially any hardware item, but is usually linked to a sequencer implementation.
 * 
 * @author Rob Davey
 * @since 0.0.2
 */


public class PlatformModel implements Comparable<PlatformModel>, Serializable {

  private static final long serialVersionUID = 1L;

  public static final Long UNSAVED_ID = 0L;

  private Platform platformType;

  private String description;

  private InstrumentType instrumentType;

  private String instrumentModel;

  private int numContainers;

  
  private long platformId = PlatformModel.UNSAVED_ID;

  public Long getId() {
    return platformId;
  }

  public void setId(Long platformId) {
    this.platformId = platformId;
  }

  public Platform getPlatformType() {
    return platformType;
  }

  public void setPlatformType(Platform platformType) {
    this.platformType = platformType;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getInstrumentModel() {
    return instrumentModel;
  }

  public void setInstrumentModel(String instrumentModel) {
    this.instrumentModel = instrumentModel;
  }

  public String getNameAndModel() {
    return platformType.getKey() + " - " + instrumentModel;
  }

  public int getNumContainers() {
    return numContainers;
  }

  public void setNumContainers(int numContainers) {
    this.numContainers = numContainers;
  }

  public InstrumentType getInstrumentType() {
    return instrumentType;
  }

  public void setInstrumentType(InstrumentType instrumentType) {
    this.instrumentType = instrumentType;
  }

  /**
   * Equivalency is based on id if set, otherwise on name, description and creation date.
   */

  @Override
  public boolean equals(Object obj) {
    if (obj == null) return false;
    if (obj == this) return true;
    if (!(obj instanceof PlatformModel)) return false;
    PlatformModel them = (PlatformModel) obj;
    // If not saved, then compare resolved actual objects. Otherwise
    // just compare IDs.
    if (getId() == PlatformModel.UNSAVED_ID || them.getId() == PlatformModel.UNSAVED_ID) {
      return getPlatformType().equals(them.getPlatformType()) && getDescription().equals(them.getDescription());
    } else {
      return getId().longValue() == them.getId().longValue();
    }
  }

  @Override
  public int hashCode() {
    if (getId() != PlatformModel.UNSAVED_ID) {
      return getId().intValue();
    } else {
      final int PRIME = 37;
      int hashcode = -1;
      if (getPlatformType() != null) hashcode = PRIME * hashcode + getPlatformType().hashCode();
      if (getDescription() != null) hashcode = PRIME * hashcode + getDescription().hashCode();
      return hashcode;
    }
  }

  @Override
  public int compareTo(PlatformModel t) {
    if (getId() < t.getId()) return -1;
    if (getId() > t.getId()) return 1;
    return 0;
  }

}