package com.griff.subscriptions.domain.model

/**
 * What kind of recurring cost an obligation is.
 *
 * The section covers insurance policies *and* periodic public charges, so the enum deliberately
 * mixes the two: the shared trait is "an amount that comes back every period and has a deadline",
 * not "an insurance".
 */
enum class ObligationCategory {
    VEHICLE_INSURANCE,
    HOME_INSURANCE,
    LAND_INSURANCE,
    DRONE_INSURANCE,
    PROPERTY_TAX,
    LAND_TAX,
    OTHER,
    ;

    /** Coarse grouping used for filtering and for the badge shown on lists. */
    val tag: ObligationTag
        get() = when (this) {
            VEHICLE_INSURANCE -> ObligationTag.VEHICLE
            HOME_INSURANCE -> ObligationTag.HOME
            LAND_INSURANCE -> ObligationTag.LAND
            DRONE_INSURANCE -> ObligationTag.DRONE
            PROPERTY_TAX, LAND_TAX -> ObligationTag.TAX
            OTHER -> ObligationTag.OTHER
        }

    /**
     * Whether records of this category usually expire, as opposed to simply falling due.
     *
     * Drives which date fields the form puts forward; it never *forbids* a date, so a tax with an
     * expiry or an insurance with a payment deadline can still be entered.
     */
    val expires: Boolean
        get() = when (this) {
            VEHICLE_INSURANCE, HOME_INSURANCE, LAND_INSURANCE, DRONE_INSURANCE -> true
            PROPERTY_TAX, LAND_TAX, OTHER -> false
        }
}

/**
 * Badge shown for an obligation.
 *
 * Derived from [ObligationCategory] instead of being stored: the category already carries the
 * information, and a persisted copy would be a second source of truth that can drift. The tag is a
 * domain grouping (both tax categories share one badge), while its label and colors belong to the
 * presentation layer.
 */
enum class ObligationTag {
    VEHICLE,
    HOME,
    LAND,
    DRONE,
    TAX,
    OTHER,
    ;

    /** Categories folded into this tag, used when a tag filter is applied. */
    val categories: Set<ObligationCategory>
        get() = ObligationCategory.entries.filterTo(mutableSetOf()) { it.tag == this }
}
