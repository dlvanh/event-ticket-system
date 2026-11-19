export interface CreateOrderTicketItem {
  ticketId: number;
  quantity: number;
}

export interface CreateOrderRequest {
  eventId: number;
  tickets: CreateOrderTicketItem[];
  discountCode?: string;
  returnUrl: string;
  cancelUrl: string;
}

export interface CreateOrderResponse {
  orderId: number;
  checkoutUrl: string;
  paymentLinkId: string;
}
