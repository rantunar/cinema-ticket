package uk.gov.dwp.uc.pairtest.domain;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/** Should be an Immutable Object */
@Builder
@Value
public class TicketRequest {

  @NonNull Integer noOfTickets;
  @NonNull Type type;

  public enum Type {
    ADULT,
    CHILD,
    INFANT
  }
}
