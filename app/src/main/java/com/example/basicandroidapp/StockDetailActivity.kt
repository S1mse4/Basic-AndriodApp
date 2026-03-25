package com.example.basicandroidapp

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.basicandroidapp.engine.StockMarketEngine
import com.example.basicandroidapp.model.BuyResult
import com.example.basicandroidapp.model.GameState
import com.example.basicandroidapp.model.SellResult
import com.example.basicandroidapp.ui.PriceChartView
import com.google.android.material.tabs.TabLayout

class StockDetailActivity : AppCompatActivity(), StockMarketEngine.OnPricesUpdatedListener {

    companion object {
        const val EXTRA_SYMBOL = "symbol"
    }

    private lateinit var symbol: String
    private lateinit var tvDetailSymbol: TextView
    private lateinit var tvDetailName: TextView
    private lateinit var tvDetailSector: TextView
    private lateinit var tvDetailPrice: TextView
    private lateinit var tvDetailChange: TextView
    private lateinit var tvDetailHighLow: TextView
    private lateinit var priceChart: PriceChartView
    private lateinit var tabLayout: TabLayout
    private lateinit var etQuantity: EditText
    private lateinit var tvTransactionSummary: TextView
    private lateinit var btnExecute: Button
    private lateinit var tvOwnedShares: TextView
    private lateinit var tvAvgBuy: TextView
    private lateinit var tvUnrealizedPnL: TextView
    private lateinit var tvAvailableCash: TextView

    private var isBuyMode = true
    private val engine = StockMarketEngine.instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stock_detail)

        symbol = intent.getStringExtra(EXTRA_SYMBOL) ?: run {
            finish()
            return
        }

        supportActionBar?.apply {
            title = symbol
            setDisplayHomeAsUpEnabled(true)
        }

        tvDetailSymbol = findViewById(R.id.tvDetailSymbol)
        tvDetailName = findViewById(R.id.tvDetailName)
        tvDetailSector = findViewById(R.id.tvDetailSector)
        tvDetailPrice = findViewById(R.id.tvDetailPrice)
        tvDetailChange = findViewById(R.id.tvDetailChange)
        tvDetailHighLow = findViewById(R.id.tvDetailHighLow)
        priceChart = findViewById(R.id.priceChart)
        tabLayout = findViewById(R.id.tabLayout)
        etQuantity = findViewById(R.id.etQuantity)
        tvTransactionSummary = findViewById(R.id.tvTransactionSummary)
        btnExecute = findViewById(R.id.btnExecute)
        tvOwnedShares = findViewById(R.id.tvOwnedShares)
        tvAvgBuy = findViewById(R.id.tvAvgBuy)
        tvUnrealizedPnL = findViewById(R.id.tvUnrealizedPnL)
        tvAvailableCash = findViewById(R.id.tvAvailableCash)

        tabLayout.addTab(tabLayout.newTab().setText("BUY"))
        tabLayout.addTab(tabLayout.newTab().setText("SELL"))

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                isBuyMode = tab.position == 0
                updateExecuteButton()
                updateTransactionSummary()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        etQuantity.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateTransactionSummary()
            }
        })

        btnExecute.setOnClickListener { executeTransaction() }

        updateUI()
    }

    override fun onResume() {
        super.onResume()
        engine.addListener(this)
        updateUI()
    }

    override fun onPause() {
        super.onPause()
        engine.removeListener(this)
    }

    override fun onPricesUpdated() {
        updateUI()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun updateUI() {
        val stock = GameState.stocks.find { it.symbol == symbol } ?: return

        tvDetailSymbol.text = stock.symbol
        tvDetailName.text = stock.companyName
        tvDetailSector.text = stock.sector
        tvDetailPrice.text = "$%.2f".format(stock.currentPrice)

        val sign = if (stock.priceChange >= 0) "+" else ""
        tvDetailChange.text = "$sign${"%.2f".format(stock.priceChange)} ($sign${"%.2f".format(stock.priceChangePercent)}%)"

        val greenColor = getColor(R.color.stock_green)
        val redColor = getColor(R.color.stock_red)
        val changeColor = if (stock.isUp) greenColor else redColor
        tvDetailChange.setTextColor(changeColor)
        tvDetailPrice.setTextColor(changeColor)

        val history = stock.priceHistory.toList()
        if (history.isNotEmpty()) {
            val high = history.max()
            val low = history.min()
            tvDetailHighLow.text = "H: $%.2f  L: $%.2f".format(high, low)
        }

        priceChart.priceHistory = history
        priceChart.isPositive = stock.isUp

        tvOwnedShares.text = "Owned: ${stock.sharesOwned} shares"
        tvAvailableCash.text = "Cash: $${"%.2f".format(GameState.cash)}"

        if (stock.sharesOwned > 0) {
            tvAvgBuy.text = "Avg Buy: $%.2f".format(stock.averageBuyPrice)
            val pnlSign = if (stock.unrealizedPnL >= 0) "+" else ""
            tvUnrealizedPnL.text = "P&L: $pnlSign$${"%.2f".format(stock.unrealizedPnL)} ($pnlSign${"%.2f".format(stock.unrealizedPnLPercent)}%)"
            tvUnrealizedPnL.setTextColor(if (stock.unrealizedPnL >= 0) greenColor else redColor)
        } else {
            tvAvgBuy.text = "Avg Buy: —"
            tvUnrealizedPnL.text = "P&L: —"
            tvUnrealizedPnL.setTextColor(getColor(R.color.text_secondary))
        }

        updateTransactionSummary()
    }

    private fun updateExecuteButton() {
        btnExecute.text = if (isBuyMode) "BUY" else "SELL"
        btnExecute.backgroundTintList = android.content.res.ColorStateList.valueOf(
            getColor(if (isBuyMode) R.color.stock_green else R.color.stock_red)
        )
    }

    private fun updateTransactionSummary() {
        val stock = GameState.stocks.find { it.symbol == symbol } ?: return
        val qty = etQuantity.text.toString().toIntOrNull() ?: 0
        if (qty <= 0) {
            tvTransactionSummary.text = "Enter quantity to see cost"
            return
        }
        val total = stock.currentPrice * qty
        if (isBuyMode) {
            val affordable = (GameState.cash / stock.currentPrice).toInt()
            tvTransactionSummary.text = "Cost: $%.2f\nMax you can buy: $affordable shares".format(total)
        } else {
            tvTransactionSummary.text = "Proceeds: $%.2f\nOwned: ${stock.sharesOwned} shares".format(total)
        }
    }

    private fun executeTransaction() {
        val qty = etQuantity.text.toString().toIntOrNull()
        if (qty == null || qty <= 0) {
            Toast.makeText(this, "Please enter a valid quantity", Toast.LENGTH_SHORT).show()
            return
        }

        if (isBuyMode) {
            when (GameState.buyStock(symbol, qty)) {
                BuyResult.SUCCESS -> {
                    val stock = GameState.stocks.find { it.symbol == symbol }!!
                    Toast.makeText(this, "Bought $qty shares of $symbol @ $%.2f".format(stock.currentPrice), Toast.LENGTH_SHORT).show()
                    etQuantity.setText("")
                    updateUI()
                }
                BuyResult.INSUFFICIENT_FUNDS -> Toast.makeText(this, "Insufficient funds!", Toast.LENGTH_SHORT).show()
                BuyResult.INVALID_QUANTITY -> Toast.makeText(this, "Invalid quantity", Toast.LENGTH_SHORT).show()
                BuyResult.STOCK_NOT_FOUND -> Toast.makeText(this, "Stock not found", Toast.LENGTH_SHORT).show()
            }
        } else {
            when (GameState.sellStock(symbol, qty)) {
                SellResult.SUCCESS -> {
                    val stock = GameState.stocks.find { it.symbol == symbol }!!
                    Toast.makeText(this, "Sold $qty shares of $symbol @ $%.2f".format(stock.currentPrice), Toast.LENGTH_SHORT).show()
                    etQuantity.setText("")
                    updateUI()
                }
                SellResult.INSUFFICIENT_SHARES -> Toast.makeText(this, "Not enough shares owned!", Toast.LENGTH_SHORT).show()
                SellResult.INVALID_QUANTITY -> Toast.makeText(this, "Invalid quantity", Toast.LENGTH_SHORT).show()
                SellResult.STOCK_NOT_FOUND -> Toast.makeText(this, "Stock not found", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
