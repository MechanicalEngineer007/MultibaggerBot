package com.example.multibaggerbot.data

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object BrokerHelper {

    fun openZerodhaKite(context: Context, symbol: String, price: Double) {
        val cleanSymbol = symbol.replace(".NS", "").replace(".BO", "")
        copyToClipboard(context, cleanSymbol, price, "Zerodha Kite")

        val packages = listOf("zerodha.kite3", "com.zerodha.kite3")
        var launched = false
        for (pkg in packages) {
            val intent = context.packageManager.getLaunchIntentForPackage(pkg)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                launched = true
                break
            }
        }

        if (!launched) {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://kite.zerodha.com")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(browserIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun openGroww(context: Context, symbol: String, price: Double) {
        val cleanSymbol = symbol.replace(".NS", "").replace(".BO", "")
        copyToClipboard(context, cleanSymbol, price, "Groww")

        val intent = context.packageManager.getLaunchIntentForPackage("com.nextbillion.groww")
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } else {
            val webUrl = "https://groww.in/stocks/$cleanSymbol"
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(browserIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun copyToClipboard(context: Context, symbol: String, price: Double, broker: String) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Stock Symbol", symbol)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(
                context,
                "📋 Copied $symbol (CMP: ₹${String.format("%,.1f", price)}) to clipboard! Opening $broker...",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
