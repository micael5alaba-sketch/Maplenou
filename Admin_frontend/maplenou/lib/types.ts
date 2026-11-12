// Types partagés — miroirs des DTOs Java du backend (voir Bakend/src/main/java/...).
// Noms de champs identiques, choix volontaire pour que la correspondance soit évidente.

export type Role = "ADMIN" | "DELIVERY_AGENT" | "DELIVERY_ADMIN" | "SUPPORT_AGENT" | "AUDITOR" | null;
export type SellerStatus = "PENDING" | "APPROVED" | "REJECTED" | null;
export type ShopStatus = "PENDING" | "APPROVED" | "REJECTED" | "SUSPENDED";
export type CourierDocumentStatus = "PENDING" | "VALID" | "REJECTED" | "EXPIRED";
export type CourierDocumentType = "DRIVING_LICENSE" | "ID_CARD" | "VEHICLE_INSURANCE" | "VEHICLE_REGISTRATION";
export type CourierEarningStatus = "PENDING" | "PAID";
export type PayoutStatus =
  | "PENDING"
  | "PREPARED"
  | "SUBMITTED"
  | "NEEDS_CORRECTION"
  | "APPROVED"
  | "REJECTED"
  | "PROCESSING"
  | "PAID"
  | "FAILED";
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
  /** Permissions du back-office (menus masqués selon le rôle ; l'API revérifie tout). */
  permissions: string[];
  mustChangePassword: boolean;
  totpEnabled: boolean;
}

export interface LoginResponse {
  mfaRequired: boolean;
  mfaToken: string | null;
  accessToken: string | null;
  refreshToken: string | null;
  user: UserResponse | null;
}

/** Tableau de bord dédié au rôle ; chaque section est null si le rôle n'y a pas droit. */
export interface RoleDashboardResponse {
  logistics: {
    pendingDeliveries: number;
    assignedDeliveries: number;
    inTransitDeliveries: number;
    deliveredToday: number;
    failedDeliveries: number;
    oldestPendingMinutes: number | null;
    averageDeliveryMinutes7d: number | null;
    onlineCouriers: number;
    activeCouriers: number;
    courierDocumentsToReview: number | null;
    courierEarningsAvailable: number | null;
  } | null;
  support: {
    openTickets: number;
    inProgressTickets: number;
    resolvedThisWeek: number;
    oldestOpenTicketHours: number | null;
    pendingSellerApplications: number;
    pendingCategoryChanges: number;
    pendingReturns: number;
    pendingResellerRequests?: number;
    unreadSupportMessages: number;
  } | null;
  finance: {
    toVerify: number;
    toCorrect: number;
    prepared: number;
    awaitingApproval: number;
    awaitingApprovalAmount: number;
    approvedToPay: number;
    approvedToPayAmount: number;
    paidLast7Days: number;
    actionRequiredSellerPayouts: number;
    actionRequiredCourierWithdrawals: number;
    actionRequiredPartnerWithdrawals?: number;
  } | null;
  audit: {
    actionsLast24h: number;
    failedLoginsLast24h: number;
    accessDeniedLast7d: number;
    recent: { id: string; action: string; details: string | null; createdAt: string }[];
  } | null;
}

export type RevenueSource = "COMMISSION" | "DELIVERY_SHARE" | "SELLER_FEE";

/** Revenus de la plateforme sur une période, par source et par jour. */
export interface RevenueSummary {
  from: string;
  to: string;
  commissions: number;
  deliveryShare: number;
  sellerFees: number;
  total: number;
  partnerShares?: number;
  daily: { date: string; commissions: number; deliveryShare: number; sellerFees: number }[];
}

export interface RevenueEntry {
  source: RevenueSource;
  referenceId: string;
  reference: string;
  label: string;
  amount: number;
  date: string;
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

export interface DeliveryPricingTierResponse {
  id: string;
  // null = dernier palier, "au-delà de X km" (illimité).
  maxDistanceKm: number | null;
  price: number;
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
  approvedBy: string | null;
  approvedAt: string | null;
}

/** Retrait des gains d'un livreur (même circuit que les reversements vendeurs). */
export interface CourierWithdrawalResponse {
  id: string;
  agentId: string;
  agentName: string;
  agentPhone: string;
  amount: number;
  status: PayoutStatus;
  paymentDetails: string;
  approvedBy: string | null;
  approvedAt: string | null;
  paidAt: string | null;
  notes: string | null;
  createdAt: string;
}

export interface WithdrawalHistoryEntry {
  id: string;
  fromStatus: PayoutStatus | null;
  toStatus: PayoutStatus;
  actorId: string | null;
  note: string | null;
  createdAt: string;
}

export interface StaffResponse {
  id: string;
  fullName: string;
  phoneNumber: string;
  email: string | null;
  role: Role;
  active: boolean;
  totpEnabled: boolean;
  mustChangePassword: boolean;
  deliveryPricingGranted: boolean;
  createdAt: string;
}

/** Compte du personnel + mot de passe initial en clair, renvoyé une seule fois. */
export interface StaffCredentialsResponse {
  staff: StaffResponse;
  temporaryPassword: string;
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
  deliveryCity: string | null;
  deliveryDistrict: string | null;
  /** Position de la boutique (point de retrait), null si la boutique n'est pas géolocalisée. */
  shopLatitude: number | null;
  shopLongitude: number | null;
}

/** Dernière position connue d'un livreur (ping de l'app livreur quand il est "En ligne"). */
export interface CourierLocationResponse {
  agentId: string;
  fullName: string;
  phoneNumber: string;
  latitude: number;
  longitude: number;
  updatedAt: string;
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

// ----- Programme partenaire revendeur -----

export type ResellerStatus = "PENDING" | "APPROVED" | "REJECTED" | "SUSPENDED" | "DEACTIVATED";

export interface ResellerProfileResponse {
  id: string;
  userId: string;
  fullName: string;
  phoneNumber: string;
  email: string;
  status: ResellerStatus;
  termsVersion: string;
  termsAcceptedAt: string;
  decidedAt: string | null;
  statusReason: string | null;
  createdAt: string;
}

export interface ResellerWalletResponse {
  available: number;
  reserved: number;
  pending: number;
  totalEarned: number;
  totalPaid: number;
  totalFees: number;
  clicks: number;
}

export interface ResellerSettingsResponse {
  version: number;
  shareRate: number;
  minUnitPrice: number;
  tierThreshold: number;
  rateLow: number;
  rateHigh: number;
  attributionDays: number;
  minWithdrawal: number;
  withdrawalFeeRate: number;
  programActive: boolean;
  changeReason: string | null;
  createdAt: string;
}

export interface ResellerCommissionResponse {
  id: string;
  orderItemId: string;
  partnerId: string;
  productName: string;
  unitIndex: number;
  unitPrice: number;
  commissionRate: number;
  maplenouCommission: number;
  partnerShare: number;
  maplenouShare: number;
  settingsVersion: number;
  status: "PENDING" | "CREDITED" | "CANCELLED";
  createdAt: string;
  creditedAt: string | null;
}

export interface ResellerWithdrawalResponse {
  id: string;
  partnerId: string;
  partnerName: string;
  amount: number;
  feeAmount: number;
  netAmount: number;
  method: "MIX_BY_YAS" | "MOOV_AFRICA";
  paymentPhone: string;
  status: PayoutStatus;
  paymentReference: string | null;
  approvedBy: string | null;
  approvedAt: string | null;
  paidAt: string | null;
  notes: string | null;
  createdAt: string;
}

// ----- Service client (supervision en lecture seule, voir lib/serviceClient.ts) -----

export interface ServiceClientTicket {
  id: string;
  ticketNumber: string;
  subject: string;
  category: { code: string; name: string };
  priority: string;
  status: string;
  requester: { id: string; name: string };
  assignedAgent: { id: string; name: string } | null;
  escalated: boolean;
  slaState: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ServiceClientPage<T> {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
}

export interface ServiceClientDashboard {
  openCount: number;
  newCount: number;
  unassignedCount: number;
  inProgressCount: number;
  waitingForCustomerCount: number;
  resolvedTodayCount: number;
  escalatedOpenCount: number;
  slaBreachedOpenCount: number;
  openByCategory: { code: string; name: string; count: number }[];
  recent: ServiceClientTicket[];
  satisfactionLast30Days?: { count: number; average: number | null; satisfiedPercent: number | null } | null;
}

export interface ServiceClientStaffRole {
  phone: string;
  role: string;
  assignedAt: string;
}
