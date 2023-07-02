package uk.gov.dwp.uc.pairtest;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import thirdparty.discount.Discount;
import thirdparty.discount.DiscountService;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketPurchaseRequest;
import uk.gov.dwp.uc.pairtest.domain.TicketRequest;
import uk.gov.dwp.uc.pairtest.enums.ErrorCodes;
import uk.gov.dwp.uc.pairtest.enums.TicketPrices;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;
import uk.gov.dwp.uc.pairtest.utils.Constants;

@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

  @NonNull private final SeatReservationService seatReservationService;
  @NonNull private final TicketPaymentService ticketPaymentService;
  @NonNull private final DiscountService discountService;

  /**
   * This is the main method used to purchase ticket for an account if there is any validation fails
   * or the purchase data is null then throw exception
   *
   * @param ticketPurchaseRequest
   * @throws InvalidPurchaseException
   */
  @Override
  public void purchaseTickets(TicketPurchaseRequest ticketPurchaseRequest)
      throws InvalidPurchaseException {
    if (ticketPurchaseRequest.getTicketTypeRequests() == null)
      throw new InvalidPurchaseException(
          ErrorCodes.ERROR05,
          String.format(
              "Purchase data is null for Account id = [%s]", ticketPurchaseRequest.getAccountId()));
    validatePurchaseRequest(
        ticketPurchaseRequest.getAccountId(), ticketPurchaseRequest.getTicketTypeRequests());
    double totalAmountToPay =
        ticketPurchaseRequest.getTicketTypeRequests().stream()
            .map(
                item ->
                    TicketPrices.valueOf(item.getType().name()).getPrice() * item.getNoOfTickets())
            .reduce(0, Integer::sum);

    int totalSeatsToAllocate =
        ticketPurchaseRequest.getTicketTypeRequests().stream()
            .filter(e -> !e.getType().equals(TicketRequest.Type.INFANT))
            .mapToInt(TicketRequest::getNoOfTickets)
            .sum();
    // Apply discount
    if (ticketPurchaseRequest.getDiscountCode() != null) {
      Discount discount =
          discountService.getDiscountPercentage(
              ticketPurchaseRequest.getAccountId(), ticketPurchaseRequest.getDiscountCode());
      totalAmountToPay =
          totalAmountToPay
              * (Optional.ofNullable(discount)
                      .orElseThrow(
                          () ->
                              new InvalidPurchaseException(
                                  ErrorCodes.ERROR01,
                                  String.format(
                                      "Discount data is null for discount code = %s",
                                      ticketPurchaseRequest.getDiscountCode())))
                      .percentage()
                  / 100);
    }
    ticketPaymentService.makePayment(ticketPurchaseRequest.getAccountId(), (int) totalAmountToPay);
    seatReservationService.reserveSeat(ticketPurchaseRequest.getAccountId(), totalSeatsToAllocate);
  }

  /**
   * validate the purchase request data, check account id and purchase information
   *
   * @param accountId
   * @param ticketTypeRequests
   */
  private void validatePurchaseRequest(Long accountId, List<TicketRequest> ticketTypeRequests) {
    if (accountId == null || accountId <= 0)
      throw new InvalidPurchaseException(
          ErrorCodes.ERROR02, String.format("Account id = [%s] is not a valid data", accountId));
    if (isMaxTicketCountExceeded(ticketTypeRequests))
      throw new InvalidPurchaseException(
          ErrorCodes.ERROR03,
          String.format(
              "Max ticket purchase count exceed the limit of = [%s]",
              Constants.MAX_NO_TICKET_ALLOWED));
    if (!isAdultTicketPresent(ticketTypeRequests))
      throw new InvalidPurchaseException(
          ErrorCodes.ERROR04,
          String.format("No adult ticket is present for account id = [%s]", accountId));
  }

  /**
   * check the maximum ticket count defined in constant is less than or equal to the requested data
   * if not then throw exception
   *
   * @param ticketTypeRequests
   * @return
   */
  private boolean isMaxTicketCountExceeded(List<TicketRequest> ticketTypeRequests) {
    int totalNoOfTickets =
        ticketTypeRequests.stream().mapToInt(TicketRequest::getNoOfTickets).sum();
    return totalNoOfTickets > Constants.MAX_NO_TICKET_ALLOWED;
  }

  /**
   * check is at least one adult ticket has been purchased or throw exception
   *
   * @param ticketTypeRequests
   * @return
   */
  private boolean isAdultTicketPresent(List<TicketRequest> ticketTypeRequests) {
    return ticketTypeRequests.stream().anyMatch(e -> e.getType().equals(TicketRequest.Type.ADULT));
  }
}
