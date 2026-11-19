export interface DiscountDto {
  discountId: number;
  eventId: number;
  code: string;
  discountType: 'percentage' | 'fixed_amount';
  value: number;
  validFrom: string;
  validTo: string;
  maxUsage: number;
}
