package com.example.basicandroidapp.model

data class Stock(
    val symbol: String,
    val companyName: String,
    val sector: String,
    var currentPrice: Double,
    var previousClosePrice: Double,
    val priceHistory: ArrayDeque<Double> = ArrayDeque(),
    val volatility: Double = 0.02,
    var sharesOwned: Int = 0,
    var averageBuyPrice: Double = 0.0
) {
    val priceChange: Double get() = currentPrice - previousClosePrice
    val priceChangePercent: Double get() = if (previousClosePrice != 0.0) (priceChange / previousClosePrice) * 100.0 else 0.0
    val isUp: Boolean get() = currentPrice >= previousClosePrice
    val holdingsValue: Double get() = currentPrice * sharesOwned
    val unrealizedPnL: Double get() = if (sharesOwned > 0) (currentPrice - averageBuyPrice) * sharesOwned else 0.0
    val unrealizedPnLPercent: Double get() = if (averageBuyPrice != 0.0) ((currentPrice - averageBuyPrice) / averageBuyPrice) * 100.0 else 0.0
}
