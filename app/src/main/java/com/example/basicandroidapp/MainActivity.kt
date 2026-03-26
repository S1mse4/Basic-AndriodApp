package com.example.basicandroidapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.basicandroidapp.engine.StockMarketEngine
import com.example.basicandroidapp.model.BankResult
import com.example.basicandroidapp.model.GameState
import com.example.basicandroidapp.model.MarketEvent
import com.example.basicandroidapp.ui.CombinedStockChartView
import com.example.basicandroidapp.ui.EventAdapter
import com.example.basicandroidapp.ui.PortfolioAdapter
import com.example.basicandroidapp.ui.SortMode
import com.example.basicandroidapp.ui.StockAdapter
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity(), StockMarketEngine.OnPricesUpdatedListener {

    private lateinit var tvTotalValue: TextView
    private lateinit var tvCashBalance: TextView
    private lateinit var tvHoldingsValue: TextView
    private lateinit var tvTotalChange: TextView
    private lateinit var tvDayCounter: TextView
    private lateinit var rvMarket: RecyclerView
    private lateinit var rvPortfolio: RecyclerView
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var marketSection: View
    private lateinit var portfolioSection: View
    private lateinit var playerSection: View
    private lateinit var bankSection: View
    private lateinit var tvEmptyPortfolio: TextView
    private lateinit var tvBiggestProfit: TextView
    private lateinit var tvPortfolioHistory: TextView
    private lateinit var spinnerSort: Spinner
    private lateinit var combinedChart: CombinedStockChartView
    private lateinit var combinedChartSection: View

    // Bank views
    private lateinit var tvBankDeposit: TextView
    private lateinit var tvBankDebt: TextView
    private lateinit var tvDebtLimit: TextView
    private lateinit var etBankAmount: EditText
    private lateinit var etDebtAmount: EditText
    private lateinit var btnDeposit: Button
    private lateinit var btnWithdraw: Button
    private lateinit var btnWithdrawAll: Button
    private lateinit var btnDepositAll: Button
    private lateinit var btnBorrow: Button
    private lateinit var btnRepay: Button
    private lateinit var btnRepayAll: Button

    private lateinit var stockAdapter: StockAdapter
    private lateinit var portfolioAdapter: PortfolioAdapter
    private lateinit var eventAdapter: EventAdapter

    // News / Events feed views
    private lateinit var newsSection: View
    private lateinit var rvEvents: RecyclerView
    private lateinit var tvEmptyNews: TextView

    private val engine = StockMarketEngine.instance
    private var currentTabId: Int = R.id.nav_market

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvTotalValue = findViewById(R.id.tvTotalValue)
        tvCashBalance = findViewById(R.id.tvCashBalance)
        tvHoldingsValue = findViewById(R.id.tvHoldingsValue)
        tvTotalChange = findViewById(R.id.tvTotalChange)
        tvDayCounter = findViewById(R.id.tvDayCounter)
        rvMarket = findViewById(R.id.rvMarket)
        rvPortfolio = findViewById(R.id.rvPortfolio)
        bottomNav = findViewById(R.id.bottomNav)
        marketSection = findViewById(R.id.marketSection)
        portfolioSection = findViewById(R.id.portfolioSection)
        playerSection = findViewById(R.id.playerSection)
        bankSection = findViewById(R.id.bankSection)
        tvEmptyPortfolio = findViewById(R.id.tvEmptyPortfolio)
        tvBiggestProfit = findViewById(R.id.tvBiggestProfit)
        tvPortfolioHistory = findViewById(R.id.tvPortfolioHistory)
        spinnerSort = findViewById(R.id.spinnerSort)
        combinedChart = findViewById(R.id.combinedChart)
        combinedChartSection = findViewById(R.id.combinedChartSection)

        tvBankDeposit = findViewById(R.id.tvBankDeposit)
        tvBankDebt = findViewById(R.id.tvBankDebt)
        tvDebtLimit = findViewById(R.id.tvDebtLimit)
        etBankAmount = findViewById(R.id.etBankAmount)
        etDebtAmount = findViewById(R.id.etDebtAmount)
        btnDeposit = findViewById(R.id.btnDeposit)
        btnWithdraw = findViewById(R.id.btnWithdraw)
        btnWithdrawAll = findViewById(R.id.btnWithdrawAll)
        btnDepositAll = findViewById(R.id.btnDepositAll)
        btnBorrow = findViewById(R.id.btnBorrow)
        btnRepay = findViewById(R.id.btnRepay)
        btnRepayAll = findViewById(R.id.btnRepayAll)

        newsSection = findViewById(R.id.newsSection)
        rvEvents = findViewById(R.id.rvEvents)
        tvEmptyNews = findViewById(R.id.tvEmptyNews)

        stockAdapter = StockAdapter(GameState.stocks) { stock ->
            val intent = Intent(this, StockDetailActivity::class.java)
            intent.putExtra(StockDetailActivity.EXTRA_SYMBOL, stock.symbol)
            startActivity(intent)
        }

        portfolioAdapter = PortfolioAdapter(emptyList()) { stock ->
            val intent = Intent(this, StockDetailActivity::class.java)
            intent.putExtra(StockDetailActivity.EXTRA_SYMBOL, stock.symbol)
            startActivity(intent)
        }

        eventAdapter = EventAdapter(GameState.eventHistory.toList())

        rvMarket.layoutManager = LinearLayoutManager(this)
        rvMarket.adapter = stockAdapter

        rvPortfolio.layoutManager = LinearLayoutManager(this)
        rvPortfolio.adapter = portfolioAdapter

        rvEvents.layoutManager = LinearLayoutManager(this)
        rvEvents.adapter = eventAdapter

        setupSortSpinner()
        setupBankButtons()

        // Dynamically adjust section top margins to match the actual rendered header height.
        // This ensures the content below the sticky header is never obscured, regardless of
        // screen density, font size, or tablet layout changes.
        val headerCard = findViewById<View>(R.id.headerCard)
        headerCard.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                headerCard.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val headerHeight = headerCard.height
                listOf(marketSection, portfolioSection, playerSection, bankSection, newsSection).forEach { section ->
                    val params = section.layoutParams as ViewGroup.MarginLayoutParams
                    params.topMargin = headerHeight
                    section.layoutParams = params
                }
            }
        })

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_market -> {
                    currentTabId = R.id.nav_market
                    marketSection.visibility = View.VISIBLE
                    portfolioSection.visibility = View.GONE
                    bankSection.visibility = View.GONE
                    playerSection.visibility = View.GONE
                    newsSection.visibility = View.GONE
                    updateHeader()
                    true
                }
                R.id.nav_portfolio -> {
                    currentTabId = R.id.nav_portfolio
                    marketSection.visibility = View.GONE
                    portfolioSection.visibility = View.VISIBLE
                    bankSection.visibility = View.GONE
                    playerSection.visibility = View.GONE
                    newsSection.visibility = View.GONE
                    refreshPortfolioList()
                    updateHeader()
                    true
                }
                R.id.nav_bank -> {
                    currentTabId = R.id.nav_bank
                    marketSection.visibility = View.GONE
                    portfolioSection.visibility = View.GONE
                    bankSection.visibility = View.VISIBLE
                    playerSection.visibility = View.GONE
                    newsSection.visibility = View.GONE
                    refreshBankData()
                    updateHeader()
                    true
                }
                R.id.nav_player -> {
                    currentTabId = R.id.nav_player
                    marketSection.visibility = View.GONE
                    portfolioSection.visibility = View.GONE
                    bankSection.visibility = View.GONE
                    playerSection.visibility = View.VISIBLE
                    newsSection.visibility = View.GONE
                    refreshPlayerData()
                    updateHeader()
                    true
                }
                R.id.nav_news -> {
                    currentTabId = R.id.nav_news
                    marketSection.visibility = View.GONE
                    portfolioSection.visibility = View.GONE
                    bankSection.visibility = View.GONE
                    playerSection.visibility = View.GONE
                    newsSection.visibility = View.VISIBLE
                    refreshEventsList()
                    updateHeader()
                    true
                }
                else -> false
            }
        }

        updateHeader()
        engine.start()
    }

    private fun setupSortSpinner() {
        val sortLabels = listOf("Price: Low → High", "Price: High → Low", "My Holdings First")
        val adapter = ArrayAdapter(this, R.layout.spinner_dropdown_item, sortLabels)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinnerSort.adapter = adapter

        spinnerSort.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val mode = when (position) {
                    0 -> SortMode.PRICE_LOW_HIGH
                    1 -> SortMode.PRICE_HIGH_LOW
                    else -> SortMode.OWNED_FIRST
                }
                stockAdapter.setSortMode(mode)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun setupBankButtons() {
        btnDeposit.setOnClickListener {
            val amount = etBankAmount.text.toString().toDoubleOrNull()
            if (amount == null || amount <= 0) {
                Toast.makeText(this, "Enter a valid amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            when (GameState.depositToBank(amount)) {
                BankResult.SUCCESS -> {
                    etBankAmount.setText("")
                    refreshBankData()
                    updateHeader()
                    Toast.makeText(this, "Deposited $${"%.2f".format(amount)}", Toast.LENGTH_SHORT).show()
                }
                BankResult.INSUFFICIENT_FUNDS -> Toast.makeText(this, "Not enough cash!", Toast.LENGTH_SHORT).show()
                else -> Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show()
            }
        }

        btnWithdraw.setOnClickListener {
            val amount = etBankAmount.text.toString().toDoubleOrNull()
            if (amount == null || amount <= 0) {
                Toast.makeText(this, "Enter a valid amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            when (GameState.withdrawFromBank(amount)) {
                BankResult.SUCCESS -> {
                    etBankAmount.setText("")
                    refreshBankData()
                    updateHeader()
                    Toast.makeText(this, "Withdrew $${"%.2f".format(amount)}", Toast.LENGTH_SHORT).show()
                }
                BankResult.INSUFFICIENT_DEPOSIT -> Toast.makeText(this, "Not enough in deposit!", Toast.LENGTH_SHORT).show()
                else -> Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show()
            }
        }

        btnWithdrawAll.setOnClickListener {
            val amount = GameState.bankDeposit
            if (amount <= 0) {
                Toast.makeText(this, "Nothing to withdraw!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            when (GameState.withdrawFromBank(amount)) {
                BankResult.SUCCESS -> {
                    etBankAmount.setText("")
                    refreshBankData()
                    updateHeader()
                    Toast.makeText(this, "Withdrew all $${"%.2f".format(amount)}", Toast.LENGTH_SHORT).show()
                }
                else -> Toast.makeText(this, "Withdrawal failed unexpectedly", Toast.LENGTH_SHORT).show()
            }
        }

        btnDepositAll.setOnClickListener {
            val amount = GameState.cash
            if (amount <= 0) {
                Toast.makeText(this, "No cash to deposit!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            when (GameState.depositToBank(amount)) {
                BankResult.SUCCESS -> {
                    etBankAmount.setText("")
                    refreshBankData()
                    updateHeader()
                    Toast.makeText(this, "Deposited all $${"%.2f".format(amount)}", Toast.LENGTH_SHORT).show()
                }
                else -> Toast.makeText(this, "Deposit failed unexpectedly", Toast.LENGTH_SHORT).show()
            }
        }

        btnBorrow.setOnClickListener {
            val amount = etDebtAmount.text.toString().toDoubleOrNull()
            if (amount == null || amount <= 0) {
                Toast.makeText(this, "Enter a valid amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            when (GameState.borrowFromBank(amount)) {
                BankResult.SUCCESS -> {
                    etDebtAmount.setText("")
                    refreshBankData()
                    updateHeader()
                    Toast.makeText(this, "Borrowed $${"%.2f".format(amount)}", Toast.LENGTH_SHORT).show()
                }
                BankResult.DEBT_LIMIT_REACHED -> Toast.makeText(this, "Debt limit of $${"%.2f".format(GameState.MAX_DEBT)} reached!", Toast.LENGTH_SHORT).show()
                else -> Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show()
            }
        }

        btnRepay.setOnClickListener {
            val amount = etDebtAmount.text.toString().toDoubleOrNull()
            if (amount == null || amount <= 0) {
                Toast.makeText(this, "Enter a valid amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val actualRepaid = minOf(amount, GameState.bankDebt)
            when (GameState.repayDebt(amount)) {
                BankResult.SUCCESS -> {
                    etDebtAmount.setText("")
                    refreshBankData()
                    updateHeader()
                    Toast.makeText(this, "Repaid $${"%.2f".format(actualRepaid)}", Toast.LENGTH_SHORT).show()
                }
                BankResult.INSUFFICIENT_FUNDS -> Toast.makeText(this, "Not enough cash!", Toast.LENGTH_SHORT).show()
                else -> Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show()
            }
        }

        btnRepayAll.setOnClickListener {
            val debt = GameState.bankDebt
            if (debt <= 0) {
                Toast.makeText(this, "No debt to repay!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            when (GameState.repayDebt(debt)) {
                BankResult.SUCCESS -> {
                    etDebtAmount.setText("")
                    refreshBankData()
                    updateHeader()
                    Toast.makeText(this, "Repaid all $${"%.2f".format(debt)}", Toast.LENGTH_SHORT).show()
                }
                BankResult.INSUFFICIENT_FUNDS -> Toast.makeText(this, "Not enough cash to repay all debt!", Toast.LENGTH_SHORT).show()
                else -> Toast.makeText(this, "Repayment failed unexpectedly", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        engine.addListener(this)
        updateHeader()
        stockAdapter.notifyPricesChanged()
        refreshPortfolioList()
    }

    override fun onPause() {
        super.onPause()
        engine.removeListener(this)
    }

    override fun onPricesUpdated() {
        updateHeader()
        stockAdapter.notifyPricesChanged()
        if (portfolioSection.visibility == View.VISIBLE) {
            refreshPortfolioList()
        }
        if (bankSection.visibility == View.VISIBLE) {
            refreshBankData()
        }
        if (playerSection.visibility == View.VISIBLE) {
            refreshPlayerData()
        }
        if (newsSection.visibility == View.VISIBLE) {
            refreshEventsList()
        }
    }

    /** Called by the engine each time a market event fires. */
    override fun onMarketEvent(event: MarketEvent) {
        // Show a brief toast so the player notices the event
        val sign = if (event.impactPercent >= 0) "+" else ""
        val affected = when (event.eventType) {
            MarketEvent.EventType.MARKET_WIDE -> "All Markets"
            MarketEvent.EventType.SECTOR_WIDE -> event.affectedSymbols.joinToString(", ")
            MarketEvent.EventType.COMPANY_SPECIFIC -> event.affectedSymbols.firstOrNull() ?: ""
        }
        Toast.makeText(
            this,
            "📰 ${event.title}: $affected $sign${"%.1f".format(event.impactPercent)}%",
            Toast.LENGTH_LONG
        ).show()

        // If the News tab is open, refresh it immediately
        if (newsSection.visibility == View.VISIBLE) {
            refreshEventsList()
        }
    }

    /** Formats [amount] with k/m abbreviations (e.g. $1.5k, $2.3m). */
    private fun formatMoney(amount: Double): String {
        val abs = kotlin.math.abs(amount)
        val sign = if (amount < 0) "-" else ""
        return when {
            abs >= 1_000_000 -> "$sign$${"%.1f".format(abs / 1_000_000)}m"
            abs >= 1_000 -> "$sign$${"%.1f".format(abs / 1_000)}k"
            else -> "$sign$${"%.2f".format(abs)}"
        }
    }

    /** Formats [amount] as a full decimal string with thousand separators (e.g. $10,000.00). */
    private fun formatMoneyFull(amount: Double): String {
        val sign = if (amount < 0) "-" else ""
        return "$sign$${"%,.2f".format(kotlin.math.abs(amount))}"
    }

    private fun updateHeader() {
        val total = GameState.totalPortfolioValue()
        val cash = GameState.cash
        val holdings = GameState.holdingsValue()
        val pnl = total - GameState.STARTING_CASH
        val pnlSign = if (pnl >= 0) "+" else ""

        val fmt: (Double) -> String = if (currentTabId == R.id.nav_bank) ::formatMoneyFull else ::formatMoney

        tvTotalValue.text = "Cash: ${fmt(cash)}"
        tvCashBalance.text = "Net Worth: ${fmt(total)}"
        tvHoldingsValue.text = "Invested: ${fmt(holdings)}"
        tvTotalChange.text = "P&L: $pnlSign${fmt(pnl)}"
        tvTotalChange.setTextColor(getColor(if (pnl >= 0) R.color.stock_green else R.color.stock_red))
        tvDayCounter.text = "Day ${GameState.daysPassed}"
    }

    private fun refreshPortfolioList() {
        val holdings = GameState.stocks.filter { it.sharesOwned > 0 }
        portfolioAdapter.updateHoldings(holdings)
        tvEmptyPortfolio.visibility = if (holdings.isEmpty()) View.VISIBLE else View.GONE

        if (holdings.isNotEmpty()) {
            val seriesData = holdings.mapIndexed { idx, stock ->
                stock to CombinedStockChartView.SERIES_COLORS[idx % CombinedStockChartView.SERIES_COLORS.size]
            }
            combinedChart.series = seriesData
            combinedChartSection.visibility = View.VISIBLE
        } else {
            combinedChartSection.visibility = View.GONE
        }
    }

    private fun refreshBankData() {
        val formattedDebt = "%.2f".format(GameState.bankDebt)
        tvBankDeposit.text = "$${"%.2f".format(GameState.bankDeposit)}"
        tvBankDebt.text = "$$formattedDebt"
        tvDebtLimit.text = "Debt limit: $${"%.2f".format(GameState.MAX_DEBT)} (used: $$formattedDebt)"
    }

    private fun refreshPlayerData() {
        val profit = GameState.biggestSingleShareProfit
        val sign = if (profit >= 0) "+" else ""
        tvBiggestProfit.text = "$sign$${"%.2f".format(profit)}"
        tvBiggestProfit.setTextColor(getColor(if (profit >= 0) R.color.stock_green else R.color.stock_red))

        val highest = GameState.highestPortfolioValue
        if (GameState.portfolioValueHistory.isEmpty()) {
            tvPortfolioHistory.text = "No data yet — values are recorded each update."
        } else {
            tvPortfolioHistory.text = "$${"%.2f".format(highest)}"
        }
    }

    private fun refreshEventsList() {
        val events = GameState.eventHistory.toList()
        eventAdapter.updateEvents(events)
        tvEmptyNews.visibility = if (events.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        engine.stop()
    }
}

