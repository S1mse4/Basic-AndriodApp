package com.example.basicandroidapp.model

/**
 * Represents a single market news event that happened during gameplay.
 * Events affect one or more stock prices and are shown in the News feed
 * so players can understand why prices moved.
 */
data class MarketEvent(
    val id: Int,
    val title: String,
    val description: String,
    val affectedSymbols: List<String>,  // which stocks were impacted
    val impactPercent: Double,          // e.g. +5.0 means +5%, -3.0 means -3%
    val eventType: EventType,
    val dayOccurred: Int                // game day when the event happened
) {
    /** The three kinds of events that can happen in the market */
    enum class EventType {
        COMPANY_SPECIFIC,   // affects one company's stock
        SECTOR_WIDE,        // affects all stocks in a sector
        MARKET_WIDE         // affects every stock
    }

    /** True when the event caused prices to rise */
    val isPositive: Boolean get() = impactPercent >= 0
}
