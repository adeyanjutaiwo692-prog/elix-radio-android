package com.elixradio.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private val background = Color.rgb(10, 12, 22)
    private val card = Color.rgb(27, 31, 47)
    private val gold = Color.rgb(224, 185, 111)
    private val ivory = Color.rgb(250, 248, 255)
    private val soft = Color.rgb(195, 192, 207)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(30, 20, 45)
        window.navigationBarColor = background
        window.decorView.setBackgroundColor(background)

        val scroll = ScrollView(this)
        scroll.setBackgroundColor(background)
        scroll.isFillViewport = true

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(dp(22), dp(30), dp(22), dp(36))
        root.setBackgroundColor(background)
        scroll.addView(root)
        setContentView(scroll)

        addText(root, "ELIX CHRISTIAN MEDIA NETWORK", 12, gold, true)
        addSpace(root, 12)
        addText(root, "ELIX RADIO", 38, ivory, true)
        addText(root, "Faith. Truth. Inspiration.", 16, soft, false)
        addSpace(root, 28)

        val liveCard = makeCard()
        addText(liveCard, "●  LIVE PROGRAMME", 12, gold, true)
        addSpace(liveCard, 12)
        addText(liveCard, "Stay connected to the Word", 24, ivory, true)
        addSpace(liveCard, 8)
        addText(liveCard, "Messages, worship, conversations and live programmes to strengthen your walk with God.", 15, soft, false)
        addSpace(liveCard, 18)
        val listen = Button(this)
        listen.text = "▶  LISTEN / WATCH LIVE"
        listen.textSize = 15f
        listen.setTextColor(Color.rgb(25, 20, 30))
        listen.setTypeface(null, Typeface.BOLD)
        listen.setBackgroundColor(gold)
        listen.isAllCaps = false
        listen.setOnClickListener { openLive() }
        liveCard.addView(listen, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(54)
        ))
        addSpace(liveCard, 12)
        addText(liveCard, "Glory Realm Global  ·  Minister PAUL AYO", 13, soft, false)
        root.addView(liveCard, fullWidth())

        addSpace(root, 26)
        addText(root, "EXPLORE ELIX RADIO", 19, ivory, true)
        addSpace(root, 12)
        addTile(root, "SERMONS", "Preaching and teaching of the Word")
        addTile(root, "WORSHIP", "Worship and praise moments")
        addTile(root, "INTERVIEWS", "Conversations and testimonies")
        addTile(root, "LIVE PROGRAMMES", "Special broadcasts and events")

        addSpace(root, 22)
        addText(root, "OUR FIRST MAIN VOICE", 18, ivory, true)
        addSpace(root, 8)
        addText(root, "Minister PAUL AYO", 17, gold, true)
        addText(root, "Featuring invited ministers and presenters as ELIX Radio grows.", 14, soft, false)
        addSpace(root, 28)
        addText(root, "ELIX RADIO  ·  ELIX CHRISTIAN MEDIA NETWORK", 11, soft, false)
    }

    private fun openLive() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=zYNNWEN9Ujc")))
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open the live programme.", Toast.LENGTH_LONG).show()
        }
    }

    private fun makeCard() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(18), dp(18), dp(18), dp(18))
        setBackgroundColor(card)
    }

    private fun addTile(parent: LinearLayout, heading: String, detail: String) {
        val tile = makeCard()
        addText(tile, heading, 16, gold, true)
        addSpace(tile, 6)
        addText(tile, detail, 14, soft, false)
        val params = fullWidth()
        params.bottomMargin = dp(10)
        parent.addView(tile, params)
    }

    private fun addText(parent: LinearLayout, value: String, size: Int, color: Int, bold: Boolean) {
        val view = TextView(this)
        view.text = value
        view.textSize = size.toFloat()
        view.setTextColor(color)
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD)
        view.gravity = Gravity.START
        view.setLineSpacing(dp(3).toFloat(), 1f)
        parent.addView(view, fullWidth())
    }

    private fun addSpace(parent: LinearLayout, height: Int) {
        val space = View(this)
        parent.addView(space, LinearLayout.LayoutParams(1, dp(height)))
    }

    private fun fullWidth() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    )

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
