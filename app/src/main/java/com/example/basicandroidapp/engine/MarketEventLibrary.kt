package com.example.basicandroidapp.engine

import com.example.basicandroidapp.model.MarketEvent

/**
 * A blueprint used to generate a specific market event.
 *
 * @param title            Short headline shown in the news feed
 * @param description      One-sentence explanation of what happened
 * @param eventType        Whether this affects one company, a sector, or all stocks
 * @param minImpactPercent Smallest price change (e.g. +2.0 or -2.0)
 * @param maxImpactPercent Largest price change (e.g. +5.0 or -5.0)
 * @param affectedSectors  For SECTOR_WIDE events: which sectors are hit (empty = all)
 */
data class EventTemplate(
    val title: String,
    val description: String,
    val eventType: MarketEvent.EventType,
    val minImpactPercent: Double,
    val maxImpactPercent: Double,
    val affectedSectors: List<String> = emptyList()
)

/**
 * All pre-defined market event templates.
 * The engine picks randomly from these lists each time an event fires.
 * Add more entries here to extend the event system with new events.
 */
object MarketEventLibrary {

    // ── Company-specific events (single random stock) ──────────────────────────

    val companyEvents = listOf(
        EventTemplate(
            "New Product Launch",
            "Company announces exciting new product lineup",
            MarketEvent.EventType.COMPANY_SPECIFIC, 4.0, 6.0
        ),
        EventTemplate(
            "Positive Earnings Report",
            "Quarterly earnings beat analyst expectations",
            MarketEvent.EventType.COMPANY_SPECIFIC, 3.0, 5.0
        ),
        EventTemplate(
            "Partnership Announcement",
            "Company signs a major strategic partnership deal",
            MarketEvent.EventType.COMPANY_SPECIFIC, 2.0, 4.0
        ),
        EventTemplate(
            "Executive Resignation",
            "CEO announces unexpected resignation",
            MarketEvent.EventType.COMPANY_SPECIFIC, -2.0, -4.0
        ),
        EventTemplate(
            "Recall / Safety Issue",
            "Product recall announced due to safety concerns",
            MarketEvent.EventType.COMPANY_SPECIFIC, -3.0, -5.0
        ),
        EventTemplate(
            "Legal Issue",
            "Company faces a new lawsuit or regulatory probe",
            MarketEvent.EventType.COMPANY_SPECIFIC, -2.0, -3.0
        )
    )

    // ── Sector-wide events (all stocks in the affected sectors) ─────────────────

    val sectorEvents = listOf(
        EventTemplate(
            "Tech Sector Optimism",
            "Analysts upgrade their outlook for technology stocks",
            MarketEvent.EventType.SECTOR_WIDE, 2.0, 3.0,
            listOf("Technology", "Semiconductors")
        ),
        EventTemplate(
            "Tech Regulation Concerns",
            "Governments propose new rules for the tech industry",
            MarketEvent.EventType.SECTOR_WIDE, -2.0, -3.0,
            listOf("Technology", "Semiconductors")
        ),
        EventTemplate(
            "Social Media Ad Boom",
            "Digital advertising revenue hits record highs",
            MarketEvent.EventType.SECTOR_WIDE, 2.0, 4.0,
            listOf("Social Media")
        ),
        EventTemplate(
            "Banking Crisis Concerns",
            "Banking sector faces unexpected liquidity pressures",
            MarketEvent.EventType.SECTOR_WIDE, -3.0, -5.0,
            listOf("Finance")
        ),
        EventTemplate(
            "Energy Price Surge",
            "Global oil and energy prices spike sharply",
            MarketEvent.EventType.SECTOR_WIDE, 2.0, 4.0,
            listOf("Energy")
        ),
        EventTemplate(
            "Consumer Spending Boost",
            "Retail sales data shows strong consumer demand",
            MarketEvent.EventType.SECTOR_WIDE, 2.0, 3.0,
            listOf("Consumer")
        ),
        EventTemplate(
            "Auto Industry Challenges",
            "Supply chain disruptions hit the automotive sector",
            MarketEvent.EventType.SECTOR_WIDE, -2.0, -4.0,
            listOf("Automotive")
        ),
        EventTemplate(
            "Streaming Wars Intensify",
            "New streaming rivals threaten entertainment companies",
            MarketEvent.EventType.SECTOR_WIDE, -1.0, -3.0,
            listOf("Entertainment")
        )
    )

    // ── Market-wide events (every stock is affected) ────────────────────────────

    val marketWideEvents = listOf(
        EventTemplate(
            "Bull Market Confidence",
            "Investor confidence reaches a yearly high",
            MarketEvent.EventType.MARKET_WIDE, 1.0, 2.0
        ),
        EventTemplate(
            "Market Recession Fears",
            "Economic indicators signal a possible recession ahead",
            MarketEvent.EventType.MARKET_WIDE, -2.0, -3.0
        ),
        EventTemplate(
            "Interest Rate Hike",
            "Central bank raises interest rates to fight inflation",
            MarketEvent.EventType.MARKET_WIDE, -1.0, -2.0
        ),
        EventTemplate(
            "Economic Growth Report",
            "GDP growth data exceeds analyst forecasts",
            MarketEvent.EventType.MARKET_WIDE, 1.0, 1.5
        ),
        EventTemplate(
            "Inflation Data Released",
            "Consumer prices rose more than expected last month",
            MarketEvent.EventType.MARKET_WIDE, -1.0, -2.0
        ),
        EventTemplate(
            "Strong Jobs Report",
            "Unemployment falls to a multi-year low",
            MarketEvent.EventType.MARKET_WIDE, 1.0, 2.0
        )
    )
}
