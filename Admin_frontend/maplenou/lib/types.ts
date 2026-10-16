// Types partagés — miroirs des DTOs Java du backend (voir Bakend/src/main/java/...).
// Noms de champs identiques, choix volontaire pour que la correspondance soit évidente.

export type Role = "ADMIN" | "DELIVERY_AGENT" | null;
export type SellerStatus = "PENDING" | "APPROVED" | "REJECTED" | null;
export type ShopStatus = "PENDING" | "APPROVED" | "REJECTED" | "SUSPENDED";
export type CourierDocumentStatus = "PENDING" | "VALID" | "REJECTED" | "EXPIRED";
export type CourierDocumentType = "DRIVING_LICENSE" | "ID_CARD" | "VEHICLE_INSURANCE" | "VEHICLE_REGISTRATION";
export type CourierEarningStatus = "PENDING" | "PAID";
export type PayoutStatus = "PENDING" | "PROCESSING" | "PAID" | "FAILED";
export type SellerFeeCategory = "STANDARD" | "HIGH_PRICE";
export type SellerFeeCategoryChangeStatus = "PENDING" | "APPROVED" | "REJECTED";
export type ReturnDecision = "APPROVED" | "REJECTED";
export type SupportTicketStatus = "OPEN" | "IN_PROGRESS" | "RESOLVED";

export interface UserResponse {
  id: string;
  fullName: string;
  phoneNumber: string;
  email: string | null;
  avatarUrl: string | null;
  role: Role;
  phoneVerified: boolean;
  sellerStatus: SellerStatus;
}

export interface LoginResponse {
  mfaRequired: boolean;
  mfaToken: string | null;
  accessToken: string | null;
  refreshToken: string | null;
  user: UserResponse | null;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface SellerProfileResponse {
  id: string;
  userId: string;
  userFullName: string;
  userPhoneNumber: string;
  shopName: string;
  status: SellerStatus;
  feeCategory: SellerFeeCategory;
  createdAt: string | null;
}

export interface ShopResponse {
  id: string;
  ownerId: string;
  name: string;
  slug: string;
  description: string | null;
  logoUrl: string | null;
  coverUrl: string | null;
  status: ShopStatus;
  city: string;
  district: string | null;
  taxId: string | null;
  createdAt: string;
}

export interface CourierDocumentResponse {
  id: string;
  courierId: string;
  courierName: string;
  type: CourierDocumentType;
  fileUrl: string;
  status: CourierDocumentStatus;
  expiresAt: string | null;
  rejectionReason: string | null;
  submittedAt: string;
  reviewedAt: string | null;
}

export interface CourierEarningResponse {
  id: string;
  deliveryId: string;
  agentId: string;
  agentName: string;
  grossAmount: number;
  commissionRate: number;
  commissionAmount: number;
  amount: number;
  status: CourierEarningStatus;
  paidAt: string | null;
  createdAt: string;
}

export interface DeliveryZoneResponse {
  id: string;
  name: string;
  deliveryFee: number;
  pricePerKm: number;
  estimatedTimeMinutes: number;
  active: boolean;
}

export interface PayoutResponse {
  id: string;
  shopId: string;
  shopName: string;
  periodStart: string;
  periodEnd: string;
  amount: number;
  commissionTotal: number;
  feesDeducted: number;
  status: PayoutStatus;
  paidAt: string | null;
  notes: string | null;
  createdAt: string;
}

export interface SellerFeeCategoryChangeResponse {
  id: string;
  shopName: string;
  requestedCategory: SellerFeeCategory;
  status: SellerFeeCategoryChangeStatus;
  effectiveFrom: string | null;
  createdAt: string;
  decidedAt: string | null;
}

export type PromoScopeType = "PLATFORM" | "SHOP";

export interface PromoCodeResponse {
  id: string;
  code: string;
  scopeType: PromoScopeType;
  scopeId: string | null;
  discountPercent: number;
  minOrderAmount: number | null;
  maxUses: number | null;
  currentUses: number;
  expiresAt: string | null;
  active: boolean;
}

export interface ReturnRequestResponse {
  id: string;
  subOrderId: string;
  shopId: string;
  shopName: string;
  buyerId: string;
  buyerName: string;
  deliveryDate: string;
  returnDeadline: string;
  reason: string;
  productCondition: string;
  decision: ReturnDecision | null;
  decidedAt: string | null;
  decidedByName: string | null;
  adminNotes: string | null;
  refundStatus: string;
  createdAt: string;
}

export interface ContentPageResponse {
  id: string;
  slug: string;
  title: string;
  body: string;
  published: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CategoryResponse {
  id: string;
  name: string;
  slug: string;
  parentId: string | null;
  imageUrl: string | null;
  active: boolean;
}

export interface AdminUserResponse {
  id: string;
  fullName: string;
  phoneNumber: string;
  email: string | null;
  role: Role;
  active: boolean;
  phoneVerified: boolean;
  totpEnabled: boolean;
  createdAt: string;
}

export interface DeliveryResponse {
  id: string;
  subOrderId: string;
  shopId: string;
  shopName: string;
  orderId: string;
  orderNumber: string;
  agentId: string | null;
  agentName: string | null;
  agentPhone: string | null;
  status: "PENDING" | "ASSIGNED" | "IN_TRANSIT" | "DELIVERED" | "FAILED";
  proofType: string | null;
  proofUrl: string | null;
  notes: string | null;
  assignedAt: string | null;
  deliveredAt: string | null;
  createdAt: string;
}

export interface AuditLogResponse {
  id: string;
  actorId: string;
  action: string;
  entityType: string;
  entityId: string | null;
  details: string | null;
  ipAddress: string | null;
  createdAt: string;
}

export interface SupportTicketResponse {
  id: string;
  ticketNumber: string;
  requesterName: string;
  category: string;
  subject: string;
  description: string;
  relatedDeliveryId: string | null;
  photoUrl: string | null;
  status: SupportTicketStatus;
  conversationId: string | null;
  resolutionNotes: string | null;
  resolvedAt: string | null;
  createdAt: string;
}

export interface SubscriberResponse {
  id: string;
  email: string;
  active: boolean;
  subscribedAt: string;
}

export interface DashboardKpiResponse {
  revenue: { totalRevenue: number; totalCommission: number; periodRevenue: number };
  orders: { total: number; paid: number; cancelled: number; periodNew: number };
  users: { total: number; periodNew: number };
  shops: { total: number; pending: number; approved: number; suspended: number };
  deliveries: { total: number; pending: number; inTransit: number; delivered: number; failed: number };
}

export type ConversationType = "BUYER_SELLER" | "SUPPORT" | "BUYER_COURIER" | "SELLER_COURIER";

export interface ConversationResponse {
  id: string;
  type: ConversationType;
  shopId: string | null;
  shopName: string | null;
  buyerId: string | null;
  buyerName: string | null;
  sellerId: string | null;
  deliveryId: string | null;
  courierId: string | null;
  courierName: string | null;
  lastMessageAt: string | null;
  createdAt: string;
}

export interface MessageResponse {
  id: string;
  conversationId: string;
  senderId: string;
  senderName: string;
  content: string;
  readAt: string | null;
  createdAt: string;
}

export interface MaintenanceStatusResponse {
  enabled: boolean;
  message: string | null;
}

export type SubOrderStatus =
  | "PENDING"
  | "PREPARING"
  | "READY_FOR_PICKUP"
  | "IN_DELIVERY"
  | "DELIVERED"
  | "CANCELLED"
  | "RETURN_REQUESTED"
  | "RETURNED";

export interface SubOrderItemResponse {
  id: string;
  productVariantId: string | null;
  productName: string;
  variantLabel: string | null;
  sku: string | null;
  unitPrice: number;
  quantity: number;
  lineTotal: number;
}

export interface SubOrderResponse {
  id: string;
  orderId: string;
  orderNumber: string;
  shopId: string;
  shopName: string;
  buyerName: string;
  status: SubOrderStatus;
  subtotal: number;
  commissionRate: number;
  commissionAmount: number;
  netAmount: number;
  deliveryFee: number;
  deliveryDistanceKm: number | null;
  items: SubOrderItemResponse[];
  createdAt: string;
  updatedAt: string;
}
