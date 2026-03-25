package com.example.basicandroidapp.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.basicandroidapp.R
import com.example.basicandroidapp.model.Stock

enum class SortMode { PRICE_LOW_HIGH, PRICE_HIGH_LOW, OWNED_FIRST }

class StockAdapter(
    private val allStocks: List<Stock>,
    private val onStockClick: (Stock) -> Unit
) : RecyclerView.Adapter<StockAdapter.StockViewHolder>() {

    private var sortMode: SortMode = SortMode.PRICE_LOW_HIGH
    private val displayedStocks: MutableList<Stock> = mutableListOf()

    init {
        applySorting()
    }

    fun setSortMode(mode: SortMode) {
        sortMode = mode
        applySorting()
    }

    /** Re-applies the current sort (call after price updates). */
    fun notifyPricesChanged() {
        applySorting()
    }

    private fun applySorting() {
        displayedStocks.clear()
        displayedStocks.addAll(
            when (sortMode) {
                SortMode.PRICE_LOW_HIGH -> allStocks.sortedBy { it.currentPrice }
                SortMode.PRICE_HIGH_LOW -> allStocks.sortedByDescending { it.currentPrice }
                SortMode.OWNED_FIRST -> allStocks.sortedWith(
                    compareByDescending<Stock> { it.sharesOwned > 0 }.thenBy { it.symbol }
                )
            }
        )
        notifyDataSetChanged()
    }

    inner class StockViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val symbolText: TextView = itemView.findViewById(R.id.tvSymbol)
        val nameText: TextView = itemView.findViewById(R.id.tvCompanyName)
        val priceText: TextView = itemView.findViewById(R.id.tvPrice)
        val changeText: TextView = itemView.findViewById(R.id.tvChange)
        val sectorText: TextView = itemView.findViewById(R.id.tvSector)
        val miniChart: PriceChartView = itemView.findViewById(R.id.miniChart)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StockViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_stock, parent, false)
        return StockViewHolder(view)
    }

    override fun onBindViewHolder(holder: StockViewHolder, position: Int) {
        val stock = displayedStocks[position]
        val ctx = holder.itemView.context

        holder.symbolText.text = stock.symbol
        holder.nameText.text = stock.companyName
        holder.sectorText.text = stock.sector
        holder.priceText.text = "$%.2f".format(stock.currentPrice)

        val sign = if (stock.priceChange >= 0) "+" else ""
        holder.changeText.text = "$sign${"%.2f".format(stock.priceChange)} ($sign${"%.2f".format(stock.priceChangePercent)}%)"

        val greenColor = ctx.getColor(R.color.stock_green)
        val redColor = ctx.getColor(R.color.stock_red)
        val changeColor = if (stock.isUp) greenColor else redColor
        holder.changeText.setTextColor(changeColor)
        holder.priceText.setTextColor(changeColor)

        holder.miniChart.priceHistory = stock.priceHistory.toList()
        holder.miniChart.isPositive = stock.isUp

        holder.itemView.setOnClickListener { onStockClick(stock) }
    }

    override fun getItemCount() = displayedStocks.size
}
