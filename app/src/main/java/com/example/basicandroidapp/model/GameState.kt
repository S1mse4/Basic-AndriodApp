package com.example.basicandroidapp.model

object GameState {
    const val STARTING_CASH = 10_000.0
    const val MAX_HISTORY = 40

    // Bank interest rates applied each engine tick
    const val DEPOSIT_INTEREST_RATE = 0.001  // 0.1% per tick on deposits
    const val DEBT_INTEREST_RATE = 0.002     // 0.2% per tick on debt

    var cash: Double = STARTING_CASH
    var daysPassed: Int = 0

    // Bank branch state
    var bankDeposit: Double = 0.0
    var bankDebt: Double = 0.0

    // Player Data: all-time portfolio value snapshots
    val portfolioValueHistory: MutableList<Double> = mutableListOf()

    // Player Data: highest portfolio value ever recorded
    var highestPortfolioValue: Double = STARTING_CASH

    // Player Data: biggest profit on a single share ever recorded (price - avgBuy)
    var biggestSingleShareProfit: Double = 0.0

    val stocks: List<Stock> = listOf(
        Stock("APPL", "AppleCore Technologies", "Technology",   182.50, 180.00, volatility = 0.015),
        Stock("GOGL", "Goggle Systems",          "Technology",   142.80, 141.50, volatility = 0.018),
        Stock("AMZN", "AmazingShop Inc",         "Consumer",     178.60, 176.90, volatility = 0.022),
        Stock("TSLX", "TeslaX Motors",           "Automotive",   248.40, 244.00, volatility = 0.038),
        Stock("MSFC", "MicroSoft Corp",          "Technology",   376.20, 374.50, volatility = 0.012),
        Stock("META", "MetaVerse Inc",           "Social Media", 495.70, 492.00, volatility = 0.025),
        Stock("NVDX", "NvidiaChip Corp",         "Semiconductors", 874.90, 860.00, volatility = 0.040),
        Stock("NFLX", "NetFlicks",               "Entertainment", 617.30, 613.00, volatility = 0.028),
        Stock("JPMC", "JP Morgan Capital",       "Finance",       199.50, 198.00, volatility = 0.014),
        Stock("ENRG", "EnergyFlex Corp",         "Energy",        78.40,  77.50, volatility = 0.030)
    )

    init {
        val rng = java.util.Random(42L)
        for (stock in stocks) {
            var basePrice = stock.currentPrice
            val tempHistory = mutableListOf<Double>()
            for (i in 0 until (MAX_HISTORY - 1)) {
                val change = rng.nextGaussian() * stock.volatility
                basePrice = maxOf(1.0, basePrice * (1.0 - change))
                tempHistory.add(0, basePrice)
            }
            stock.priceHistory.addAll(tempHistory)
            stock.priceHistory.addLast(stock.currentPrice)
        }
    }

    fun buyStock(symbol: String, quantity: Int): BuyResult {
        val stock = stocks.find { it.symbol == symbol } ?: return BuyResult.STOCK_NOT_FOUND
        val totalCost = stock.currentPrice * quantity
        if (totalCost > cash) return BuyResult.INSUFFICIENT_FUNDS
        if (quantity <= 0) return BuyResult.INVALID_QUANTITY
        val prevTotal = stock.averageBuyPrice * stock.sharesOwned
        stock.sharesOwned += quantity
        stock.averageBuyPrice = (prevTotal + totalCost) / stock.sharesOwned
        cash -= totalCost
        return BuyResult.SUCCESS
    }

    fun sellStock(symbol: String, quantity: Int): SellResult {
        val stock = stocks.find { it.symbol == symbol } ?: return SellResult.STOCK_NOT_FOUND
        if (quantity <= 0) return SellResult.INVALID_QUANTITY
        if (quantity > stock.sharesOwned) return SellResult.INSUFFICIENT_SHARES
        cash += stock.currentPrice * quantity
        stock.sharesOwned -= quantity
        if (stock.sharesOwned == 0) stock.averageBuyPrice = 0.0
        return SellResult.SUCCESS
    }

    fun totalPortfolioValue(): Double = cash + holdingsValue() + bankDeposit - bankDebt

    fun holdingsValue(): Double = stocks.sumOf { it.holdingsValue }

    // Bank operations
    fun depositToBank(amount: Double): BankResult {
        if (amount <= 0) return BankResult.INVALID_AMOUNT
        if (amount > cash) return BankResult.INSUFFICIENT_FUNDS
        cash -= amount
        bankDeposit += amount
        return BankResult.SUCCESS
    }

    fun withdrawFromBank(amount: Double): BankResult {
        if (amount <= 0) return BankResult.INVALID_AMOUNT
        if (amount > bankDeposit) return BankResult.INSUFFICIENT_DEPOSIT
        bankDeposit -= amount
        cash += amount
        return BankResult.SUCCESS
    }

    fun borrowFromBank(amount: Double): BankResult {
        if (amount <= 0) return BankResult.INVALID_AMOUNT
        bankDebt += amount
        cash += amount
        return BankResult.SUCCESS
    }

    fun repayDebt(amount: Double): BankResult {
        if (amount <= 0) return BankResult.INVALID_AMOUNT
        val repaid = minOf(amount, bankDebt)
        if (repaid > cash) return BankResult.INSUFFICIENT_FUNDS
        cash -= repaid
        bankDebt -= repaid
        return BankResult.SUCCESS
    }

    fun reset() {
        cash = STARTING_CASH
        daysPassed = 0
        bankDeposit = 0.0
        bankDebt = 0.0
        portfolioValueHistory.clear()
        highestPortfolioValue = STARTING_CASH
        biggestSingleShareProfit = 0.0
        for (stock in stocks) {
            stock.sharesOwned = 0
            stock.averageBuyPrice = 0.0
        }
    }
}

enum class BuyResult { SUCCESS, INSUFFICIENT_FUNDS, STOCK_NOT_FOUND, INVALID_QUANTITY }
enum class SellResult { SUCCESS, INSUFFICIENT_SHARES, STOCK_NOT_FOUND, INVALID_QUANTITY }
enum class BankResult { SUCCESS, INSUFFICIENT_FUNDS, INSUFFICIENT_DEPOSIT, INVALID_AMOUNT }
