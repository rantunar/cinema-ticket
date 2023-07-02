import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import thirdparty.discount.Discount;
import thirdparty.discount.DiscountService;
import thirdparty.discount.exception.InvalidDiscountCodeException;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.TicketServiceImpl;
import uk.gov.dwp.uc.pairtest.domain.TicketPurchaseRequest;
import uk.gov.dwp.uc.pairtest.domain.TicketRequest;
import uk.gov.dwp.uc.pairtest.domain.TicketRequest.Type;
import uk.gov.dwp.uc.pairtest.enums.ErrorCodes;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {
  @InjectMocks private TicketServiceImpl ticketService;

  @Mock private SeatReservationService seatReservationService;
  @Mock private TicketPaymentService ticketPaymentService;
  @Mock private DiscountService discountService;

  @Test
  void givenNullAccountId_whenPurchaseTicket_thenFailed() {
    TicketRequest ticketTypeRequest =
        TicketRequest.builder().type(TicketRequest.Type.ADULT).noOfTickets(1).build();
    TicketPurchaseRequest ticketPurchaseRequest =
        TicketPurchaseRequest.builder()
            .accountId(null)
            .ticketRequests(List.of(ticketTypeRequest))
            .build();
    InvalidPurchaseException invalidPurchaseException =
        assertThrows(
            InvalidPurchaseException.class,
            () -> ticketService.purchaseTickets(ticketPurchaseRequest));
    assertNotNull(invalidPurchaseException);
    assertEquals(ErrorCodes.ERROR02.name(), invalidPurchaseException.getErrorCode().name());
    assertEquals(ErrorCodes.ERROR02.getMsg(), invalidPurchaseException.getErrorCode().getMsg());
  }

  @Test
  void givenZeroAccountId_whenPurchaseTicket_thenFailed() {
    TicketRequest ticketTypeRequest =
        TicketRequest.builder().type(TicketRequest.Type.ADULT).noOfTickets(1).build();
    TicketPurchaseRequest ticketPurchaseRequest =
        TicketPurchaseRequest.builder()
            .accountId(0L)
            .ticketRequests(List.of(ticketTypeRequest))
            .build();
    InvalidPurchaseException invalidPurchaseException =
        assertThrows(
            InvalidPurchaseException.class,
            () -> ticketService.purchaseTickets(ticketPurchaseRequest));
    assertNotNull(invalidPurchaseException);
    assertEquals(ErrorCodes.ERROR02.name(), invalidPurchaseException.getErrorCode().name());
    assertEquals(ErrorCodes.ERROR02.getMsg(), invalidPurchaseException.getErrorCode().getMsg());
  }

  @Test
  void givenTotalTicketCountMoreThan20_whenPurchaseTicket_thenFailed() {
    TicketRequest ticketTypeRequest =
        TicketRequest.builder().type(TicketRequest.Type.ADULT).noOfTickets(21).build();
    TicketPurchaseRequest ticketPurchaseRequest =
        TicketPurchaseRequest.builder()
            .accountId(1L)
            .ticketRequests(List.of(ticketTypeRequest))
            .build();
    InvalidPurchaseException invalidPurchaseException =
        assertThrows(
            InvalidPurchaseException.class,
            () -> ticketService.purchaseTickets(ticketPurchaseRequest));
    assertNotNull(invalidPurchaseException);
    assertEquals(ErrorCodes.ERROR03.name(), invalidPurchaseException.getErrorCode().name());
    assertEquals(ErrorCodes.ERROR03.getMsg(), invalidPurchaseException.getErrorCode().getMsg());
  }

  @Test
  void givenNoAdultTicket_whenPurchaseTicket_thenFailed() {
    TicketRequest ticketTypeRequest =
        TicketRequest.builder().type(Type.CHILD).noOfTickets(20).build();
    TicketPurchaseRequest ticketPurchaseRequest =
        TicketPurchaseRequest.builder()
            .accountId(1L)
            .ticketRequests(List.of(ticketTypeRequest))
            .build();
    InvalidPurchaseException invalidPurchaseException =
        assertThrows(
            InvalidPurchaseException.class,
            () -> ticketService.purchaseTickets(ticketPurchaseRequest));
    assertNotNull(invalidPurchaseException);
    assertEquals(ErrorCodes.ERROR04.name(), invalidPurchaseException.getErrorCode().name());
    assertEquals(ErrorCodes.ERROR04.getMsg(), invalidPurchaseException.getErrorCode().getMsg());
  }

  @Test
  void givenPurchaseDataValid_whenPurchaseTicket_thenSucceed() {
    TicketRequest ticketTypeRequest =
        TicketRequest.builder().type(TicketRequest.Type.ADULT).noOfTickets(20).build();
    TicketPurchaseRequest ticketPurchaseRequest =
        TicketPurchaseRequest.builder()
            .accountId(1L)
            .ticketRequests(List.of(ticketTypeRequest))
            .build();
    ticketService.purchaseTickets(ticketPurchaseRequest);
    Mockito.verify(seatReservationService).reserveSeat(1L, 20);
    Mockito.verify(ticketPaymentService).makePayment(1L, 400);
  }

  @Test
  void givenCombinedAdultChildInfant_whenPurchaseTicket_thenSucceed() {
    TicketPurchaseRequest ticketPurchaseRequest =
        TicketPurchaseRequest.builder()
            .accountId(1L)
            .ticketRequests(
                List.of(
                    TicketRequest.builder().type(TicketRequest.Type.ADULT).noOfTickets(5).build(),
                    TicketRequest.builder().type(Type.CHILD).noOfTickets(10).build(),
                    TicketRequest.builder().type(Type.INFANT).noOfTickets(5).build()))
            .build();
    ticketService.purchaseTickets(ticketPurchaseRequest);
    Mockito.verify(seatReservationService).reserveSeat(1L, 15);
    Mockito.verify(ticketPaymentService).makePayment(1L, 200);
  }

  @Test
  void givenEmptyPurchaseData_whenPurchaseTicket_thenFailed() {
    TicketPurchaseRequest ticketPurchaseRequest =
        TicketPurchaseRequest.builder().accountId(1L).ticketRequests(List.of()).build();
    InvalidPurchaseException invalidPurchaseException =
        assertThrows(
            InvalidPurchaseException.class,
            () -> ticketService.purchaseTickets(ticketPurchaseRequest));
    assertEquals(ErrorCodes.ERROR04.name(), invalidPurchaseException.getErrorCode().name());
    assertEquals(ErrorCodes.ERROR04.getMsg(), invalidPurchaseException.getErrorCode().getMsg());
  }

  @Test
  void givenNullPurchaseData_whenPurchaseTicket_thenFailed() {
    TicketPurchaseRequest ticketPurchaseRequest =
        TicketPurchaseRequest.builder().accountId(1L).ticketRequests(null).build();
    InvalidPurchaseException invalidPurchaseException =
        assertThrows(
            InvalidPurchaseException.class,
            () -> ticketService.purchaseTickets(ticketPurchaseRequest));
    assertEquals(ErrorCodes.ERROR05.name(), invalidPurchaseException.getErrorCode().name());
    assertEquals(ErrorCodes.ERROR05.getMsg(), invalidPurchaseException.getErrorCode().getMsg());
  }

  @Test
  void givenTicketPaymentServiceThrowError_whenPurchaseTicket_thenFailed() {
    doThrow(new InvalidPurchaseException(ErrorCodes.ERROR01, "Payment Failed!!"))
        .when(ticketPaymentService)
        .makePayment(1L, 20);
    TicketPurchaseRequest ticketPurchaseRequest =
        TicketPurchaseRequest.builder()
            .accountId(1L)
            .ticketRequests(
                List.of(TicketRequest.builder().type(Type.ADULT).noOfTickets(1).build()))
            .build();
    InvalidPurchaseException invalidPurchaseException =
        assertThrows(
            InvalidPurchaseException.class,
            () -> ticketService.purchaseTickets(ticketPurchaseRequest));
    assertEquals(ErrorCodes.ERROR01.name(), invalidPurchaseException.getErrorCode().name());
    assertEquals("Payment Failed!!", invalidPurchaseException.getMessage());
  }

  @Test
  void givenSeatReservationServiceThrowError_whenPurchaseTicket_thenFailed() {
    doThrow(new InvalidPurchaseException(ErrorCodes.ERROR01, "Seat Reservation Failed!!"))
        .when(seatReservationService)
        .reserveSeat(1L, 20);
    TicketPurchaseRequest ticketPurchaseRequest =
        TicketPurchaseRequest.builder()
            .accountId(1L)
            .ticketRequests(
                List.of(TicketRequest.builder().type(Type.ADULT).noOfTickets(20).build()))
            .build();
    InvalidPurchaseException invalidPurchaseException =
        assertThrows(
            InvalidPurchaseException.class,
            () -> ticketService.purchaseTickets(ticketPurchaseRequest));
    assertEquals(ErrorCodes.ERROR01.name(), invalidPurchaseException.getErrorCode().name());
    assertEquals("Seat Reservation Failed!!", invalidPurchaseException.getMessage());
  }

  @Test
  void givenDiscountCodeWithPercentage_thenReturnDiscountedPrice() {
    TicketPurchaseRequest ticketPurchaseRequest =
        TicketPurchaseRequest.builder()
            .accountId(1L)
            .ticketRequests(
                List.of(TicketRequest.builder().type(Type.ADULT).noOfTickets(20).build()))
            .discountCode("AK001")
            .build();
    Discount discount = new Discount(50);
    when(discountService.getDiscountPercentage(1L, "AK001")).thenReturn(discount);
    ticketService.purchaseTickets(ticketPurchaseRequest);
    verify(ticketPaymentService).makePayment(1L, 200);
  }

  @Test
  void givenInvalidDiscountCode_thenReturnInvalidDiscountCodeException() {
    TicketPurchaseRequest ticketPurchaseRequest =
        TicketPurchaseRequest.builder()
            .accountId(1L)
            .ticketRequests(
                List.of(TicketRequest.builder().type(Type.ADULT).noOfTickets(20).build()))
            .discountCode("AK001")
            .build();
    when(discountService.getDiscountPercentage(1L, "AK001"))
        .thenThrow(new InvalidDiscountCodeException("Invalid code AK001"));
    InvalidDiscountCodeException invalidDiscountCodeException =
        assertThrows(
            InvalidDiscountCodeException.class,
            () -> ticketService.purchaseTickets(ticketPurchaseRequest));
    assertEquals("Invalid code AK001", invalidDiscountCodeException.getMessage());
    verify(ticketPaymentService, times(0)).makePayment(1L, 200);
  }

  @Test
  void givenNullDiscountObject_thenThrowException() {
    TicketPurchaseRequest ticketPurchaseRequest =
        TicketPurchaseRequest.builder()
            .accountId(1L)
            .ticketRequests(
                List.of(TicketRequest.builder().type(Type.ADULT).noOfTickets(20).build()))
            .discountCode("AK001")
            .build();
    when(discountService.getDiscountPercentage(1L, "AK001")).thenReturn(null);
    InvalidPurchaseException invalidPurchaseException =
        assertThrows(
            InvalidPurchaseException.class,
            () -> ticketService.purchaseTickets(ticketPurchaseRequest));
    assertEquals(ErrorCodes.ERROR01, invalidPurchaseException.getErrorCode());
    assertEquals(
        "Discount data is null for discount code = AK001", invalidPurchaseException.getMessage());
  }
}
