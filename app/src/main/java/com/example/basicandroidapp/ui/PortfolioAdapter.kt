package com.example.basicandroidapp.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.basicandroidapp.R
import com.example.basicandroidapp.model.Stock

class PortfolioAdapter(
    holdings: List<Stock>,
    private val onStockClick: (Stock) -> Unit
) : RecyclerView.Adapter<PortfolioAdapter.HoldingViewHolder>() {

    private val items: MutableList<Stock> = holdings.toMutableList()

    fun updateHoldings(newHoldings: List<Stock>) {
        items.clear()
        items.addAll(newHoldings)
        notifyDataSetChanged()
    }

    inner class HoldingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val symbolText: TextView = itemView.findViewById(R.id.tvHoldingSymbol)
        val nameText: TextView = itemView.findViewById(R.id.tvHoldingName)
        val sharesText: TextView = itemView.findViewById(R.id.tvShares)
        val valueText: TextView = itemView.findViewById(R.id.tvHoldingValue)
        val pnlText: TextView = itemView.findViewById(R.id.tvPnL)
        val avgPriceText: TextView = itemView.findViewById(R.id.tvAvgPrice)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HoldingViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_portfolio_holding, parent, false)
        return HoldingViewHolder(view)
    }

    override fun onBindViewHolder(holder: HoldingViewHolder, position: Int) {
        val stock = items[position]
        val ctx = holder.itemView.context

        holder.symbolText.text = stock.symbol
        holder.nameText.text = stock.companyName
        holder.sharesText.text = "${stock.sharesOwned} shares"
        holder.valueText.text = "$%.2f".format(stock.holdingsValue)
        holder.avgPriceText.text = "Avg: $%.2f".format(stock.averageBuyPrice)

        val sign = if (stock.unrealizedPnL >= 0) "+" else ""
        holder.pnlText.text = "$sign${"%.2f".format(stock.unrealizedPnL)} ($sign${"%.2f".format(stock.unrealizedPnLPercent)}%)"

        val greenColor = ctx.getColor(R.color.stock_green)
        val redColor = ctx.getColor(R.color.stock_red)
        holder.pnlText.setTextColor(if (stock.unrealizedPnL >= 0) greenColor else redColor)

        holder.itemView.setOnClickListener { onStockClick(stock) }
    }

    override fun getItemCount() = items.size
}
