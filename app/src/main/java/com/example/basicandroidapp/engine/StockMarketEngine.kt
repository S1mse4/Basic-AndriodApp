package com.example.basicandroidapp.engine

import android.os.Handler
import android.os.Looper
import com.example.basicandroidapp.model.GameState
import java.util.Random

class StockMarketEngine {

    private val handler = Handler(Looper.getMainLooper())
    private val rng = Random()
    private val listeners = mutableListOf<OnPricesUpdatedListener>()
    private var isRunning = false

    companion object {
        const val UPDATE_INTERVAL_MS = 8000L
        val instance: StockMarketEngine by lazy { StockMarketEngine() }
    }

    interface OnPricesUpdatedListener {
        fun onPricesUpdated()
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

        // Record portfolio value snapshot for Player Data history (capped at 500 entries)
        GameState.portfolioValueHistory.add(GameState.totalPortfolioValue())
        if (GameState.portfolioValueHistory.size > 500) {
            GameState.portfolioValueHistory.removeAt(0)
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

        notifyListeners()
    }

    private fun notifyListeners() {
        listeners.forEach { it.onPricesUpdated() }
    }
}
