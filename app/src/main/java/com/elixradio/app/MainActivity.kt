package com.elixradio.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.media.MediaPlayer
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import android.widget.FrameLayout
import android.graphics.BitmapFactory
import org.json.JSONObject
import java.net.URL

class MainActivity : Activity() {
    private val bg = Color.rgb(4, 7, 17)
    private val panel = Color.rgb(10, 19, 38)
    private val gold = Color.rgb(35, 132, 255)
    private val signalRed = Color.rgb(238, 32, 48)
    private val white = Color.rgb(250, 248, 255)
    private val muted = Color.rgb(190, 193, 208)
    private lateinit var content: LinearLayout
    private lateinit var nav: LinearLayout
    private val uploadRequestCode = 4107
    private var selectedUploadType = "audio"
    private var selectedPageId: String? = null
    private var libraryType = "all"
    private var mediaPlayer: MediaPlayer? = null
    private val homeFeedItems = ArrayList<JSONObject>()
    private var homeFeedIndex = 0
    private var activeFeedMediaId = ""
    private var feedItemStartedAt = 0L
    private var homeVideo: VideoView? = null
    private var homeImage: ImageView? = null
    private var homeAudioPanel: LinearLayout? = null
    private var homeFeedTitle: TextView? = null
    private var homeFeedCategory: TextView? = null
    private var homeFeedPosition: TextView? = null
    private var homeFeedFrame: FrameLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(3, 7, 17)
        window.navigationBarColor = bg
        window.decorView.setBackgroundColor(bg)

        val shell = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.elix_background)
        }
        val scroll = ScrollView(this).apply { isFillViewport = true; setBackgroundColor(bg) }
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(28))
            setBackgroundResource(R.drawable.elix_background)
        }
        scroll.addView(content)
        shell.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(5), dp(8), dp(5), dp(8))
            setBackgroundColor(Color.rgb(3, 8, 20))
            gravity = Gravity.CENTER
        }
        shell.addView(nav, LinearLayout.LayoutParams(-1, dp(62)))
        setContentView(shell)
        showHome()
    }

    private fun setNav(active: String) {
        nav.removeAllViews()
        listOf("Home", "Discover", "Account", "Broadcasts").forEach { label ->
            val b = TextView(this).apply {
                text = label
                textSize = 10f
                gravity = Gravity.CENTER
                setTypeface(null, if (label == active) Typeface.BOLD else Typeface.NORMAL)
                setTextColor(if (label == active) Color.rgb(48, 145, 255) else muted)
                setPadding(dp(3), dp(8), dp(3), dp(8))
                setOnClickListener {
                    when (label) {
                        "Home" -> showHome()
                        "Discover" -> showDiscover()
                        "Account" -> showAccount()
                        "Broadcasts" -> showBroadcasts()
                    }
                }
            }
            nav.addView(b, LinearLayout.LayoutParams(0, -1, 1f))
        }
    }

    private fun base(active: String, eyebrow: String, title: String, subtitle: String) {
        try { homeVideo?.stopPlayback() } catch (_: Exception) { }
        stopAudio()
        content.removeAllViews()
        content.setPadding(dp(20), dp(24), dp(20), dp(28))
        setNav(active)
        val brand = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val logo = ImageView(this).apply {
            setImageResource(R.drawable.elix_logo)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        brand.addView(logo, LinearLayout.LayoutParams(dp(58), dp(58)))
        val brandText = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        text(brandText, "ELIX RADIO", 19, white, true)
        text(brandText, "CHRISTIAN MEDIA NETWORK", 9, Color.rgb(50, 145, 255), true)
        brand.addView(brandText, LinearLayout.LayoutParams(0, -2, 1f))
        content.addView(brand, fullWidth())
        space(content, 12)
        text(content, eyebrow.uppercase(), 11, signalRed, true)
        space(content, 7)
        text(content, title, 30, white, true)
        text(content, subtitle, 15, muted, false)
        space(content, 20)
    }

    private fun showHome() {
        base("Home", "Discover Christian voices", "MINISTRIES & PAGES", "Find ministries and Christian public figures. Open a page to explore their content and programmes.")
        if (AuthApi.isSignedIn(this)) button(content, "＋  CREATE A PAGE") { createPageDialog() }
        val list = card(content); text(list, "Loading pages…", 13, muted, false)
        SupabaseApi.getWithError(this, "ministries?select=id,name,city,page_type,description&approved=eq.true&order=name.asc&limit=200") { rows, error ->
            runOnUiThread { list.removeAllViews();
                if (error != null) text(list, error, 12, signalRed, false)
                else if (rows.length() == 0) text(list, "No public pages yet. Create an account and submit the first page.", 14, muted, false)
                else for (i in 0 until rows.length()) { val p = rows.getJSONObject(i); tile(list, p.optString("name"), p.optString("page_type").replace("_"," ").uppercase() + " • " + p.optString("city","Nigeria")) { showCreatorPage(p.optString("id")) } }
            }
        }
        text(content, "DISCOVER SOMETHING NEW", 16, white, true)
        tile(content, "Discover videos, audio and images", "Swipe through autoplaying Christian content.") { showDiscover() }
    }

    private fun showBroadcasts() {
        base("Broadcasts", "Listen and watch live", "RADIO & LIVE", "Broadcasts are separate from ministry pages and Discover.")
        tile(content, "ELIX RADIO", "Recorded audio programmes, sermons and music.") { showRadio() }
        tile(content, "LIVE RADIO", "Real-time audio broadcasts only.") { showLiveRadio() }
        tile(content, "LIVE VIDEO", "Authorised live video sessions.") { showLive() }
    }

    private fun showCreatorPage(pageId: String) {
        base("Home", "Ministry & creator channel", "PAGE", "Explore this page’s complete content and programmes.")
        val profile = card(content); text(profile, "Loading page…", 13, muted, false)
        SupabaseApi.getWithError(this, "ministries?select=id,name,description,city,page_type&id=eq.$pageId&limit=1") { pages, error ->
            runOnUiThread { profile.removeAllViews(); if (error != null || pages.length() == 0) text(profile, "Page could not be loaded.", 13, signalRed, false) else {
                val p = pages.getJSONObject(0); text(profile, p.optString("name"), 23, white, true); text(profile, p.optString("page_type").replace("_"," ").uppercase() + " • " + p.optString("city","Nigeria"), 12, gold, true); text(profile, p.optString("description"), 14, muted, false)
                if (AuthApi.isSignedIn(this)) button(profile, "＋ FOLLOW PAGE") { SupabaseApi.postWithError(this, "ministry_follows", JSONObject().put("user_id", AuthApi.currentUserId(this)).put("ministry_id", pageId)) { e -> runOnUiThread { info(if (e == null) "Page followed." else "Could not follow: $e") } } }
            } }
        }
        text(content, "UPCOMING PROGRAMMES & CONFERENCES", 16, white, true); val programmes = card(content)
        SupabaseApi.getWithError(this, "programmes?select=title,description,starts_at&ministry_id=eq.$pageId&status=eq.scheduled&order=starts_at.asc&limit=50") { rows, e -> runOnUiThread { programmes.removeAllViews(); if (e != null || rows.length() == 0) text(programmes, "No upcoming programmes announced.", 13, muted, false) else for (i in 0 until rows.length()) { val x=rows.getJSONObject(i); text(programmes,x.optString("title"),15,gold,true); text(programmes,x.optString("starts_at").replace("T"," "),12,muted,false); text(programmes,x.optString("description"),13,muted,false) } } }
        listOf("BOOKS" to "pdf", "AUDIO MESSAGES" to "audio", "SHORTS & VIDEOS" to "video", "IMAGES" to "image").forEach { (heading, type) ->
            text(content, heading, 16, white, true); val section = card(content)
            SupabaseApi.getWithError(this, "media_items?select=id,title,kind,media_type,file_path&ministry_id=eq.$pageId&status=eq.approved&media_type=eq.$type&order=created_at.desc&limit=40") { rows, e -> runOnUiThread { section.removeAllViews(); if (e != null || rows.length() == 0) text(section, "Nothing published here yet.", 13, muted, false) else for (i in 0 until rows.length()) { val x=rows.getJSONObject(i); val t=x.optString("title"); text(section,t,15,gold,true); text(section,x.optString("kind").replace("_"," ").uppercase(),11,muted,false); button(section,if(type=="audio") "▶ PLAY AUDIO" else "OPEN") { if(type=="audio") playAudio(t,x.optString("file_path")) else openMedia(x.optString("file_path")) } } } }
        }
        text(content, "LIVE MESSAGES", 16, white, true); tile(content, "Live broadcasts", "See authorised live radio and video sessions.") { showBroadcasts() }
        button(content, "BACK TO PAGES") { showHome() }
    }

    private fun createPageDialog() {
        val name=android.widget.EditText(this).apply { hint="Ministry or public figure name"; setTextColor(white) }
        val desc=android.widget.EditText(this).apply { hint="Short description"; setTextColor(white) }
        val city=android.widget.EditText(this).apply { hint="City / location"; setTextColor(white) }
        val type=android.widget.Spinner(this).apply { adapter=android.widget.ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,arrayOf("Ministry","Public figure")) }
        val box=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(16),dp(8),dp(16),0); addView(name); addView(type); addView(desc); addView(city) }
        android.app.AlertDialog.Builder(this).setTitle("Create public page").setView(box).setNegativeButton("Cancel",null).setPositiveButton("Submit",null).create().also { d -> d.setOnShowListener { d.getButton(-1).setOnClickListener { val n=name.text.toString().trim(); if(n.isBlank()){name.error="Enter a name";return@setOnClickListener}; val slug=n.lowercase().replace(Regex("[^a-z0-9]+"),"-").trim('-')+"-"+System.currentTimeMillis().toString().takeLast(5); val body=JSONObject().put("name",n).put("slug",slug).put("description",desc.text.toString()).put("city",city.text.toString()).put("page_type",if(type.selectedItemPosition==1)"public_figure" else "ministry").put("owner_id",AuthApi.currentUserId(this)); SupabaseApi.postWithError(this,"ministries",body){ e -> runOnUiThread { info(if(e==null)"Page submitted for ELIX approval." else "Page creation failed: $e"); if(e==null)showAccount() } }; d.dismiss() } }; d.show() }
    }


    private fun editProfileDialog() {
        val name = android.widget.EditText(this).apply { hint = "Display name"; setText(AuthApi.currentName(this@MainActivity)); setTextColor(white) }
        android.app.AlertDialog.Builder(this).setTitle("Edit account profile").setView(name).setNegativeButton("Cancel", null)
            .setPositiveButton("Save", null).create().also { d ->
                d.setOnShowListener { d.getButton(-1).setOnClickListener {
                    val value = name.text.toString().trim()
                    if (value.isBlank()) { name.error = "Enter your display name"; return@setOnClickListener }
                    SupabaseApi.patchWithError(this, "profiles?id=eq.${AuthApi.currentUserId(this)}", JSONObject().put("display_name", value)) { e ->
                        runOnUiThread { info(if (e == null) "Profile updated. Sign in again to refresh the displayed name." else "Profile update failed: $e"); if (e == null) showAccount() }
                    }
                    d.dismiss()
                } }
                d.show()
            }
    }

    private fun editPageDialog(page: JSONObject) {
        val name = android.widget.EditText(this).apply { hint = "Page name"; setText(page.optString("name")); setTextColor(white) }
        val desc = android.widget.EditText(this).apply { hint = "Description"; setText(page.optString("description")); setTextColor(white) }
        val city = android.widget.EditText(this).apply { hint = "City"; setText(page.optString("city")); setTextColor(white) }
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(8), dp(16), 0); addView(name); addView(desc); addView(city) }
        android.app.AlertDialog.Builder(this).setTitle("Edit page profile").setView(box).setNegativeButton("Cancel", null)
            .setPositiveButton("Save", null).create().also { d ->
                d.setOnShowListener { d.getButton(-1).setOnClickListener {
                    val body = JSONObject().put("name", name.text.toString().trim()).put("description", desc.text.toString().trim()).put("city", city.text.toString().trim())
                    SupabaseApi.patchWithError(this, "ministries?id=eq.${page.optString("id")}", body) { e ->
                        runOnUiThread { info(if (e == null) "Page profile updated." else "Page update failed: $e"); if (e == null) showAccount() }
                    }
                    d.dismiss()
                } }
                d.show()
            }
    }

    private fun editMediaDialog(item: JSONObject) {
        val title=android.widget.EditText(this).apply { setText(item.optString("title")); setTextColor(white) }; val desc=android.widget.EditText(this).apply { hint="Description"; setText(item.optString("description")); setTextColor(white) }
        val box=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(16),dp(8),dp(16),0); addView(title); addView(desc) }
        android.app.AlertDialog.Builder(this).setTitle("Edit your content").setView(box).setNegativeButton("Cancel",null).setPositiveButton("Save",null).create().also { d -> d.setOnShowListener { d.getButton(-1).setOnClickListener { val body=JSONObject().put("title",title.text.toString().trim()).put("description",desc.text.toString().trim()); SupabaseApi.patchWithError(this,"media_items?id=eq.${item.optString("id")}",body){ e -> runOnUiThread { info(if(e==null)"Content updated." else "Update failed: $e"); if(e==null)showAccount() } }; d.dismiss() } }; d.show() }
    }
    private fun showDiscover() {
        content.removeAllViews()
        content.setPadding(0, 0, 0, 0)
        setNav("Discover")
        val height = (resources.displayMetrics.heightPixels - dp(170)).coerceAtLeast(dp(420))
        val frame = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        homeFeedFrame = frame
        val video = VideoView(this)
        homeVideo = video
        frame.addView(video, FrameLayout.LayoutParams(-1, -1))
        val image = ImageView(this).apply { scaleType = ImageView.ScaleType.CENTER_CROP; visibility = View.GONE }
        homeImage = image
        frame.addView(image, FrameLayout.LayoutParams(-1, -1))
        val audioPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(28), dp(28), dp(28), dp(28))
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(Color.rgb(4, 18, 43), Color.rgb(12, 55, 105), Color.rgb(80, 8, 30)))
            visibility = View.GONE
        }
        val audioMark = TextView(this).apply { text = "ELIX\n♪"; gravity = Gravity.CENTER; textSize = 48f; setTextColor(white); setTypeface(null, Typeface.BOLD) }
        audioPanel.addView(audioMark, LinearLayout.LayoutParams(-1, dp(170)))
        homeAudioPanel = audioPanel
        frame.addView(audioPanel, FrameLayout.LayoutParams(-1, -1))
        val overlay = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(45), dp(18), dp(18))
            background = GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, intArrayOf(0xDD020611.toInt(), 0x00000000))
        }
        val brand = TextView(this).apply { text = "ELIX  •  DISCOVER"; textSize = 14f; setTextColor(white); setTypeface(null, Typeface.BOLD) }
        overlay.addView(brand)
        val spacer = View(this)
        overlay.addView(spacer, LinearLayout.LayoutParams(1, 0, 1f))
        val category = TextView(this).apply { textSize = 12f; setTextColor(Color.rgb(65, 160, 255)); setTypeface(null, Typeface.BOLD) }
        homeFeedCategory = category
        overlay.addView(category)
        val title = TextView(this).apply { textSize = 21f; setTextColor(white); setTypeface(null, Typeface.BOLD); setPadding(0, dp(5), 0, dp(8)) }
        homeFeedTitle = title
        overlay.addView(title)
        val position = TextView(this).apply { text = "Loading your feed…"; textSize = 12f; setTextColor(muted) }
        homeFeedPosition = position
        overlay.addView(position)
        val controls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val pause = Button(this).apply { text = "Ⅱ  PAUSE"; isAllCaps = false; setOnClickListener {
            val current = homeFeedItems.getOrNull(homeFeedIndex)
            if (current?.optString("media_type") == "video") { if (homeVideo?.isPlaying == true) { homeVideo?.pause(); text = "▶  PLAY" } else { homeVideo?.start(); text = "Ⅱ  PAUSE" } }
            else if (current?.optString("media_type") == "audio") { if (mediaPlayer?.isPlaying == true) { mediaPlayer?.pause(); text = "▶  PLAY" } else { mediaPlayer?.start(); text = "Ⅱ  PAUSE" } }
        } }
        controls.addView(pause, LinearLayout.LayoutParams(0, dp(48), 1f))
        val like = Button(this).apply { text = "♡  LIKE"; isAllCaps = false; setOnClickListener {
            val item = homeFeedItems.getOrNull(homeFeedIndex)
            if (item != null) { FeedEngagementApi.record(this@MainActivity, item.optString("id"), "like"); text = "♥  LIKED" }
        } }
        controls.addView(like, LinearLayout.LayoutParams(0, dp(48), 1f))
        val share = Button(this).apply { text = "↗"; isAllCaps = false; setOnClickListener { shareCurrentFeedItem() } }
        controls.addView(share, LinearLayout.LayoutParams(0, dp(48), 1f))
        overlay.addView(controls)
        val feedback = Button(this).apply { text = "⋯  NOT INTERESTED"; isAllCaps = false; setOnClickListener {
            val item = homeFeedItems.getOrNull(homeFeedIndex)
            if (item != null) FeedEngagementApi.record(this@MainActivity, item.optString("id"), "not_interested")
            moveFeed(1)
        } }
        overlay.addView(feedback)
        frame.addView(overlay, FrameLayout.LayoutParams(-1, -1))
        val touch = View.OnTouchListener { _, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> { feedTouchY = event.y; true }
                android.view.MotionEvent.ACTION_UP -> {
                    val delta = event.y - feedTouchY
                    if (kotlin.math.abs(delta) > dp(55)) { if (delta < 0) moveFeed(1) else moveFeed(-1) }
                    true
                }
                else -> true
            }
        }
        feedTouchY = 0f
        frame.setOnTouchListener(touch)
        video.setOnTouchListener(touch)
        image.setOnTouchListener(touch)
        audioPanel.setOnTouchListener(touch)
        content.addView(frame, LinearLayout.LayoutParams(-1, height))
        val hint = TextView(this).apply { text = "SWIPE UP FOR NEXT  ↑"; textSize = 10f; gravity = Gravity.CENTER; setTextColor(muted); setPadding(0, dp(8), 0, dp(8)) }
        content.addView(hint, fullWidth())
        val feedLoading = TextView(this).apply { text = "Loading approved ELIX content…"; textSize = 12f; setTextColor(muted); gravity = Gravity.CENTER }
        content.addView(feedLoading, fullWidth())
        SupabaseApi.getWithError(this, "media_items?select=id,title,kind,media_type,file_path,duration_seconds&status=eq.approved&order=created_at.desc&limit=200") { rows, error ->
            runOnUiThread {
                if (isFinishing || homeFeedFrame !== frame) return@runOnUiThread
                content.removeView(feedLoading)
                homeFeedItems.clear()
                if (error != null) { homeFeedTitle?.text = "Could not load ELIX feed"; homeFeedCategory?.text = error; homeFeedPosition?.text = "Please try again later"; return@runOnUiThread }
                for (i in 0 until rows.length()) homeFeedItems.add(rows.getJSONObject(i))
                if (homeFeedItems.isEmpty()) { homeFeedTitle?.text = "Your ELIX feed is waiting"; homeFeedCategory?.text = "NO PUBLISHED CONTENT YET"; homeFeedPosition?.text = "Approved sermons, worship, videos and audio will appear here."; return@runOnUiThread }
                homeFeedIndex = 0
                FeedEngagementApi.loadSignals(this@MainActivity) { signals ->
                    runOnUiThread {
                        if (isFinishing || homeFeedFrame !== frame) return@runOnUiThread
                        rankFeedForDiscovery(signals)
                        displayHomeFeedItem()
                    }
                }
            }
        }
    }

    private var feedTouchY = 0f

    // Personal relevance is balanced with category variety so the FYP does not
    // become a wall of one content type after a few strong engagement signals.
    private fun rankFeedForDiscovery(signals: FeedEngagementApi.Signals) {
        val remaining = homeFeedItems.withIndex().toMutableList()
        val ranked = ArrayList<JSONObject>(remaining.size)
        val categoryCounts = mutableMapOf<String, Int>()
        var previousCategory = ""
        var sameCategoryRun = 0

        while (remaining.isNotEmpty()) {
            var bestIndex = 0
            var bestScore = Double.NEGATIVE_INFINITY
            for (i in remaining.indices) {
                val item = remaining[i].value
                val category = item.optString("kind", "other").ifBlank { "other" }
                val baseScore = (signals.itemScores[item.optString("id")] ?: 0.0) +
                    (signals.categoryScores[category] ?: 0.0) * 0.35
                val varietyBonus = 4.0 / (1.0 + (categoryCounts[category] ?: 0))
                val repeatPenalty = if (category == previousCategory && sameCategoryRun >= 2) 1000.0 else 0.0
                val score = baseScore + varietyBonus - repeatPenalty - (i * 0.0001)
                if (score > bestScore) {
                    bestScore = score
                    bestIndex = i
                }
            }
            val chosen = remaining.removeAt(bestIndex).value
            val chosenCategory = chosen.optString("kind", "other").ifBlank { "other" }
            if (chosenCategory == previousCategory) sameCategoryRun++ else {
                previousCategory = chosenCategory
                sameCategoryRun = 1
            }
            categoryCounts[chosenCategory] = (categoryCounts[chosenCategory] ?: 0) + 1
            ranked.add(chosen)
        }
        homeFeedItems.clear()
        homeFeedItems.addAll(ranked)
    }

    private fun moveFeed(direction: Int) {
        if (homeFeedItems.isEmpty()) return
        val next = (homeFeedIndex + direction).coerceIn(0, homeFeedItems.lastIndex)
        if (next != homeFeedIndex) { homeFeedIndex = next; displayHomeFeedItem() }
    }

    private fun displayHomeFeedItem() {
        val item = homeFeedItems.getOrNull(homeFeedIndex) ?: return
        if (activeFeedMediaId.isNotBlank()) {
            val watched = ((System.currentTimeMillis() - feedItemStartedAt) / 1000L).toInt().coerceAtLeast(0)
            FeedEngagementApi.record(this, activeFeedMediaId, if (watched <= 3) "skip" else "watch", watched)
        }
        activeFeedMediaId = item.optString("id")
        feedItemStartedAt = System.currentTimeMillis()
        stopAudio()
        try { homeVideo?.stopPlayback() } catch (_: Exception) { }
        homeVideo?.visibility = View.GONE
        homeImage?.visibility = View.GONE
        homeAudioPanel?.visibility = View.GONE
        val title = item.optString("title", "ELIX media")
        val category = item.optString("kind", "other").replace('_', ' ').uppercase()
        val type = item.optString("media_type", "video")
        val path = item.optString("file_path")
        homeFeedTitle?.text = title
        homeFeedCategory?.text = category + "  •  " + type.uppercase()
        homeFeedPosition?.text = (homeFeedIndex + 1).toString() + " / " + homeFeedItems.size + "   •   ELIX CHRISTIAN MEDIA NETWORK"
        if (path.isBlank()) return
        val url = BuildConfig.SUPABASE_URL.trimEnd('/') + "/storage/v1/object/public/elix-media/" + Uri.encode(path, "/")
        when (type) {
            "video" -> {
                val player = homeVideo ?: return
                player.visibility = View.VISIBLE
                player.setVideoURI(Uri.parse(url))
                player.setOnPreparedListener { mp -> mp.isLooping = true; mp.start() }
                player.setOnErrorListener { _, _, _ -> info("This video could not be played."); true }
            }
            "audio" -> { homeAudioPanel?.visibility = View.VISIBLE; playAudio(title, path) }
            "image" -> {
                val target = homeImage ?: return
                target.visibility = View.VISIBLE
                Thread {
                    try {
                        val bitmap = URL(url).openStream().use { BitmapFactory.decodeStream(it) }
                        runOnUiThread { if (homeFeedItems.getOrNull(homeFeedIndex) === item && bitmap != null) target.setImageBitmap(bitmap) }
                    } catch (_: Exception) { runOnUiThread { info("Could not load this image.") } }
                }.start()
            }
            else -> { homeAudioPanel?.visibility = View.VISIBLE }
        }
    }

    private fun shareCurrentFeedItem() {
        val item = homeFeedItems.getOrNull(homeFeedIndex) ?: return
        FeedEngagementApi.record(this, item.optString("id"), "share")
        val path = item.optString("file_path")
        val url = BuildConfig.SUPABASE_URL.trimEnd('/') + "/storage/v1/object/public/elix-media/" + Uri.encode(path, "/")
        val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, item.optString("title") + "\n" + url) }
        startActivity(Intent.createChooser(send, "Share ELIX content"))
    }
    private fun showLive() {
        base("Live Video", "Video broadcast network", "LIVE VIDEO", "Watch authorised live video broadcasts.")
        val c = card(content)
        text(c, "GLORY REALM GLOBAL", 12, gold, true)
        space(c, 8)
        text(c, "Minister PAUL AYO", 22, white, true)
        text(c, "Featured live programme", 14, muted, false)
        space(c, 14)
        button(c, "▶  WATCH LIVE VIDEO") { openLive() }
        space(content, 12)
        tile(content, "Live Radio", "Listen to a separate live audio broadcast.") { showLiveRadio() }
        space(content, 18)
        tile(content, "More ministry channels", "Participating ministries will appear here when their authorised feeds are connected and verified.") { showMinistries() }
        notice(content, "Live feed discovery requires the ELIX streaming backend and participating ministries' authorised HLS/RTMP/SRT integrations. This screen does not pretend that recorded videos are live.")
    }

    private fun showLibrary() {
        base("Library", "On-demand content", "ELIX MEDIA LIBRARY", "Audio, video and images are separated and grouped by category.")
        if (AuthApi.isSignedIn(this)) button(content, "＋  UPLOAD MEDIA") { chooseUploadFile() }
        else notice(content, "Sign in to upload audio, video or image files.")
        val filters = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("all" to "ALL", "audio" to "AUDIO", "video" to "VIDEO", "image" to "IMAGES", "pdf" to "BOOKS").forEach { pair ->
            val b = Button(this).apply { text = pair.second; textSize = 10f; isAllCaps = false; setOnClickListener { libraryType = pair.first; showLibrary() } }
            filters.addView(b, LinearLayout.LayoutParams(0, dp(44), 1f))
        }
        content.addView(filters, fullWidth())
        space(content, 12)
        val list = card(content)
        text(list, "Loading catalogue…", 13, muted, false)
        val filter = if (libraryType == "all") "" else "&media_type=eq.$libraryType"
        SupabaseApi.getWithError(this, "media_items?select=title,kind,media_type,file_path&status=eq.approved$filter&order=kind.asc,created_at.desc&limit=100") { rows, error ->
            runOnUiThread {
                list.removeAllViews()
                if (error != null) text(list, "Catalogue error: $error", 12, signalRed, false)
                else if (rows.length() == 0) text(list, "No published media in this section yet.", 14, muted, false)
                else {
                    var last = ""
                    for (i in 0 until rows.length()) {
                        val item = rows.getJSONObject(i)
                        val kind = item.optString("kind", "other")
                        val category = kind.replace('_', ' ').uppercase()
                        if (category != last) { text(list, category, 12, signalRed, true); last = category }
                        val mediaType = item.optString("media_type", "media")
                        val path = item.optString("file_path")
                        text(list, item.optString("title", "ELIX media"), 16, gold, true)
                        text(list, mediaType.uppercase(), 11, muted, false)
                        button(list, if (mediaType == "audio") "▶  PLAY AUDIO" else "OPEN FILE") {
                            if (mediaType == "audio") playAudio(item.optString("title", "ELIX Radio"), path) else openMedia(path)
                        }
                        space(list, 12)
                    }
                }
            }
        }
        notice(content, "Every upload is listed under its selected media type and content category.")
    }

    private fun showRadio() {
        base("Radio", "ELIX audio station", "ELIX RADIO", "Listen to recorded audio programmes and messages.")
        val player = card(content)
        text(player, "AUDIO PLAYER", 12, signalRed, true)
        text(player, "Choose a programme below to listen.", 14, muted, false)
        button(player, "■  STOP") { stopAudio() }
        space(content, 12)
        text(content, "RECENT AUDIO", 17, white, true)
        val list = card(content)
        SupabaseApi.getWithError(this, "media_items?select=title,kind,file_path&status=eq.approved&media_type=eq.audio&order=created_at.desc&limit=100") { rows, error ->
            runOnUiThread {
                list.removeAllViews()
                if (error != null) text(list, "Could not load audio: $error", 12, signalRed, false)
                else if (rows.length() == 0) text(list, "No audio uploaded yet. Use Library → Upload Media.", 14, muted, false)
                else for (i in 0 until rows.length()) {
                    val item = rows.getJSONObject(i)
                    text(list, item.optString("title", "ELIX audio"), 15, gold, true)
                    text(list, item.optString("kind", "other").replace('_', ' ').uppercase(), 11, muted, false)
                    button(list, "▶  PLAY") { playAudio(item.optString("title", "ELIX Radio"), item.optString("file_path")) }
                }
            }
        }
    }

    private fun showLiveRadio() {
        base("Live Radio", "Live audio broadcast", "LIVE RADIO", "A real-time audio session, separate from recorded audio and live video.")
        val panel = card(content)
        text(panel, "●  CHECKING LIVE AUDIO", 13, signalRed, true)
        SupabaseApi.getWithError(this, "live_streams?select=title,playback_url,protocol,state&stream_type=eq.audio&state=eq.live&is_authorised=eq.true") { rows, error ->
            runOnUiThread {
                panel.removeAllViews()
                if (error != null) {
                    text(panel, "Live radio setup is not available yet: $error", 12, muted, false)
                } else if (rows.length() == 0) {
                    text(panel, "No live radio session is on air right now.", 15, white, true)
                    text(panel, "An ELIX administrator must add and authorise an audio stream before listeners can tune in.", 13, muted, false)
                } else {
                    for (i in 0 until rows.length()) {
                        val stream = rows.getJSONObject(i)
                        text(panel, stream.optString("title", "ELIX Live Radio"), 17, gold, true)
                        button(panel, "▶  LISTEN LIVE") { playStream(stream.optString("playback_url"), stream.optString("title", "ELIX Live Radio")) }
                    }
                }
            }
        }
        notice(content, "Live Radio is for real-time audio. Recorded audio is under ELIX Radio; live video is under Live Video.")
    }

    private fun playAudio(title: String, path: String) {
        val url = BuildConfig.SUPABASE_URL.trimEnd('/') + "/storage/v1/object/public/elix-media/" + Uri.encode(path, "/")
        if (path.isBlank()) { info("This audio has no file path."); return }
        stopAudio()
        val player = MediaPlayer()
        mediaPlayer = player
        try {
            player.setDataSource(url)
            player.setOnPreparedListener { it.start(); info("Now playing: $title") }
            player.setOnErrorListener { _, _, _ -> info("Audio playback failed."); stopAudio(); true }
            player.prepareAsync()
        } catch (e: Exception) { info("Audio playback failed: " + (e.message ?: "unknown error")); stopAudio() }
    }

    private fun playStream(url: String, title: String) {
        if (url.isBlank()) { info("This live station has no playback URL."); return }
        stopAudio()
        val player = MediaPlayer()
        mediaPlayer = player
        try {
            player.setDataSource(url)
            player.setOnPreparedListener { it.start(); info("Listening live: " + title) }
            player.setOnErrorListener { _, _, _ -> info("Live radio playback failed."); stopAudio(); true }
            player.prepareAsync()
        } catch (e: Exception) { info("Live radio playback failed: " + (e.message ?: "unknown error")); stopAudio() }
    }

    private fun stopAudio() {
        try { mediaPlayer?.stop() } catch (_: Exception) { }
        try { mediaPlayer?.release() } catch (_: Exception) { }
        mediaPlayer = null
    }

    private fun openMedia(path: String) {
        val url = BuildConfig.SUPABASE_URL.trimEnd('/') + "/storage/v1/object/public/elix-media/" + Uri.encode(path, "/")
        try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        catch (_: Exception) { info("Could not open this file.") }
    }

    private fun chooseUploadFile() {
        if(!AuthApi.isSignedIn(this)){showAccount();return}
        SupabaseApi.getWithError(this,"ministries?select=id,name&owner_id=eq.${AuthApi.currentUserId(this)}&order=name.asc"){ rows,e -> runOnUiThread {
            val labels=mutableListOf("Personal account"); val ids=mutableListOf<String?>(null); for(i in 0 until rows.length()){val p=rows.getJSONObject(i);labels.add(p.optString("name"));ids.add(p.optString("id"))}
            android.app.AlertDialog.Builder(this).setTitle("Publish under which account?").setItems(labels.toTypedArray()){_,idx -> selectedPageId=ids[idx]; android.app.AlertDialog.Builder(this).setTitle("Choose media type").setItems(arrayOf("Audio","Video","Images","Book (PDF)")){_,which -> selectedUploadType=arrayOf("audio","video","image","pdf")[which]; val type=if(selectedUploadType=="audio")"audio/*" else if(selectedUploadType=="video")"video/*" else if(selectedUploadType=="pdf")"application/pdf" else "image/*"; val intent=Intent(Intent.ACTION_OPEN_DOCUMENT).apply{addCategory(Intent.CATEGORY_OPENABLE);this.type=type};startActivityForResult(intent,uploadRequestCode)}.show()}.show()
        } }
    }
    @Deprecated("Use platform activity result APIs")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == uploadRequestCode && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            val titleInput = android.widget.EditText(this).apply {
                hint = "Give your upload a title"
                setTextColor(white)
                setHintTextColor(muted)
                setPadding(dp(16), dp(12), dp(16), dp(12))
            }
            val dialog = android.app.AlertDialog.Builder(this)
                .setTitle("Publish to ELIX Radio")
                .setMessage("Selected type: " + selectedUploadType.uppercase() + ". Add a title, then choose its category.")
                .setView(titleInput)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("UPLOAD", null)
                .create()
            dialog.setOnShowListener {
                dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    val title = titleInput.text.toString().trim()
                    if (title.isBlank()) {
                        titleInput.error = "Enter a title"
                        return@setOnClickListener
                    }
                    dialog.dismiss()
                    val categoryLabels = arrayOf("Sermon", "Worship", "Conference", "Prayer", "Interview", "Bible Study", "Children & Family", "Podcast", "Other")
                    val categoryKeys = arrayOf("sermon", "worship", "conference", "prayer", "interview", "bible_study", "children_family", "podcast", "other")
                    android.app.AlertDialog.Builder(this).setTitle("Choose content category")
                        .setItems(categoryLabels) { _, index ->
                            info("Uploading file…")
                            UploadApi.upload(this, uri, title, selectedUploadType, categoryKeys[index], selectedPageId) { result ->
                        runOnUiThread {
                            if (result.ok) {
                                info(result.message)
                                showAccount()
                            } else {
                                android.app.AlertDialog.Builder(this)
                                    .setTitle("ELIX upload could not be published")
                                    .setMessage(result.message)
                                    .setPositiveButton("OK", null)
                                    .show()
                            }
                        }
                            }
                        }
                        .show()
                }
            }
            dialog.show()
        }
    }

    private fun showMinistries() = showHome()

    private fun showUpcoming() {
        base("Upcoming", "Broadcast calendar", "UPCOMING PROGRAMMES", "Plan ahead and join upcoming ministry broadcasts.")
        tile(content, "Programme calendar", "Scheduled services, conferences, crusades and special broadcasts will appear here.") { info("The programme calendar will populate from the ELIX admin portal after backend connection.") }
    }

    private fun showAccount() {
        base("Account","Your ELIX identity","MY ACCOUNT","Your profile, pages, uploads and creator tools are managed here.")
        if (!AuthApi.isSignedIn(this)) { button(content,"CREATE ACCOUNT"){showAuthDialog(true)}; button(content,"SIGN IN"){showAuthDialog(false)}; notice(content,"Create an account to publish under your profile or manage a ministry/public-figure page."); return }
        text(content,AuthApi.currentName(this).ifBlank{"ELIX member"},21,white,true); text(content,"Account role: "+AuthApi.currentRole(this).replace("_"," ").uppercase(),12,muted,true)
        button(content,"⚙ ACCOUNT SETTINGS / EDIT PROFILE"){ editProfileDialog() }
        button(content,"＋ CREATE MINISTRY / PUBLIC FIGURE PAGE"){createPageDialog()}; button(content,"＋ UPLOAD CONTENT"){chooseUploadFile()}
        text(content,"MY PAGES",16,white,true); val pages=card(content)
        SupabaseApi.getWithError(this,"ministries?select=id,name,description,city,page_type,approved&owner_id=eq.${AuthApi.currentUserId(this)}&order=created_at.desc"){ rows,e -> runOnUiThread { pages.removeAllViews(); if(e!=null||rows.length()==0)text(pages,"You have not created a page yet.",13,muted,false) else for(i in 0 until rows.length()){val p=rows.getJSONObject(i); tile(pages,p.optString("name"),p.optString("page_type").uppercase()+" • "+if(p.optBoolean("approved"))"PUBLIC" else "AWAITING APPROVAL"){showCreatorPage(p.optString("id"))}; button(pages,"EDIT PAGE PROFILE"){editPageDialog(p)}} } }
        text(content,"MY UPLOADS & CONTENT MANAGEMENT",16,white,true); val uploads=card(content)
        SupabaseApi.getWithError(this,"media_items?select=id,title,description,kind,media_type,status&uploaded_by=eq.${AuthApi.currentUserId(this)}&order=created_at.desc&limit=100"){ rows,e -> runOnUiThread { uploads.removeAllViews(); if(e!=null)text(uploads,"Could not load uploads: $e",12,signalRed,false) else if(rows.length()==0)text(uploads,"Your uploaded files will appear here under this account.",13,muted,false) else for(i in 0 until rows.length()){val item=rows.getJSONObject(i); text(uploads,item.optString("title"),15,gold,true); text(uploads,item.optString("media_type").uppercase()+" • "+item.optString("kind").uppercase()+" • "+item.optString("status").uppercase(),11,muted,false); button(uploads,"EDIT CONTENT"){editMediaDialog(item)}} } }
        if(AuthApi.currentRole(this)=="super_admin")button(content,"⚙ OPEN DEVELOPER DASHBOARD"){showDeveloperDashboard()}
        button(content,"SIGN OUT"){AuthApi.signOut(this);showAccount()}
    }
    private fun showAuthDialog(create: Boolean) {
        val fields = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
        }
        val nameInput = android.widget.EditText(this).apply {
            hint = "Full name"
            setTextColor(white)
            setHintTextColor(muted)
            setSingleLine(true)
        }
        if (create) fields.addView(nameInput)
        val emailInput = android.widget.EditText(this).apply {
            hint = "Email address"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            setTextColor(white)
            setHintTextColor(muted)
            setSingleLine(true)
        }
        fields.addView(emailInput)
        val passwordInput = android.widget.EditText(this).apply {
            hint = "Password (minimum 6 characters)"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            setTextColor(white)
            setHintTextColor(muted)
            setSingleLine(true)
        }
        fields.addView(passwordInput)
        val policyCheck = android.widget.CheckBox(this).apply {
            text = "I agree to the ELIX Terms and Christian Content Policy"
            setTextColor(white)
            buttonTintList = android.content.res.ColorStateList.valueOf(gold)
            textSize = 13f
        }
        if (create) {
            fields.addView(policyCheck)
            val policyText = TextView(this).apply {
                text = "ELIX is a Christian media platform. Uploads must be Christian content and must comply with our purpose. Non-Christian, inappropriate or unrelated content may be removed by ELIX administrators without prior notice. Repeated violations may lead to account suspension or termination. You confirm that you own or have permission to share anything you upload."
                textSize = 12f
                setTextColor(muted)
                setPadding(0, dp(6), 0, dp(8))
            }
            fields.addView(policyText)
        }
        android.app.AlertDialog.Builder(this)
            .setTitle(if (create) "Create ELIX account" else "Sign in to ELIX")
            .setView(fields)
            .setNegativeButton("Cancel", null)
            .setPositiveButton(if (create) "Create account" else "Sign in") { _, _ ->
                val name = nameInput.text.toString().trim()
                val email = emailInput.text.toString().trim()
                val password = passwordInput.text.toString()
                if (email.isBlank() || password.length < 6 || (create && name.isBlank()) || (create && !policyCheck.isChecked)) {
                    info(if (create && !policyCheck.isChecked) "Please accept the ELIX Christian Content Policy to register." else "Enter the required details. Password must contain at least 6 characters.")
                } else {
                    val callback: (AuthApi.Result) -> Unit = { result ->
                        runOnUiThread {
                            info(result.message)
                            if (result.ok && AuthApi.isSignedIn(this)) showAccount()
                        }
                    }
                    if (create) AuthApi.signUp(this, name, email, password, callback)
                    else AuthApi.signIn(this, email, password, callback)
                }
            }.show()
    }

    private fun showDeveloperDashboard() {
        if (!AuthApi.isSignedIn(this)) {
            showAccount()
            return
        }
        base("Account", "Restricted developer access", "ELIX CONTROL ROOM", "Owner dashboard · Full platform oversight")
        if (AuthApi.currentRole(this) != "super_admin") {
            notice(content, "Access denied. This account does not have the ELIX super administrator role.")
            button(content, "BACK TO ACCOUNT") { showAccount() }
            return
        }
        val stats = card(content)
        text(stats, "PLATFORM OVERVIEW", 13, gold, true)
        text(stats, "Loading account and platform statistics…", 13, muted, false)
        AdminApi.get(this, "profiles?select=id&limit=1000") { users, userError ->
            AdminApi.get(this, "media_items?select=id&limit=1000") { media, mediaError ->
                AdminApi.get(this, "ministries?select=id&limit=1000") { ministries, ministryError ->
                    AdminApi.get(this, "programmes?select=id&limit=1000") { programmes, programmeError ->
                        runOnUiThread {
                            stats.removeAllViews()
                            text(stats, "PLATFORM OVERVIEW", 13, gold, true)
                            text(stats, "Registered accounts: ${users.length()}", 15, white, true)
                            text(stats, "Media records: ${media.length()}", 15, white, true)
                            text(stats, "Ministry channels: ${ministries.length()}", 15, white, true)
                            text(stats, "Programmes: ${programmes.length()}", 15, white, true)
                            val errors = listOfNotNull(userError, mediaError, ministryError, programmeError)
                            if (errors.isNotEmpty()) text(stats, "Some statistics could not load: " + errors.first(), 11, signalRed, false)
                        }
                    }
                }
            }
        }
        space(content, 12)
        text(content, "MANAGE ELIX", 16, white, true)
        tile(content, "User accounts", "Review registered members and account roles") {
            showAdminList("USER ACCOUNTS", "profiles?select=display_name,role,created_at&order=created_at.desc&limit=100", "display_name", "role")
        }
        tile(content, "Media catalogue", "Review published and uploaded content") {
            showAdminList("MEDIA CATALOGUE", "media_items?select=title,media_type,status,created_at&order=created_at.desc&limit=100", "title", "status")
        }
        tile(content, "Ministry channels", "Review ministry records and approval status") {
            showAdminList("MINISTRY CHANNELS", "ministries?select=name,city,approved&order=name.asc&limit=100", "name", "city")
        }
        tile(content, "Programmes", "Review scheduled and completed programmes") {
            showAdminList("PROGRAMMES", "programmes?select=title,status,starts_at&order=starts_at.desc&limit=100", "title", "status")
        }
        notice(content, "Developer access is verified against the signed-in Supabase account role. Never share your password or private service-role key.")
        button(content, "BACK TO MY ACCOUNT") { showAccount() }
    }

    private fun showAdminList(title: String, query: String, primary: String, secondary: String) {
        base("Account", "Developer dashboard", title, "Restricted ELIX platform records")
        val list = card(content)
        text(list, "Loading records…", 13, muted, false)
        AdminApi.get(this, query) { rows, error ->
            runOnUiThread {
                list.removeAllViews()
                if (error != null) {
                    text(list, error, 13, signalRed, false)
                } else if (rows.length() == 0) {
                    text(list, "No records found.", 13, muted, false)
                } else {
                    for (i in 0 until rows.length()) {
                        val item = rows.getJSONObject(i)
                        text(list, item.optString(primary).ifBlank { "Untitled record" }, 15, gold, true)
                        val detail = item.optString(secondary)
                        if (detail.isNotBlank()) text(list, detail, 12, muted, false)
                        space(list, 8)
                    }
                }
            }
        }
        button(content, "BACK TO DEVELOPER DASHBOARD") { showDeveloperDashboard() }
    }

    private fun openLive() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/live/zYNNWEN9Ujc")))
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open the live programme.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() { try { homeVideo?.stopPlayback() } catch (_: Exception) { }; stopAudio(); super.onDestroy() }

    private fun info(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()

    private fun card(parent: LinearLayout): LinearLayout {
        val v = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(17), dp(17), dp(17), dp(17))
            background = GradientDrawable().apply {
                setColor(panel)
                cornerRadius = dp(18).toFloat()
                setStroke(dp(1), Color.rgb(18, 65, 126))
            }
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
            background = GradientDrawable().apply {
                setColor(Color.rgb(8, 22, 43))
                cornerRadius = dp(14).toFloat()
                setStroke(dp(1), Color.rgb(22, 86, 160))
            }
        }
        val p=fullWidth(); p.topMargin=dp(10); parent.addView(v,p)
    }

    private fun button(parent: LinearLayout, label: String, action: () -> Unit) {
        val b = Button(this).apply {
            text = label
            textSize = 14f
            isAllCaps = false
            setTypeface(null, Typeface.BOLD)
            setTextColor(white)
            background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(signalRed, Color.rgb(170, 12, 35))).apply {
                cornerRadius = dp(14).toFloat()
                setStroke(dp(1), Color.rgb(255, 70, 83))
            }
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
