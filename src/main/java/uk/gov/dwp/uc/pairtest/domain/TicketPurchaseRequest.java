package uk.gov.dwp.uc.pairtest.domain;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;

/** Should be an Immutable Object */
@Builder
public class TicketPurchaseRequest {

  private final Long accountId;
  private final List<TicketRequest> ticketRequests;
  private final String discountCode;

  public TicketPurchaseRequest(
      Long accountId, List<TicketRequest> ticketRequests, String discountCode) {
    this.accountId = accountId;
    this.ticketRequests = ticketRequests;
    this.discountCode = discountCode;
  }

  public Long getAccountId() {
    return accountId;
  }

  public List<TicketRequest> getTicketTypeRequests() {
    if (ticketRequests != null) return new ArrayList<>(ticketRequests);
    else return ticketRequests;
  }

  public String getDiscountCode() {
    return discountCode;
  }
}
