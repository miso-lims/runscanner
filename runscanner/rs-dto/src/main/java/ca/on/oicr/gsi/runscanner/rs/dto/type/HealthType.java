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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MISO. If not, see <http://www.gnu.org/licenses/>.
 *
 * *********************************************************************
 */

package ca.on.oicr.gsi.runscanner.rs.dto.type;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * This enum represents the health of a particular object, given some kind of underlying process
 * 
 * @author Rob Davey
 * @since 0.0.2
 */

public enum HealthType {
  UNKNOWN("Unknown", false), //
  COMPLETED("Completed", true), //
  FAILED("Failed", true), //
  STOPPED("Stopped", true), //
  RUNNING("Running", false);

  public static final Comparator<HealthType> COMPARATOR = new Comparator<HealthType>() {
    @Override
    public int compare(HealthType o1, HealthType o2) {
      int p1 = o1 == null ? -1 : o1.ordinal();
      int p2 = o2 == null ? -1 : o2.ordinal();
      return p1 - p2;
    }
  };

  /**
   * Field lookup
   */
  private static final Map<String, HealthType> lookup = new HashMap<>();

  static {
    for (HealthType s : EnumSet.allOf(HealthType.class))
      lookup.put(s.getKey(), s);
  }

  /**
   * Returns a HealthType given an enum key
   * 
   * @param key
   *          of type String
   * @return HealthType
   */
  @JsonCreator
  public static HealthType get(String key) {
    return lookup.get(key);
  }

  /** Field key */
  private final String key;

  private final boolean isDone;

  /**
   * Constructs a HealthType based on a given key
   * 
   * @param key
   *          of type String
   */
  HealthType(String key, boolean isDone) {
    this.key = key;
    this.isDone = isDone;
  }

  /**
   * Returns the key of this HealthType enum.
   * 
   * @return String key.
   */
  @JsonValue
  public String getKey() {
    return key;
  }

  public boolean isDone() {
    return isDone;
  }
}
