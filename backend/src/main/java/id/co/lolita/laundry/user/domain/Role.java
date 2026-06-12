package id.co.lolita.laundry.user.domain;

public enum Role {
    /**
     * Top-level administrator: all dashboards, system config (users, master data, items), and adjustments.
     */
    SUPER_ADMIN,
    /**
     * Finance/back-office: clients (read), orders, billing, reports, operational dashboard.
     */
    FINANCE_STAFF,
    /**
     * In-house daily operator: enters orders, sees the order list (with prices), confirms deliveries.
     */
    DAILY_STAFF
}
