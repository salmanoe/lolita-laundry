package id.co.lolita.laundry.client.domain;

public enum BillingMode {
    /**
     * One monthly billing document for all orders (all clients except PBS).
     */
    COMBINED,
    /**
     * Separate monthly billing per department (PBS: Room Linen / Uniform+Guest / F&B Linen).
     */
    PER_DEPARTMENT
}
