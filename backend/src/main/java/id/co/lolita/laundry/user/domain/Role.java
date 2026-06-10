package id.co.lolita.laundry.user.domain;

public enum Role {
    /** Top-level administrator: all dashboards, system config (users, master data, items), and adjustments. */
    SUPER_ADMIN,
    OWNER,
    STAFF,
    DRIVER
}
