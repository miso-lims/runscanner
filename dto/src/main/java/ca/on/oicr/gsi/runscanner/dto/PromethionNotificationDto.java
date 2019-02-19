package ca.on.oicr.gsi.runscanner.dto;

import ca.on.oicr.gsi.runscanner.dto.type.Platform;

public class PromethionNotificationDto extends NotificationDto {
  @Override
  public Platform getPlatformType() {
    return Platform.PROMETHION;
  }
}
