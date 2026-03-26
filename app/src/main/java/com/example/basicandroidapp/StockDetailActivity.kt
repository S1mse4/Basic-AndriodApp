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
    private lateinit var etQuantity: EditText
    private lateinit var tvTransactionSummary: TextView
    private lateinit var btnExecute: Button
    private lateinit var tvOwnedShares: TextView
    private lateinit var tvAvgBuy: TextView
    private lateinit var tvUnrealizedPnL: TextView
    private lateinit var tvAvailableCash: TextView
    private lateinit var btnBuyAll: Button
    private lateinit var btnSellAll: Button
    private lateinit var btnSell: Button

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
        etQuantity = findViewById(R.id.etQuantity)
        tvTransactionSummary = findViewById(R.id.tvTransactionSummary)
        btnExecute = findViewById(R.id.btnExecute)
        tvOwnedShares = findViewById(R.id.tvOwnedShares)
        tvAvgBuy = findViewById(R.id.tvAvgBuy)
        tvUnrealizedPnL = findViewById(R.id.tvUnrealizedPnL)
        tvAvailableCash = findViewById(R.id.tvAvailableCash)
        btnBuyAll = findViewById(R.id.btnBuyAll)
        btnSellAll = findViewById(R.id.btnSellAll)
        btnSell = findViewById(R.id.btnSell)

        etQuantity.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateTransactionSummary()
            }
        })

        btnExecute.setOnClickListener { executeBuy() }
        btnSell.setOnClickListener { executeSell() }

        btnBuyAll.setOnClickListener { executeBuyAll() }
        btnSellAll.setOnClickListener { executeSellAll() }

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

    private fun updateTransactionSummary() {
        val stock = GameState.stocks.find { it.symbol == symbol } ?: return
        val qty = etQuantity.text.toString().toIntOrNull() ?: 0
        if (qty <= 0) {
            tvTransactionSummary.text = "Enter quantity to see cost"
            return
        }
        val total = stock.currentPrice * qty
        val affordable = (GameState.cash / stock.currentPrice).toInt()
        val formattedTotal = "%.2f".format(total)
        tvTransactionSummary.text = "Cost: \$$formattedTotal  ·  Max buy: $affordable\nProceeds: \$$formattedTotal  ·  Owned: ${stock.sharesOwned}"
    }

    private fun executeBuy() {
        val qty = etQuantity.text.toString().toIntOrNull()
        if (qty == null || qty <= 0) {
            Toast.makeText(this, "Please enter a valid quantity", Toast.LENGTH_SHORT).show()
            return
        }
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
    }

    private fun executeSell() {
        val qty = etQuantity.text.toString().toIntOrNull()
        if (qty == null || qty <= 0) {
            Toast.makeText(this, "Please enter a valid quantity", Toast.LENGTH_SHORT).show()
            return
        }
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

    private fun executeBuyAll() {
        val stock = GameState.stocks.find { it.symbol == symbol } ?: return
        val maxQty = (GameState.cash / stock.currentPrice).toInt()
        if (maxQty <= 0) {
            Toast.makeText(this, "Insufficient funds to buy any shares!", Toast.LENGTH_SHORT).show()
            return
        }
        when (GameState.buyStock(symbol, maxQty)) {
            BuyResult.SUCCESS -> {
                Toast.makeText(this, "Bought $maxQty shares of $symbol @ $%.2f".format(stock.currentPrice), Toast.LENGTH_SHORT).show()
                etQuantity.setText("")
                updateUI()
            }
            BuyResult.INSUFFICIENT_FUNDS -> Toast.makeText(this, "Insufficient funds!", Toast.LENGTH_SHORT).show()
            BuyResult.INVALID_QUANTITY -> Toast.makeText(this, "Invalid quantity", Toast.LENGTH_SHORT).show()
            BuyResult.STOCK_NOT_FOUND -> Toast.makeText(this, "Stock not found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun executeSellAll() {
        val stock = GameState.stocks.find { it.symbol == symbol } ?: return
        if (stock.sharesOwned <= 0) {
            Toast.makeText(this, "No shares to sell!", Toast.LENGTH_SHORT).show()
            return
        }
        val qty = stock.sharesOwned
        when (GameState.sellStock(symbol, qty)) {
            SellResult.SUCCESS -> {
                Toast.makeText(this, "Sold all $qty shares of $symbol @ $%.2f".format(stock.currentPrice), Toast.LENGTH_SHORT).show()
                etQuantity.setText("")
                updateUI()
            }
            SellResult.INSUFFICIENT_SHARES -> Toast.makeText(this, "Not enough shares owned!", Toast.LENGTH_SHORT).show()
            SellResult.INVALID_QUANTITY -> Toast.makeText(this, "Invalid quantity", Toast.LENGTH_SHORT).show()
            SellResult.STOCK_NOT_FOUND -> Toast.makeText(this, "Stock not found", Toast.LENGTH_SHORT).show()
        }
    }
}
