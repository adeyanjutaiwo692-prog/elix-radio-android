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
    private val bg = Color.rgb(10, 12, 24)
    private val panel = Color.rgb(27, 32, 51)
    private val gold = Color.rgb(226, 187, 105)
    private val white = Color.rgb(250, 248, 255)
    private val muted = Color.rgb(190, 193, 208)
    private lateinit var content: LinearLayout
    private lateinit var nav: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(25, 18, 43)
        window.navigationBarColor = bg
        window.decorView.setBackgroundColor(bg)

        val shell = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
        }
        val scroll = ScrollView(this).apply { isFillViewport = true; setBackgroundColor(bg) }
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(28))
            setBackgroundColor(bg)
        }
        scroll.addView(content)
        shell.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(5), dp(8), dp(5), dp(8))
            setBackgroundColor(Color.rgb(19, 23, 39))
            gravity = Gravity.CENTER
        }
        shell.addView(nav, LinearLayout.LayoutParams(-1, dp(62)))
        setContentView(shell)
        showHome()
    }

    private fun setNav(active: String) {
        nav.removeAllViews()
        listOf("Home", "Live", "Library", "Ministries", "Upcoming", "Account").forEach { label ->
            val b = TextView(this).apply {
                text = label
                textSize = 10f
                gravity = Gravity.CENTER
                setTypeface(null, if (label == active) Typeface.BOLD else Typeface.NORMAL)
                setTextColor(if (label == active) gold else muted)
                setPadding(dp(3), dp(8), dp(3), dp(8))
                setOnClickListener {
                    when (label) {
                        "Home" -> showHome()
                        "Live" -> showLive()
                        "Library" -> showLibrary()
                        "Ministries" -> showMinistries()
                        "Upcoming" -> showUpcoming()
                        "Account" -> showAccount()
                    }
                }
            }
            nav.addView(b, LinearLayout.LayoutParams(0, -1, 1f))
        }
    }

    private fun base(active: String, eyebrow: String, title: String, subtitle: String) {
        content.removeAllViews()
        setNav(active)
        text(content, eyebrow.uppercase(), 11, gold, true)
        space(content, 9)
        text(content, title, 32, white, true)
        text(content, subtitle, 15, muted, false)
        space(content, 22)
    }

    private fun showHome() {
        base("Home", "ELIX Christian Media Network", "ELIX RADIO", "Faith. Truth. Inspiration.")
        val live = card(content)
        text(live, "●  LIVE PROGRAMME", 12, gold, true)
        space(live, 10)
        text(live, "Stay connected to the Word", 23, white, true)
        space(live, 8)
        text(live, "Discover authorised live broadcasts, Christian radio, sermons and programmes from ministries across Nigeria.", 15, muted, false)
        space(live, 16)
        button(live, "▶  LISTEN / WATCH LIVE") { openLive() }
        space(live, 10)
        text(live, "Glory Realm Global  ·  Minister PAUL AYO", 13, muted, false)
        space(content, 22)
        text(content, "DISCOVER CATALOGUES", 18, white, true)
        space(content, 10)
        listOf(
            "Sermons" to "Preaching and teaching of the Word",
            "Worship" to "Worship and praise sessions",
            "Conferences" to "Conventions, crusades and meetings",
            "Prayer" to "Prayer meetings and intercession",
            "Interviews & Podcasts" to "Conversations and testimonies",
            "Bible Study" to "Bible teaching and discipleship",
            "Children & Family" to "Faith-building family programmes"
        ).forEach { (a,b) -> tile(content,a,b) { showLibrary() } }
        space(content, 16)
        text(content, "FEATURED MINISTRY", 18, white, true)
        tile(content, "Glory Realm Global", "Minister PAUL AYO · Featured pilot ministry") { openLive() }
        space(content, 16)
        text(content, "COMING UP", 18, white, true)
        tile(content, "Upcoming programmes", "See scheduled ministry broadcasts and events") { showUpcoming() }
    }

    private fun showLive() {
        base("Live", "Broadcast network", "LIVE NOW", "Authorised ministry broadcasts in one place.")
        val c = card(content)
        text(c, "GLORY REALM GLOBAL", 12, gold, true)
        space(c, 8)
        text(c, "Minister PAUL AYO", 22, white, true)
        text(c, "Featured live programme", 14, muted, false)
        space(c, 14)
        button(c, "▶  OPEN LIVE BROADCAST") { openLive() }
        space(content, 18)
        tile(content, "More ministry channels", "Participating ministries will appear here when their authorised feeds are connected and verified.") { showMinistries() }
        notice(content, "Live feed discovery requires the ELIX streaming backend and participating ministries' authorised HLS/RTMP/SRT integrations. This screen does not pretend that recorded videos are live.")
    }

    private fun showLibrary() {
        base("Library", "On-demand content", "LIBRARY", "Explore teaching, worship and Christian media.")
        listOf(
            "Sermons" to "Search by preacher, ministry, topic and date",
            "Worship" to "Songs, worship services and praise sessions",
            "Conferences" to "Conference messages and crusade archives",
            "Prayer" to "Prayer sessions and intercession",
            "Interviews" to "Christian conversations and testimonies",
            "Bible Study" to "Bible lessons and teaching series",
            "Podcasts" to "Audio programmes and discussions",
            "Children & Family" to "Christian family resources"
        ).forEach { (a,b) -> tile(content,a,b) { info("The searchable media catalogue will populate after ELIX backend and ministry uploads are connected.") } }
        notice(content, "Offline downloads and searchable archives will be enabled when media storage and secure playback are connected.")
    }

    private fun showMinistries() {
        base("Ministries", "Our broadcast partners", "MINISTRY CHANNELS", "Follow ministries and discover their broadcasts.")
        val ministryList = card(content)
        text(ministryList, "Loading approved ministry channels…", 14, muted, false)
        SupabaseApi.get("ministries?select=name,city&approved=eq.true&order=name.asc") { rows ->
            runOnUiThread {
                ministryList.removeAllViews()
                if (rows.length() == 0) text(ministryList, "No approved ministry channels have been published yet.", 14, muted, false)
                for (i in 0 until rows.length()) {
                    val item = rows.getJSONObject(i)
                    text(ministryList, item.optString("name"), 17, gold, true)
                    text(ministryList, item.optString("city", "Nigeria"), 13, muted, false)
                    space(ministryList, 8)
                }
            }
        }
        tile(content, "Glory Realm Global live", "Minister PAUL AYO · Pilot broadcast") { openLive() }
    }

    private fun showUpcoming() {
        base("Upcoming", "Broadcast calendar", "UPCOMING PROGRAMMES", "Plan ahead and join upcoming ministry broadcasts.")
        tile(content, "Programme calendar", "Scheduled services, conferences, crusades and special broadcasts will appear here.") { info("The programme calendar will populate from the ELIX admin portal after backend connection.") }
    }

    private fun showAccount() {
        base("Account", "Your ELIX experience", "MY ACCOUNT", "Create an account to personalise your Christian media experience.")
        tile(content, "Create account / Sign in", "Google and email authentication will be activated when the ELIX identity backend is configured.") { info("Account registration is not active in this preview build. It requires the ELIX authentication service and Google OAuth configuration.") }
        tile(content, "Followed ministries", "Keep your favourite ministry channels together.") { info("Sign-in and ministry-following services are coming with the backend integration.") }
        tile(content, "Saved library & downloads", "Return to saved teachings and offline content.") { info("Offline downloads will be enabled after secure media storage is connected.") }
        tile(content, "Subscriptions", "Manage premium access and your subscription.") { info("Payments are not activated yet. ELIX will connect a verified payment provider before accepting subscriptions.") }
    }

    private fun openLive() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/live/zYNNWEN9Ujc")))
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open the live programme.", Toast.LENGTH_LONG).show()
        }
    }

    private fun info(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()

    private fun card(parent: LinearLayout): LinearLayout {
        val v = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(17), dp(17), dp(17), dp(17))
            setBackgroundColor(panel)
        }
        val p = fullWidth(); p.bottomMargin = dp(10); parent.addView(v,p)
        return v
    }

    private fun tile(parent: LinearLayout, title: String, detail: String, action: () -> Unit) {
        val v = card(parent)
        text(v, title, 16, gold, true)
        space(v, 5)
        text(v, detail, 14, muted, false)
        v.setOnClickListener { action() }
    }

    private fun notice(parent: LinearLayout, value: String) {
        val v = TextView(this).apply {
            text = value
            textSize = 13f
            setTextColor(muted)
            setPadding(dp(13), dp(13), dp(13), dp(13))
            setBackgroundColor(Color.rgb(23, 29, 46))
        }
        val p=fullWidth(); p.topMargin=dp(10); parent.addView(v,p)
    }

    private fun button(parent: LinearLayout, label: String, action: () -> Unit) {
        val b = Button(this).apply {
            text = label
            textSize = 14f
            isAllCaps = false
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.rgb(25,20,30))
            setBackgroundColor(gold)
            setOnClickListener { action() }
        }
        parent.addView(b, LinearLayout.LayoutParams(-1, dp(52)))
    }

    private fun text(parent: LinearLayout, value: String, size: Int, color: Int, bold: Boolean) {
        val v = TextView(this).apply {
            text=value; textSize=size.toFloat(); setTextColor(color)
            if (bold) setTypeface(Typeface.DEFAULT,Typeface.BOLD)
            gravity=Gravity.START; setLineSpacing(dp(3).toFloat(),1f)
        }
        parent.addView(v,fullWidth())
    }

    private fun space(parent: LinearLayout, h: Int) {
        parent.addView(View(this),LinearLayout.LayoutParams(1,dp(h)))
    }

    private fun fullWidth() = LinearLayout.LayoutParams(-1,-2)
    private fun dp(v: Int) = (v*resources.displayMetrics.density).toInt()
}
