package com.griff.keeper.domain.model

/**
 * A non-negative amount of money stored in minor units (grosze) to avoid floating point errors.
 *
 * The first version of the app supports a single currency ([Currency.PLN]) only, therefore the
 * currency is not part of the value. Persistence keeps a currency column so that multi-currency
 * support can be added later without a breaking schema change.
 */
@JvmInline
value class Money private constructor(val minorUnits: Long) : Comparable<Money> {

    init {
        require(minorUnits >= 0) { "Money cannot be negative, was $minorUnits" }
    }

    val wholeUnits: Long get() = minorUnits / MINOR_UNITS_PER_UNIT
    val fraction: Int get() = (minorUnits % MINOR_UNITS_PER_UNIT).toInt()

    val isZero: Boolean get() = minorUnits == 0L

    operator fun plus(other: Money): Money = Money(minorUnits + other.minorUnits)

    operator fun times(factor: Int): Money {
        require(factor >= 0) { "Factor cannot be negative, was $factor" }
        return Money(minorUnits * factor)
    }

    /** Divides the amount, rounding the result half up to the nearest minor unit. */
    fun dividedBy(divisor: Int): Money {
        require(divisor > 0) { "Divisor must be positive, was $divisor" }
        return Money((minorUnits + divisor / 2) / divisor)
    }

    /** Share of [total] in the range `0f..1f`, or `0f` when [total] is zero. */
    fun shareOf(total: Money): Float =
        if (total.isZero) 0f else minorUnits.toFloat() / total.minorUnits.toFloat()

    override fun compareTo(other: Money): Int = minorUnits.compareTo(other.minorUnits)

    override fun toString(): String = "$wholeUnits.${fraction.toString().padStart(2, '0')}"

    companion object {
        const val MINOR_UNITS_PER_UNIT: Long = 100

        val ZERO: Money = Money(0)

        fun ofMinorUnits(minorUnits: Long): Money = Money(minorUnits)

        fun ofUnits(units: Long, fraction: Int = 0): Money {
            require(fraction in 0..99) { "Fraction must be in 0..99, was $fraction" }
            return Money(units * MINOR_UNITS_PER_UNIT + fraction)
        }
    }
}

fun Iterable<Money>.sum(): Money = fold(Money.ZERO) { acc, money -> acc + money }

/** Currencies known to the domain. The first version is PLN-only. */
enum class Currency(val code: String) {
    PLN("PLN"),
    ;

    companion object {
        val Default: Currency = PLN

        fun fromCode(code: String): Currency =
            entries.firstOrNull { it.code == code } ?: error("Unsupported currency code: $code")
    }
}
