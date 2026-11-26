package com.elixradio.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private val bg = Color.rgb(9, 11, 19)
    private val gold = Color.rgb(213, 174, 102)
    private val white = Color.rgb(247, 245, 255)
    private val muted = Color.rgb(190, 185, 202)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(24, 18, 39)
        window.navigationBarColor = bg
        window.decorView.setBackgroundColor(bg)

        val scroll = ScrollView(this)
        scroll.setBackgroundColor(bg)
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(28), dp(22), dp(30))
            setBackgroundColor(bg)
        }
        scroll.addView(page)
        setContentView(scroll)

        page.addView(label("ELIX CHRISTIAN MEDIA NETWORK", 12, gold, true))
        page.addView(space(12))
        page.addView(label("ELIX RADIO", 36, white, true))
        page.addView(label("Faith. Truth. Inspiration.", 15, muted, false))
        page.addView(space(28))

        val live = panel()
        live.addView(label("●  LIVE PROGRAMME", 12, gold, true))
        live.addView(space(12))
        live.addView(label("Stay connected to the Word", 23, white, true))
        live.addView(space(8))
        live.addView(label("Messages, worship, conversations and live programmes to strengthen your walk with God.", 15, muted, false))
        live.addView(space(18))
        live.addView(actionButton("▶  LISTEN / WATCH LIVE") {
            openLive()
        })
        live.addView(space(12))
        live.addView(label("Glory Realm Global  ·  Minister PAUL AYO", 13, muted, false))
        page.addView(live)

        page.addView(space(26))
        page.addView(label("EXPLORE ELIX RADIO", 19, white, true))
        page.addView(space(12))
        val items = listOf(
            "SERMONS" to "Preaching and teaching of the Word",
            "WORSHIP" to "Worship and praise moments",
            "INTERVIEWS" to "Conversations and testimonies",
            "LIVE PROGRAMMES" to "Special broadcasts and events"
        )
        items.forEach { (title, desc) ->
            val tile = panel()
            tile.addView(label(title, 16, gold, true))
            tile.addView(space(6))
            tile.addView(label(desc, 14, muted, false))
            page.addView(tile)
            page.addView(space(10))
        }

        page.addView(space(18))
        page.addView(label("OUR FIRST MAIN VOICE", 18, white, true))
        page.addView(space(8))
        page.addView(label("Minister PAUL AYO", 17, gold, true))
        page.addView(label("Featuring invited ministers and presenters as ELIX Radio grows.", 14, muted, false))
        page.addView(space(30))
        page.addView(label("ELIX RADIO  ·  ELIX CHRISTIAN MEDIA NETWORK", 11, muted, false))
    }

    private fun openLive() {
        val url = "https://www.youtube.com/watch?v=zYNNWEN9Ujc"
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {
            Toast.makeText(this, "Unable to open the live programme.", Toast.LENGTH_LONG).show()
        }
    }

    private fun panel(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(17), dp(17), dp(17), dp(17))
        setBackgroundColor(Color.rgb(23, 27, 41))
        val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.bottomMargin = dp(2)
        layoutParams = params
    }

    private fun label(value: String, size: Int, color: Int, bold: Boolean): TextView =
        TextView(this).apply {
            text = value
            textSize = size.toFloat()
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.START
            setLineSpacing(dp(3).toFloat(), 1f)
        }

    private fun actionButton(value: String, action: () -> Unit): Button =
        Button(this).apply {
            text = value
            setTextColor(Color.rgb(23, 18, 30))
            textSize = 14f
            setTypeface(typeface, Typeface.BOLD)
            setBackgroundColor(gold)
            isAllCaps = false
            setOnClickListener { action() }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54))
        }

    private fun space(height: Int) = android.view.View(this).apply {
        layoutParams = LinearLayout.LayoutParams(1, dp(height))
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
