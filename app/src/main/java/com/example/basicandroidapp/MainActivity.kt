package com.example.basicandroidapp

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.basicandroidapp.engine.StockMarketEngine
import com.example.basicandroidapp.model.GameState
import com.example.basicandroidapp.ui.PortfolioAdapter
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
    private lateinit var marketSection: android.view.View
    private lateinit var portfolioSection: android.view.View
    private lateinit var tvEmptyPortfolio: TextView

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
        tvEmptyPortfolio = findViewById(R.id.tvEmptyPortfolio)

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

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_market -> {
                    marketSection.visibility = android.view.View.VISIBLE
                    portfolioSection.visibility = android.view.View.GONE
                    true
                }
                R.id.nav_portfolio -> {
                    marketSection.visibility = android.view.View.GONE
                    portfolioSection.visibility = android.view.View.VISIBLE
                    refreshPortfolioList()
                    true
                }
                else -> false
            }
        }

        updateHeader()
        engine.start()
    }

    override fun onResume() {
        super.onResume()
        engine.addListener(this)
        updateHeader()
        stockAdapter.notifyDataSetChanged()
        refreshPortfolioList()
    }

    override fun onPause() {
        super.onPause()
        engine.removeListener(this)
    }

    override fun onPricesUpdated() {
        updateHeader()
        stockAdapter.notifyDataSetChanged()
        if (portfolioSection.visibility == android.view.View.VISIBLE) {
            refreshPortfolioList()
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
        tvEmptyPortfolio.visibility = if (holdings.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        engine.stop()
    }
}

