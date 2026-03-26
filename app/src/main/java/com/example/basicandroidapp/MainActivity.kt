package com.example.basicandroidapp

import android.content.Intent
import android.os.Bundle
import android.view.View
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

    // Bank views
    private lateinit var tvBankDeposit: TextView
    private lateinit var tvBankDebt: TextView
    private lateinit var etBankAmount: EditText
    private lateinit var etDebtAmount: EditText
    private lateinit var btnDeposit: Button
    private lateinit var btnWithdraw: Button
    private lateinit var btnWithdrawAll: Button
    private lateinit var btnBorrow: Button
    private lateinit var btnRepay: Button

    private lateinit var stockAdapter: StockAdapter
    private lateinit var portfolioAdapter: PortfolioAdapter

    private val engine = StockMarketEngine.instance

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

        tvBankDeposit = findViewById(R.id.tvBankDeposit)
        tvBankDebt = findViewById(R.id.tvBankDebt)
        etBankAmount = findViewById(R.id.etBankAmount)
        etDebtAmount = findViewById(R.id.etDebtAmount)
        btnDeposit = findViewById(R.id.btnDeposit)
        btnWithdraw = findViewById(R.id.btnWithdraw)
        btnWithdrawAll = findViewById(R.id.btnWithdrawAll)
        btnBorrow = findViewById(R.id.btnBorrow)
        btnRepay = findViewById(R.id.btnRepay)

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

        rvMarket.layoutManager = LinearLayoutManager(this)
        rvMarket.adapter = stockAdapter

        rvPortfolio.layoutManager = LinearLayoutManager(this)
        rvPortfolio.adapter = portfolioAdapter

        setupSortSpinner()
        setupBankButtons()

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_market -> {
                    marketSection.visibility = View.VISIBLE
                    portfolioSection.visibility = View.GONE
                    bankSection.visibility = View.GONE
                    playerSection.visibility = View.GONE
                    true
                }
                R.id.nav_portfolio -> {
                    marketSection.visibility = View.GONE
                    portfolioSection.visibility = View.VISIBLE
                    bankSection.visibility = View.GONE
                    playerSection.visibility = View.GONE
                    refreshPortfolioList()
                    true
                }
                R.id.nav_bank -> {
                    marketSection.visibility = View.GONE
                    portfolioSection.visibility = View.GONE
                    bankSection.visibility = View.VISIBLE
                    playerSection.visibility = View.GONE
                    refreshBankData()
                    true
                }
                R.id.nav_player -> {
                    marketSection.visibility = View.GONE
                    portfolioSection.visibility = View.GONE
                    bankSection.visibility = View.GONE
                    playerSection.visibility = View.VISIBLE
                    refreshPlayerData()
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
    }

    private fun updateHeader() {
        val total = GameState.totalPortfolioValue()
        val holdings = GameState.holdingsValue()
        val pnl = total - GameState.STARTING_CASH
        val sign = if (pnl >= 0) "+" else ""

        tvTotalValue.text = "$${"%.2f".format(total)}"
        tvCashBalance.text = "Cash: $${"%.2f".format(GameState.cash)}"
        tvHoldingsValue.text = "Invested: $${"%.2f".format(holdings)}"
        tvTotalChange.text = "P&L: $sign$${"%.2f".format(pnl)}"
        tvTotalChange.setTextColor(getColor(if (pnl >= 0) R.color.stock_green else R.color.stock_red))
        tvDayCounter.text = "Day ${GameState.daysPassed}"
    }

    private fun refreshPortfolioList() {
        val holdings = GameState.stocks.filter { it.sharesOwned > 0 }
        portfolioAdapter.updateHoldings(holdings)
        tvEmptyPortfolio.visibility = if (holdings.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun refreshBankData() {
        tvBankDeposit.text = "$${"%.2f".format(GameState.bankDeposit)}"
        tvBankDebt.text = "$${"%.2f".format(GameState.bankDebt)}"
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

    override fun onDestroy() {
        super.onDestroy()
        engine.stop()
    }
}

