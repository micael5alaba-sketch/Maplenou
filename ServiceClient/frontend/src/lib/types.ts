// Types mirroring the backend API (docs/architecture/openapi.json).

export type Role =
  | "CUSTOMER"
  | "SELLER"
  | "DELIVERY_DRIVER"
  | "SUPPORT_AGENT"
  | "SUPPORT_MANAGER"
  | "ADMINISTRATOR";

/** Built-in statuses. Administrators may add their own (GET /vocabulary), so any code is accepted. */
export type SystemStatus = "NEW" | "ASSIGNED" | "IN_PROGRESS" | "WAITING_FOR_CUSTOMER" | "RESOLVED" | "CLOSED";
export type TicketStatus = SystemStatus | (string & {});
export type SystemPriority = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";
export type TicketPriority = SystemPriority | (string & {});
/** What a status means for the lifecycle; custom statuses are IN_PROGRESS or ON_HOLD (DEC-CS11-02). */
export type StatusKind = "NEW" | "ASSIGNED" | "IN_PROGRESS" | "WAITING_REQUESTER" | "ON_HOLD" | "RESOLVED" | "CLOSED";
export type SlaState = "ON_TRACK" | "APPROACHING" | "BREACHED" | "MET" | "PAUSED";
export type Visibility = "PUBLIC" | "INTERNAL";

export type TicketAction =
  | "SEND_MESSAGE"
  | "ADD_INTERNAL_NOTE"
  | "VIEW_INTERNAL"
  | "VIEW_CONTEXT"
  | "VIEW_SLA"
  | "ASSIGN_SELF"
  | "REASSIGN"
  | "CHANGE_STATUS"
  | "RESOLVE"
  | "CLOSE"
  | "CHANGE_PRIORITY"
  | "SET_CRITICAL"
  | "CHANGE_CATEGORY"
  | "ESCALATE"
  | "REQUEST_REOPEN"
  | "DECIDE_REOPEN"
  | "RATE"
  | "USE_AI_ASSIST"
  | "REQUEST_REFUND"
  | "RESCHEDULE_DELIVERY"
  | "CONTACT_DRIVER";

export interface User {
  id: string;
  role: Role;
  displayName: string;
  email?: string | null;
  phone?: string | null;
}

export interface Person {
  id: string;
  name: string;
  role: Role | null;
}

export interface Category {
  id: string;
  code: string;
  name: string;
  nameEn?: string | null;
  description: string | null;
  descriptionEn?: string | null;
  sortOrder?: number;
  active: boolean;
}

export interface SlaClock {
  startedAt: string;
  dueAt: string;
  completedAt: string | null;
  state: SlaState;
}

export interface SlaPeriod {
  periodNumber: number;
  priority: TicketPriority;
  startedAt: string;
  dueAt: string;
  endedAt: string | null;
  endReason: string | null;
  pausedMinutes?: number;
}

export interface ReopenRequest {
  id: string;
  status: "PENDING" | "APPROVED" | "REJECTED";
  reason: string;
  requestedAt: string;
  decidedAt: string | null;
  decisionNote: string | null;
}

export interface TicketSummary {
  id: string;
  ticketNumber: string;
  subject: string;
  category: Category;
  priority: TicketPriority;
  status: TicketStatus;
  statusKind?: StatusKind;
  requester: Person;
  assignedAgent: Person | null;
  escalated: boolean;
  slaState: SlaState | null;
  orderRef: string | null;
  deliveryRef: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface TicketDetail {
  id: string;
  ticketNumber: string;
  subject: string;
  description: string;
  category: Category;
  priority: TicketPriority;
  status: TicketStatus;
  statusKind?: StatusKind;
  requester: Person;
  assignedAgent: Person | null;
  requesterReportedUrgent: boolean;
  escalated: boolean;
  escalatedAt: string | null;
  references: {
    orderRef: string | null;
    paymentRef: string | null;
    deliveryRef: string | null;
    sellerRef: string | null;
    refundRef: string | null;
    /** Every linked reference by type (ORDER, PAYMENT, DELIVERY, SELLER, REFUND…). */
    all?: Record<string, string>;
  };
  sla: {
    firstResponse: SlaClock;
    resolution: SlaClock;
    overall: SlaState;
    /** Set while the resolution clock is paused (waiting for the requester, DEC-CS11-03). */
    pausedSince?: string | null;
    periods: SlaPeriod[];
  } | null;
  pendingReopenRequest: ReopenRequest | null;
  lastReopenDecision: ReopenRequest | null;
  allowedActions: TicketAction[];
  allowedStatusTargets: TicketStatus[];
  createdAt: string;
  updatedAt: string;
  resolvedAt: string | null;
  closedAt: string | null;
  version: number;
  /** Number in the former Maplenou support (imported tickets). */
  legacyNumber?: string | null;
  satisfaction?: Satisfaction | null;
  aiSuggestion?: AiSuggestion | null;
  externalActions?: ExternalAction[];
}

export interface Satisfaction {
  rating: number;
  comment: string | null;
  ratedAt: string;
}

export interface AiSuggestion {
  categoryCode: string;
  priorityCode: string;
  confidence: number;
  reason: string | null;
  createdAt: string;
  decision: "APPLIED" | "DISMISSED" | null;
}

export type ExternalActionType = "REFUND_REQUEST" | "DELIVERY_RESCHEDULE" | "DRIVER_MESSAGE";

export interface ExternalAction {
  id: string;
  type: ExternalActionType;
  targetRef: string | null;
  succeeded: boolean;
  resultRef: string | null;
  error: string | null;
  requestedBy: Person | null;
  createdAt: string;
}

export interface SatisfactionStats {
  count: number;
  average: number | null;
  satisfiedPercent: number | null;
}

// ---------------------------------------------------------------- configurable vocabulary (GET /vocabulary)

export interface StatusDefinition {
  code: string;
  kind: StatusKind;
  labelFr: string;
  labelEn: string | null;
  sortOrder: number;
  slaPaused: boolean;
  system: boolean;
  active: boolean;
}

export interface PriorityDefinition {
  code: string;
  rank: number;
  labelFr: string;
  labelEn: string | null;
  requesterSelectable: boolean;
  defaultPriority: boolean;
  system: boolean;
  active: boolean;
}

export interface Vocabulary {
  statuses: StatusDefinition[];
  transitions: { from: string; to: string }[];
  priorities: PriorityDefinition[];
}

export type PermissionScope = "OWN" | "ASSIGNED" | "UNASSIGNED" | "ANY";

export interface PermissionRule {
  role: Role;
  action: TicketAction;
  scope: PermissionScope;
}

export interface SlaPolicy {
  priorityCode: string;
  firstResponseMinutes: number;
  resolutionMinutes: number | null;
  resolutionDays: number | null;
}

export interface CannedReply {
  id: string;
  title: string;
  body: string;
  language: "fr" | "en";
  categoryCode: string | null;
  active: boolean;
  updatedAt: string;
}

export interface MessageTemplate {
  type: string;
  language: "fr" | "en";
  title: string;
  body: string;
}

export interface AiDraft {
  text: string;
  references: string[];
}

export interface HelpArticle {
  title: string;
  breadcrumb: string;
  url: string;
  excerpt: string;
}

export interface Attachment {
  id: string;
  filename: string;
  contentType: string;
  sizeBytes: number;
  createdAt: string;
}

export interface Message {
  id: string;
  author: Person;
  visibility: Visibility;
  body: string;
  createdAt: string;
  attachments: Attachment[];
}

export interface HistoryEntry {
  id: string;
  action: string;
  actor: Person | null;
  oldValue: string | null;
  newValue: string | null;
  details: string | null;
  createdAt: string;
}

export interface Page<T> {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

export interface OrderSummary {
  ref: string;
  customerName: string;
  sellerRef: string;
  sellerName: string;
  orderedAt: string;
  amount: number;
  currency: string;
  status: string;
  paymentRef: string | null;
  deliveryRef: string | null;
  refundRef: string | null;
  subOrders?: { id: string; shopName: string; status: string; subtotal: number }[];
}

export interface PaymentSummary {
  ref: string;
  orderRef: string;
  status: string;
  amount: number;
  currency: string;
  provider: string;
  commissionAmount: number | null;
  payoutStatus: string | null;
  payoutAmount: number | null;
  paidAt: string | null;
}

export interface DeliverySummary {
  ref: string;
  orderRef: string;
  customerName: string;
  driverName: string;
  status: string;
  pickedUpAt: string | null;
  lastUpdateAt: string | null;
  destination: string;
  timeSlot: string | null;
}

export interface SellerSummary {
  ref: string;
  shopName: string;
  ownerName: string;
  accountStatus: string;
  balance: number;
  currency: string;
}

export interface RefundSummary {
  ref: string;
  orderRef: string;
  status: string;
  amount: number;
  currency: string;
  requestedAt: string;
}

export interface Section<T> {
  linked: boolean;
  available: boolean;
  data: T | null;
}

export interface TicketContext {
  requester: {
    id: string;
    name: string;
    role: Role;
    email: string | null;
    phone: string | null;
    active: boolean;
    knownSince: string;
    ticketCount: number;
  };
  order: Section<OrderSummary>;
  payment: Section<PaymentSummary>;
  delivery: Section<DeliverySummary>;
  seller: Section<SellerSummary>;
  refund: Section<RefundSummary>;
}

export interface Dashboard {
  openCount: number;
  newCount: number;
  unassignedCount: number;
  inProgressCount: number;
  waitingForCustomerCount: number;
  resolvedTodayCount: number;
  escalatedOpenCount: number;
  slaBreachedOpenCount: number;
  myOpenCount: number;
  openByPriority: Record<string, number>;
  openByCategory: { code: string; name: string; count: number }[];
  recent: TicketSummary[];
  satisfactionLast30Days?: SatisfactionStats | null;
}

export interface Report {
  from: string;
  to: string;
  created: number;
  resolved: number;
  stillOpen: number;
  avgFirstResponseBusinessMinutes: number | null;
  avgResolutionBusinessMinutes: number | null;
  firstResponseSlaMetPercent: number | null;
  resolutionSlaMetPercent: number | null;
  byCategory: { code: string; name: string; count: number }[];
  createdPerDay: Record<string, number>;
  satisfaction?: SatisfactionStats | null;
}

export interface Agent {
  id: string;
  name: string;
  email: string | null;
  role: Role;
  active: boolean;
  openTickets: number;
}

export interface Notification {
  id: string;
  ticketId: string | null;
  type: string;
  /** French title/body written by the backend. */
  title: string;
  body: string;
  /** Language-neutral facts used to render the notification in English. */
  ticketNumber?: string | null;
  detail?: string | null;
  createdAt: string;
  readAt: string | null;
}

export interface Holiday {
  date: string;
  label: string;
}

/** RFC 9457 problem details returned by the API. */
export interface Problem {
  status: number;
  title?: string;
  detail?: string;
  code?: string;
  rule?: string;
  errors?: Record<string, string>;
}
