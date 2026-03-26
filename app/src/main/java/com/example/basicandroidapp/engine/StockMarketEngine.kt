package com.example.basicandroidapp.engine

import android.os.Handler
import android.os.Looper
import com.example.basicandroidapp.model.GameState
import com.example.basicandroidapp.model.MarketEvent
import java.util.Random

class StockMarketEngine {

    private val handler = Handler(Looper.getMainLooper())
    private val rng = Random()
    private val listeners = mutableListOf<OnPricesUpdatedListener>()
    private var isRunning = false

    // Event timing: fire one event every 3–5 ticks
    private var ticksSinceLastEvent = 0
    private var nextEventTick: Int = rng.nextInt(3) + 3   // 3, 4, or 5

    companion object {
        const val UPDATE_INTERVAL_MS = 8000L
        val instance: StockMarketEngine by lazy { StockMarketEngine() }
    }

    interface OnPricesUpdatedListener {
        fun onPricesUpdated()
        /** Called when a market event fires. Override to react to events in the UI. */
        fun onMarketEvent(event: MarketEvent) {}
    }

    private val tickRunnable = object : Runnable {
        override fun run() {
            if (isRunning) {
                tick()
                handler.postDelayed(this, UPDATE_INTERVAL_MS)
            }
        }
    }

    fun addListener(listener: OnPricesUpdatedListener) {
        if (!listeners.contains(listener)) listeners.add(listener)
    }

    fun removeListener(listener: OnPricesUpdatedListener) {
        listeners.remove(listener)
    }

    fun start() {
        if (!isRunning) {
            isRunning = true
            handler.postDelayed(tickRunnable, UPDATE_INTERVAL_MS)
        }
    }

    fun stop() {
        isRunning = false
        handler.removeCallbacks(tickRunnable)
    }

    private fun tick() {
        GameState.daysPassed++
        for (stock in GameState.stocks) {
            stock.previousClosePrice = stock.currentPrice

            // Geometric Brownian Motion: dS = S * (mu*dt + sigma*dW)
            val drift = 0.0001   // slight upward bias
            val dW = rng.nextGaussian() * stock.volatility
            val newPrice = maxOf(1.0, stock.currentPrice * (1.0 + drift + dW))

            // Occasional market event (1% chance per tick)
            val eventPrice = if (rng.nextDouble() < 0.01) {
                val shock = (rng.nextDouble() - 0.5) * 0.12  // ±6% shock
                maxOf(1.0, newPrice * (1.0 + shock))
            } else {
                newPrice
            }

            stock.currentPrice = eventPrice
            stock.priceHistory.addLast(eventPrice)
            if (stock.priceHistory.size > GameState.MAX_HISTORY) {
                stock.priceHistory.removeFirst()
            }
        }

        // Apply bank deposit interest
        if (GameState.bankDeposit > 0) {
            GameState.bankDeposit *= (1.0 + GameState.DEPOSIT_INTEREST_RATE)
        }
        // Apply debt interest (debt grows over time)
        if (GameState.bankDebt > 0) {
            GameState.bankDebt *= (1.0 + GameState.DEBT_INTEREST_RATE)
        }

        // Record portfolio value snapshot for Player Data history (capped at 500 entries)
        GameState.portfolioValueHistory.add(GameState.totalPortfolioValue())
        if (GameState.portfolioValueHistory.size > 500) {
            GameState.portfolioValueHistory.removeAt(0)
        }

        // Track highest portfolio value ever recorded
        val currentTotal = GameState.totalPortfolioValue()
        if (currentTotal > GameState.highestPortfolioValue) {
            GameState.highestPortfolioValue = currentTotal
        }

        // Track biggest single-share profit (unrealized, without selling)
        for (stock in GameState.stocks) {
            if (stock.sharesOwned > 0) {
                val profitPerShare = stock.currentPrice - stock.averageBuyPrice
                if (profitPerShare > GameState.biggestSingleShareProfit) {
                    GameState.biggestSingleShareProfit = profitPerShare
                }
            }
        }

        // Fire a market event every 3–5 ticks
        ticksSinceLastEvent++
        if (ticksSinceLastEvent >= nextEventTick) {
            triggerMarketEvent()
            ticksSinceLastEvent = 0
            nextEventTick = rng.nextInt(3) + 3
        }

        notifyListeners()
    }

    /**
     * Picks a random event template from the library, applies the price impact to
     * the affected stocks, records the event in GameState.eventHistory, and
     * notifies all listeners so the UI can display it.
     */
    private fun triggerMarketEvent() {
        // Choose event category: 50% company, 30% sector, 20% market-wide
        val roll = rng.nextDouble()
        val template = when {
            roll < 0.50 -> {
                val list = MarketEventLibrary.companyEvents
                list[rng.nextInt(list.size)]
            }
            roll < 0.80 -> {
                val list = MarketEventLibrary.sectorEvents
                list[rng.nextInt(list.size)]
            }
            else -> {
                val list = MarketEventLibrary.marketWideEvents
                list[rng.nextInt(list.size)]
            }
        }

        // Determine which stocks are affected
        val affectedStocks = when (template.eventType) {
            MarketEvent.EventType.COMPANY_SPECIFIC -> {
                val stock = GameState.stocks[rng.nextInt(GameState.stocks.size)]
                listOf(stock)
            }
            MarketEvent.EventType.SECTOR_WIDE -> {
                GameState.stocks.filter { it.sector in template.affectedSectors }
            }
            MarketEvent.EventType.MARKET_WIDE -> {
                GameState.stocks.toList()
            }
        }

        // Nothing to affect – skip (e.g. sector template with no matching stocks)
        if (affectedStocks.isEmpty()) return

        // Pick a random impact between minImpact and maxImpact
        val impact = template.minImpactPercent +
                rng.nextDouble() * (template.maxImpactPercent - template.minImpactPercent)
        val impactFactor = 1.0 + (impact / 100.0)

        // Apply the price change and update the most-recent price history entry
        for (stock in affectedStocks) {
            val newPrice = maxOf(1.0, stock.currentPrice * impactFactor)
            stock.currentPrice = newPrice
            if (stock.priceHistory.isNotEmpty()) {
                stock.priceHistory.removeLast()
                stock.priceHistory.addLast(newPrice)
            }
        }

        // Build and store the event record
        val event = MarketEvent(
            id = GameState.eventHistory.size,
            title = template.title,
            description = template.description,
            affectedSymbols = affectedStocks.map { it.symbol },
            impactPercent = impact,
            eventType = template.eventType,
            dayOccurred = GameState.daysPassed
        )
        GameState.eventHistory.add(0, event)
        if (GameState.eventHistory.size > 20) {
            GameState.eventHistory.removeAt(GameState.eventHistory.size - 1)
        }

        // Tell the UI about the new event
        listeners.forEach { it.onMarketEvent(event) }
    }

    private fun notifyListeners() {
        listeners.forEach { it.onPricesUpdated() }
    }
}
