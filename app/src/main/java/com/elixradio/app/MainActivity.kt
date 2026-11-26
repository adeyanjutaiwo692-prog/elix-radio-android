package com.elixradio.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.media.MediaPlayer
import android.media.MediaMetadataRetriever
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
import java.io.ByteArrayOutputStream

class MainActivity : Activity() {
    private val bg = Color.rgb(250, 250, 250)
    private val panel = Color.rgb(255, 255, 255)
    private val gold = Color.rgb(0, 149, 246)
    private val signalRed = Color.rgb(0, 149, 246)
    private val white = Color.rgb(20, 20, 20)
    private val muted = Color.rgb(110, 110, 110)
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
        window.statusBarColor = Color.rgb(250, 250, 250)
        window.navigationBarColor = Color.WHITE
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
            setBackgroundColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        shell.addView(nav, LinearLayout.LayoutParams(-1, dp(62)))
        setContentView(shell)
        if (AuthApi.isSignedIn(this)) showHome() else showWelcome()
    }

    private fun showWelcome() {
        try { homeVideo?.stopPlayback() } catch (_: Exception) { }; stopAudio()
        nav.visibility = View.GONE
        content.removeAllViews()
        content.setPadding(dp(28), dp(42), dp(28), dp(28))
        val mark = TextView(this).apply { text = "elix"; textSize = 48f; setTextColor(white); setTypeface(Typeface.create("sans-serif", Typeface.BOLD)); gravity = Gravity.CENTER; letterSpacing = -0.04f }
        content.addView(mark, LinearLayout.LayoutParams(-1, dp(90)))
        text(content, "CHRISTIAN MEDIA NETWORK", 11, muted, true)
        space(content, 8)
        val tagline = TextView(this).apply { text = "Faith. Truth. Inspiration."; textSize = 16f; setTextColor(muted); gravity = Gravity.CENTER }
        content.addView(tagline, fullWidth())
        space(content, 34)
        text(content, "Sign in to ELIX", 28, white, true)
        text(content, "Connect with ministries, creators and Christian media.", 14, muted, false)
        space(content, 22)
        val email = android.widget.EditText(this).apply { hint = "Email address"; inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS; setTextColor(white); setHintTextColor(muted); setSingleLine(true); setPadding(dp(16), dp(14), dp(16), dp(14)); background = GradientDrawable().apply { setColor(Color.WHITE); cornerRadius = dp(10).toFloat(); setStroke(dp(1), Color.rgb(220,220,220)) } }
        val password = android.widget.EditText(this).apply { hint = "Password"; inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD; setTextColor(white); setHintTextColor(muted); setSingleLine(true); setPadding(dp(16), dp(14), dp(16), dp(14)); background = GradientDrawable().apply { setColor(Color.WHITE); cornerRadius = dp(10).toFloat(); setStroke(dp(1), Color.rgb(220,220,220)) } }
        content.addView(email, fullWidth()); space(content, 10); content.addView(password, fullWidth()); space(content, 16)
        button(content, "Log in") { val e=email.text.toString().trim(); val p=password.text.toString(); if(e.isBlank()||p.length<6) info("Enter your email and password.") else AuthApi.signIn(this,e,p) { result -> runOnUiThread { info(result.message); if(result.ok && AuthApi.isSignedIn(this)) { nav.visibility=View.VISIBLE; showHome() } } } }
        space(content, 12)
        val create = TextView(this).apply { text = "Create new account"; textSize = 15f; gravity = Gravity.CENTER; setTextColor(Color.rgb(0,149,246)); setPadding(0, dp(14), 0, dp(14)); setOnClickListener { showAuthDialog(true) } }
        content.addView(create, fullWidth())
        space(content, 20)
        text(content, "By continuing, you agree to the ELIX Terms and Christian Content Policy.", 11, muted, false)
    }

    private fun setNav(active: String) {
        nav.removeAllViews()
        listOf("Home", "Discover", "Explore", "Account").forEach { label ->
            val symbol = when(label) { "Home" -> "⌂"; "Discover" -> "▶"; "Explore" -> "◎"; else -> "◉" }
            val b = TextView(this).apply {
                text = "$symbol\n$label"
                textSize = 11f
                gravity = Gravity.CENTER
                setTypeface(null, if (label == active) Typeface.BOLD else Typeface.NORMAL)
                setTextColor(if (label == active) Color.rgb(48, 145, 255) else muted)
                setPadding(dp(2), dp(4), dp(2), dp(4))
                setOnClickListener {
                    when (label) {
                        "Home" -> showHome()
                        "Discover" -> showDiscover()
                        "Explore" -> showExploreHub()
                        "Account" -> showAccount()
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
        text(brandText, "ELIX", 22, white, true)
        text(brandText, "CHRISTIAN MEDIA NETWORK", 9, muted, true)
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
        base("Home", "THE ELIX COMMUNITY", "Find your people.", "Discover ministries, churches, Christian voices and organisations in one place.")

        val topActions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val countLabel = TextView(this).apply { text = "MINISTRY & CREATOR DIRECTORY"; textSize = 11f; letterSpacing = 0.08f; setTextColor(muted); typeface = Typeface.create("sans-serif-medium", Typeface.BOLD) }
        topActions.addView(countLabel, LinearLayout.LayoutParams(0, dp(42), 1f))
        if (AuthApi.isSignedIn(this)) {
            val create = TextView(this).apply {
                text = "＋ Create page"; textSize = 13f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
                setPadding(dp(14), dp(10), dp(14), dp(10))
                background = GradientDrawable().apply { setColor(Color.rgb(0,149,246)); cornerRadius = dp(22).toFloat() }
                setOnClickListener { createPageDialog() }
            }
            topActions.addView(create)
        }
        content.addView(topActions, fullWidth())

        val search = android.widget.EditText(this).apply {
            hint = "Search ministries, churches, preachers…"
            textSize = 14f; setSingleLine(true); setTextColor(white); setHintTextColor(muted)
            setPadding(dp(16), 0, dp(16), 0)
            background = GradientDrawable().apply { setColor(panel); cornerRadius = dp(15).toFloat(); setStroke(dp(1), Color.rgb(225,225,225)) }
            compoundDrawablePadding = dp(8)
        }
        val searchParams = fullWidth().apply { height = dp(52); topMargin = dp(10); bottomMargin = dp(12) }
        content.addView(search, searchParams)

        val filters = listOf("All", "Ministries", "Churches", "Organisations", "Preachers", "Authors", "Artists")
        var selectedFilter = "All"
        val chipScroll = android.widget.HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; clipToPadding = false }
        val chipRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        chipScroll.addView(chipRow)
        content.addView(chipScroll, fullWidth())

        text(content, "DISCOVER CHRISTIAN COMMUNITIES", 15, white, true)
        space(content, 5)
        text(content, "Follow a page to keep its messages, videos and programmes close.", 12, muted, false)
        space(content, 12)
        val resultsTitle = TextView(this).apply { text = "ALL PAGES"; textSize = 11f; letterSpacing = 0.08f; setTextColor(gold); typeface = Typeface.DEFAULT_BOLD }
        content.addView(resultsTitle, fullWidth())
        space(content, 8)
        val directory = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(directory, fullWidth())
        val allPages = mutableListOf<JSONObject>()

        fun matchesType(page: JSONObject, filter: String): Boolean {
            val type = page.optString("page_type").lowercase()
            return when (filter) {
                "Ministries" -> type == "ministry"
                "Churches" -> type == "church"
                "Organisations" -> type == "organisation"
                "Preachers" -> type == "public_figure"
                "Authors" -> type == "author"
                "Artists" -> type == "artist"
                else -> true
            }
        }
        fun renderDirectory() {
            directory.removeAllViews()
            val term = search.text.toString().trim().lowercase()
            val filtered = allPages.filter { p ->
                matchesType(p, selectedFilter) &&
                    (term.isBlank() || listOf(p.optString("name"), p.optString("description"), p.optString("city"), p.optString("page_type")).any { it.lowercase().contains(term) })
            }
            resultsTitle.text = if (selectedFilter == "All") "ALL PAGES · ${filtered.size}" else "${selectedFilter.uppercase()} · ${filtered.size}"
            if (filtered.isEmpty()) {
                val empty = card(directory)
                text(empty, if (allPages.isEmpty()) "No pages have joined ELIX yet." else "No pages match your search.", 15, white, true)
                text(empty, "Try another name or category, or create a page for your ministry.", 12, muted, false)
                if (AuthApi.isSignedIn(this)) button(empty, "＋ CREATE A PAGE") { createPageDialog() }
                return
            }
            filtered.forEachIndexed { index, p ->
                val pageId = p.optString("id")
                val name = p.optString("name").ifBlank { "ELIX Page" }
                val type = p.optString("page_type").replace("_", " ").replaceFirstChar { it.uppercase() }
                val city = p.optString("city").ifBlank { "Online" }
                val item = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(13), dp(13), dp(13), dp(13))
                    background = GradientDrawable().apply { setColor(panel); cornerRadius = dp(17).toFloat(); setStroke(dp(1), Color.rgb(232,232,232)) }
                    isClickable = true; isFocusable = true
                    setOnClickListener { showCreatorPage(pageId) }
                }
                val avatarFrame = FrameLayout(this)
                val fallback = TextView(this).apply {
                    text = name.trim().take(1).uppercase(); textSize = 22f; gravity = Gravity.CENTER; setTextColor(Color.WHITE)
                    typeface = Typeface.DEFAULT_BOLD
                    background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.rgb(0,149,246)) }
                }
                avatarFrame.addView(fallback, FrameLayout.LayoutParams(-1,-1))
                val avatar = ImageView(this).apply { scaleType = ImageView.ScaleType.CENTER_CROP; background = GradientDrawable().apply { shape = GradientDrawable.OVAL } }
                avatarFrame.addView(avatar, FrameLayout.LayoutParams(-1,-1))
                if (p.optString("logo_url").isNotBlank()) loadRemoteImage(p.optString("logo_url"), avatar)
                item.addView(avatarFrame, LinearLayout.LayoutParams(dp(58),dp(58)))
                val details = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12),0,dp(8),0) }
                val title = TextView(this).apply { text = name; textSize = 15f; setTextColor(white); typeface = Typeface.create("sans-serif-medium",Typeface.BOLD); maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END }
                val subtitle = TextView(this).apply { text = type + "  ·  " + city; textSize = 11f; setTextColor(gold); maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END }
                val description = TextView(this).apply { text = p.optString("description").ifBlank { "Explore messages, videos and updates." }; textSize = 12f; setTextColor(muted); maxLines = 2; ellipsize = android.text.TextUtils.TruncateAt.END }
                details.addView(title); details.addView(subtitle); details.addView(description)
                item.addView(details, LinearLayout.LayoutParams(0,-2,1f))
                val arrow = TextView(this).apply { text = "›"; textSize = 30f; setTextColor(muted); gravity = Gravity.CENTER }
                item.addView(arrow, LinearLayout.LayoutParams(dp(22),dp(48)))
                val params = fullWidth().apply { bottomMargin = dp(9) }
                directory.addView(item,params)
            }
        }
        fun renderChips() {
            chipRow.removeAllViews()
            filters.forEach { filter ->
                val active = filter == selectedFilter
                val chip = TextView(this).apply {
                    text = filter; textSize = 12f; gravity = Gravity.CENTER
                    setPadding(dp(15),dp(9),dp(15),dp(9))
                    setTextColor(if (active) Color.WHITE else muted)
                    background = GradientDrawable().apply { setColor(if (active) Color.rgb(0,149,246) else panel); cornerRadius = dp(22).toFloat(); if (!active) setStroke(dp(1),Color.rgb(225,225,225)) }
                    setOnClickListener { selectedFilter = filter; renderChips(); renderDirectory() }
                }
                val params = LinearLayout.LayoutParams(-2,dp(38)).apply { marginEnd = dp(8) }
                chipRow.addView(chip,params)
            }
        }
        renderChips()
        search.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { renderDirectory() }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        text(directory, "Loading pages…", 13, muted, false)
        SupabaseApi.getWithError(this, "ministries?select=id,name,city,page_type,description,owner_id,logo_url&or=(approved.eq.true,owner_id.eq.${AuthApi.currentUserId(this)})&order=name.asc&limit=300") { rows, error ->
            runOnUiThread {
                if (error != null) {
                    directory.removeAllViews()
                    text(directory, "The directory could not load: $error", 12, signalRed, false)
                } else {
                    allPages.clear()
                    for (i in 0 until rows.length()) allPages.add(rows.getJSONObject(i))
                    renderDirectory()
                }
            }
        }
    }

    private fun showExploreHub() {
        base("Explore", "Explore ELIX", "DISCOVER", "Find ministries, books, eBooks, libraries, radio and live broadcasts.")
        tile(content, "MINISTRIES & PUBLIC FIGURES", "Explore ministry and creator pages.") { showHome() }
        tile(content, "LIBRARY & EBOOKS", "Browse Christian books and digital resources.") { showLibrary() }
        tile(content, "RADIO", "Listen to ELIX Radio and audio programmes.") { showRadio() }
        tile(content, "LIVE RADIO", "Listen to live audio broadcasts.") { showLiveRadio() }
        tile(content, "LIVE VIDEO", "Watch live ministry broadcasts.") { showLive() }
        tile(content, "UPCOMING PROGRAMMES", "Conferences, services and events.") { showUpcoming() }
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
        SupabaseApi.getWithError(this, "ministries?select=id,name,description,city,page_type,logo_url&id=eq.$pageId&limit=1") { pages, error ->
            runOnUiThread { profile.removeAllViews(); if (error != null || pages.length() == 0) text(profile, "Page could not be loaded.", 13, signalRed, false) else {
                val p = pages.getJSONObject(0)
                val logo = ImageView(this).apply { scaleType = ImageView.ScaleType.CENTER_CROP; background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.LTGRAY) } }
                profile.addView(logo, LinearLayout.LayoutParams(dp(92), dp(92)))
                if (p.optString("logo_url").isNotBlank()) loadRemoteImage(p.optString("logo_url"), logo)
                text(profile, p.optString("name"), 23, white, true); text(profile, p.optString("page_type").replace("_"," ").uppercase() + " • " + p.optString("city","Nigeria"), 12, gold, true); text(profile, p.optString("description"), 14, muted, false)
                val followerCount = TextView(this).apply { textSize = 13f; setTextColor(muted) }
                profile.addView(followerCount)
                val followButton = Button(this).apply { isAllCaps = false }
                if (AuthApi.isSignedIn(this)) profile.addView(followButton)
                fun refreshFollowState() {
                    SupabaseApi.getWithError(this, "ministry_follower_counts?select=follower_count&ministry_id=eq.$pageId&limit=1") { counts, countError ->
                        val total = if (countError == null && counts.length() > 0) counts.getJSONObject(0).optLong("follower_count") else 0L
                        runOnUiThread { followerCount.text = if (countError == null) "$total followers" else "Followers" }
                        if (AuthApi.isSignedIn(this)) {
                            SupabaseApi.getWithError(this, "ministry_follows?select=user_id&user_id=eq.${AuthApi.currentUserId(this)}&ministry_id=eq.$pageId&limit=1") { mineRows, followError ->
                                runOnUiThread {
                                    val mine = followError == null && mineRows.length() > 0
                                    followButton.text = if (mine) "FOLLOWING · TAP TO UNFOLLOW" else "＋ FOLLOW PAGE"
                                    followButton.setOnClickListener {
                                        if (mine) SupabaseApi.deleteWithError(this, "ministry_follows?user_id=eq.${AuthApi.currentUserId(this)}&ministry_id=eq.$pageId") { e ->
                                            runOnUiThread { info(if (e == null) "Unfollowed page." else "Could not unfollow: $e"); refreshFollowState() }
                                        } else SupabaseApi.postWithError(this, "ministry_follows", JSONObject().put("user_id", AuthApi.currentUserId(this)).put("ministry_id", pageId)) { e ->
                                            runOnUiThread { info(if (e == null) "Now following this page." else "Could not follow: $e"); refreshFollowState() }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                refreshFollowState()
            } }
        }
        text(content, "UPCOMING PROGRAMMES & CONFERENCES", 16, white, true); val programmes = card(content)
        SupabaseApi.getWithError(this, "programmes?select=title,description,starts_at&ministry_id=eq.$pageId&status=eq.scheduled&order=starts_at.asc&limit=50") { rows, e -> runOnUiThread { programmes.removeAllViews(); if (e != null || rows.length() == 0) text(programmes, "No upcoming programmes announced.", 13, muted, false) else for (i in 0 until rows.length()) { val x=rows.getJSONObject(i); text(programmes,x.optString("title"),15,gold,true); text(programmes,x.optString("starts_at").replace("T"," "),12,muted,false); text(programmes,x.optString("description"),13,muted,false) } } }
        listOf("BOOKS" to "pdf", "AUDIO MESSAGES" to "audio", "SHORTS & VIDEOS" to "video", "IMAGES" to "image").forEach { (heading, type) ->
            text(content, heading, 16, white, true); val section = card(content)
            SupabaseApi.getWithError(this, "media_items?select=id,title,kind,media_type,file_path&ministry_id=eq.$pageId&status=eq.approved&media_type=eq.$type&order=created_at.desc&limit=40") { rows, e -> runOnUiThread { section.removeAllViews(); if (e != null || rows.length() == 0) text(section, "Nothing published here yet.", 13, muted, false) else for (i in 0 until rows.length()) { val x=rows.getJSONObject(i); val t=x.optString("title"); text(section,t,15,gold,true); text(section,x.optString("kind").replace("_"," ").uppercase(),11,muted,false); button(section,if(type=="audio") "▶ PLAY AUDIO" else "OPEN") { if(type=="audio") playAudio(t,x.optString("file_path")) else openMedia(x.optString("file_path"),type) } } } }
        }
        text(content, "LIVE MESSAGES", 16, white, true); tile(content, "Live broadcasts", "See authorised live radio and video sessions.") { showBroadcasts() }
        button(content, "BACK TO PAGES") { showHome() }
    }

    private fun showPageManager(page: JSONObject) {
        val pageId = page.optString("id")
        selectedPageId = pageId
        base("Account", "Page account", page.optString("name"), "Your ministry's independent publishing and management space.")
        button(content, "CHANGE PAGE PROFILE PICTURE") { chooseProfileImage(pageId) }
        text(content, "PAGE CONTENT STUDIO", 13, gold, true)
        tile(content, "Audio messages & podcasts", "Upload sermons, worship, teaching, interviews and podcasts.") { selectedPageId = pageId; selectedUploadType = "audio"; chooseUploadFile() }
        tile(content, "Videos & short clips", "Upload sermons, testimonies, teaching, event videos and shorts.") { selectedPageId = pageId; selectedUploadType = "video"; chooseUploadFile() }
        tile(content, "Images & announcements", "Publish flyers, photos, banners and ministry announcements.") { selectedPageId = pageId; selectedUploadType = "image"; chooseUploadFile() }
        tile(content, "Books & eBooks", "Upload PDF books, study guides, devotionals and resources.") { selectedPageId = pageId; selectedUploadType = "pdf"; chooseUploadFile() }
        tile(content, "Write a post", "Share teachings, updates, testimonies and articles.") { createPageTextContent(pageId, "post") }
        tile(content, "Create a story", "Publish a short ministry update or announcement.") { createPageTextContent(pageId, "story") }
        tile(content, "Live broadcasts", "Open live radio and live video tools.") { showBroadcasts() }
        tile(content, "Programmes & events", "Services, conferences, crusades and event listings.") { showUpcoming() }
        text(content, "PUBLISHED PAGE MEDIA", 16, white, true)
        val media = card(content)
        SupabaseApi.getWithError(this, "media_items?select=id,title,media_type,kind&ministry_id=eq.$pageId&order=created_at.desc&limit=100") { rows, error ->
            runOnUiThread {
                media.removeAllViews()
                if (error != null) text(media, "Could not load page content: $error", 12, signalRed, false)
                else if (rows.length() == 0) text(media, "Your page is ready. Start publishing content above.", 13, muted, false)
                else for (i in 0 until rows.length()) { val item = rows.getJSONObject(i); text(media, item.optString("title"), 14, white, true); text(media, item.optString("media_type").uppercase() + " · " + item.optString("kind").replace("_", " "), 11, muted, false) }
            }
        }
        button(content, "VIEW PUBLIC PAGE") { showCreatorPage(pageId) }
        button(content, "BACK TO MY PAGES") { showMyPages() }
    }

    private fun createPageTextContent(pageId: String, type: String) {
        val title = android.widget.EditText(this).apply { hint = "Heading"; setTextColor(white) }
        val body = android.widget.EditText(this).apply { hint = "Write your ministry update…"; setTextColor(white); minLines = 4; gravity = Gravity.TOP }
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(8), dp(16), 0); addView(title); addView(body) }
        val dialog = android.app.AlertDialog.Builder(this).setTitle(if (type == "story") "Create page story" else "Write page post").setView(box).setNegativeButton("Cancel", null).setPositiveButton("PUBLISH", null).create()
        dialog.setOnShowListener {
            dialog.getButton(-1).setOnClickListener {
                val heading = title.text.toString().trim()
                val textBody = body.text.toString().trim()
                if (heading.isBlank() || textBody.isBlank()) { info("Add a heading and content first."); return@setOnClickListener }
                val data = JSONObject().put("ministry_id", pageId).put("content_type", type).put("title", heading).put("body", textBody).put("created_by", AuthApi.currentUserId(this))
                SupabaseApi.postWithError(this, "ministry_publications", data) { error ->
                    runOnUiThread { info(if (error == null) "Published to your page." else "Could not publish: $error"); if (error == null) showMyPages() }
                }
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun createPageDialog() {
        if (!AuthApi.isSignedIn(this)) { info("Sign in before creating a page."); return }
        val name = android.widget.EditText(this).apply { hint = "Ministry or public figure name"; setTextColor(white) }
        val desc = android.widget.EditText(this).apply { hint = "About this page"; setTextColor(white); minLines = 2 }
        val city = android.widget.EditText(this).apply { hint = "City / location"; setTextColor(white) }
        val type = android.widget.Spinner(this).apply { adapter = android.widget.ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, arrayOf("Ministry", "Church", "Christian organisation", "Public figure", "Christian author", "Christian artist")) }
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(8), dp(16), 0); addView(name); addView(type); addView(desc); addView(city) }
        val dialog = android.app.AlertDialog.Builder(this).setTitle("Create a ministry page").setMessage("Create a separate public account for your ministry or Christian work.").setView(box).setNegativeButton("Cancel", null).setPositiveButton("CREATE PAGE", null).create()
        dialog.setOnShowListener {
            dialog.getButton(-1).setOnClickListener {
                val n = name.text.toString().trim()
                if (n.isBlank()) { name.error = "Enter a page name"; return@setOnClickListener }
                val slug = n.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-') + "-" + System.currentTimeMillis().toString().takeLast(6)
                val kind = when (type.selectedItemPosition) { 1 -> "church"; 2 -> "organisation"; 3 -> "public_figure"; 4 -> "author"; 5 -> "artist"; else -> "ministry" }
                val data = JSONObject().put("name", n).put("slug", slug).put("description", desc.text.toString().trim()).put("city", city.text.toString().trim()).put("page_type", kind).put("owner_id", AuthApi.currentUserId(this)).put("approved", true)
                dialog.getButton(-1).isEnabled = false
                SupabaseApi.postWithError(this, "ministries", data) { error ->
                    runOnUiThread {
                        if (error == null) { dialog.dismiss(); info("Your page account has been created."); showMyPages() }
                        else { info("Page creation failed: $error"); dialog.getButton(-1).isEnabled = true }
                    }
                }
            }
        }
        dialog.show()
    }

    private fun chooseProfileImage(pageId: String?) {
        if (!AuthApi.isSignedIn(this)) { info("Sign in first."); return }
        imageTargetPageId = pageId
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "image/*" }
        startActivityForResult(intent, if (pageId.isNullOrBlank()) profileImageRequestCode else pageImageRequestCode)
    }

    private fun loadRemoteImage(url: String, target: ImageView) {
        Thread {
            try { val bitmap = URL(url).openStream().use { BitmapFactory.decodeStream(it) }; runOnUiThread { if (bitmap != null && target.parent != null) target.setImageBitmap(bitmap) } } catch (_: Exception) { }
        }.start()
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


    private fun confirmDeleteUpload(item: JSONObject) {
        android.app.AlertDialog.Builder(this).setTitle("Delete uploaded file?")
            .setMessage("\"" + item.optString("title") + "\" will be removed from ELIX and its stored file deleted. This cannot be undone.")
            .setNegativeButton("Cancel", null).setPositiveButton("DELETE") { _, _ ->
                val id = item.optString("id")
                val path = item.optString("file_path")
                if (id.isBlank() || path.isBlank()) { info("This upload is missing its file reference."); return@setPositiveButton }
                UploadApi.deleteStoredFile(this, path) { storageError ->
                    if (storageError != null) runOnUiThread { info("File deletion failed: $storageError") }
                    else SupabaseApi.deleteWithError(this, "media_items?id=eq.$id&uploaded_by=eq.${AuthApi.currentUserId(this)}") { dbError ->
                        runOnUiThread { info(if (dbError == null) "Upload deleted." else "File removed, but catalogue cleanup failed: $dbError"); showAccount() }
                    }
                }
            }.show()
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
        val height = -1
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
        val category = TextView(this).apply { textSize = 12f; setTextColor(Color.rgb(255, 45, 85)); setTypeface(null, Typeface.BOLD) }
        homeFeedCategory = category
        overlay.addView(category)
        val title = TextView(this).apply { textSize = 21f; setTextColor(white); setTypeface(null, Typeface.BOLD); setPadding(0, dp(5), 0, dp(8)) }
        homeFeedTitle = title
        overlay.addView(title)
        val position = TextView(this).apply { text = "Loading your feed…"; textSize = 12f; setTextColor(muted) }
        homeFeedPosition = position
        overlay.addView(position)
        val playback = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; setPadding(dp(8), dp(5), dp(8), dp(5)); background = GradientDrawable().apply { setColor(0x66000000); cornerRadius = dp(22).toFloat() } }
        val backSeek = Button(this).apply { text = "−10s"; isAllCaps = false; setOnClickListener { seekFeed(-10000) } }
        val pausePlay = Button(this).apply { text = "Ⅱ  PAUSE"; isAllCaps = false; setOnClickListener { toggleFeedPlayback() } }
        val forwardSeek = Button(this).apply { text = "+10s"; isAllCaps = false; setOnClickListener { seekFeed(10000) } }
        feedPlayButton = pausePlay
        playback.addView(backSeek); playback.addView(pausePlay); playback.addView(forwardSeek)
        overlay.addView(playback, LinearLayout.LayoutParams(-1, dp(48)))
        val actionRail = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(5), dp(8), dp(5), dp(8))
            background = GradientDrawable().apply { setColor(0x55000000); cornerRadius = dp(18).toFloat() }
        }
        fun railAction(symbol: String, label: String, action: () -> Unit): TextView {
            val item = TextView(this).apply {
                text = "$symbol\n$label"
                gravity = Gravity.CENTER
                textSize = 11f
                setTextColor(white)
                setTypeface(null, Typeface.BOLD)
                setPadding(dp(5), dp(8), dp(5), dp(8))
                setOnClickListener { action() }
            }
            actionRail.addView(item, LinearLayout.LayoutParams(dp(62), dp(58)))
            return item
        }
        railAction("＋", "FOLLOW") { followCurrentCreator() }
        val likeAction = railAction("♡", "0") { val item = homeFeedItems.getOrNull(homeFeedIndex); if (item != null) { FeedEngagementApi.record(this@MainActivity, item.optString("id"), "like"); info("Liked"); refreshFeedActionCounts(item) } }
        val commentAction = railAction("▢", "0") { openCommentsForCurrentItem() }
        val shareAction = railAction("↗", "0") { shareCurrentFeedItem() }
        railAction("★", "SUBSCRIBE") { subscribeCurrentCreator() }
        homeFeedFrame?.tag = listOf(likeAction, commentAction, shareAction)
        val railParams = FrameLayout.LayoutParams(dp(70), -2, Gravity.RIGHT or Gravity.CENTER_VERTICAL)
        railParams.rightMargin = dp(8)
        frame.addView(actionRail, railParams)
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
        SupabaseApi.getWithError(this, "media_items?select=id,title,kind,media_type,file_path,duration_seconds,ministry_id&status=eq.approved&order=created_at.desc&limit=200") { rows, error ->
            runOnUiThread {
                if (isFinishing || homeFeedFrame !== frame) return@runOnUiThread
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


    private fun openCommentsForCurrentItem() {
        val item = homeFeedItems.getOrNull(homeFeedIndex) ?: return
        val mediaId = item.optString("id")
        val panelView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(8))
            setBackgroundColor(panel)
        }
        val commentList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(this).apply { addView(commentList) }
        panelView.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        val input = android.widget.EditText(this).apply { hint = "Add a comment…"; setTextColor(white); setHintTextColor(muted) }
        val send = Button(this).apply { text = "POST COMMENT"; isAllCaps = false }
        val composer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; addView(input); addView(send) }
        panelView.addView(composer, LinearLayout.LayoutParams(-1, -2))
        val dialog = android.app.AlertDialog.Builder(this).setTitle("Comments · " + item.optString("title"))
            .setView(panelView).setPositiveButton("CLOSE", null).create()
        fun loadComments() {
            commentList.removeAllViews()
            text(commentList, "Loading comments…", 13, muted, false)
            SupabaseApi.getWithError(this, "media_comments?select=id,comment_text,author_name,created_at&media_id=eq.$mediaId&order=created_at.asc&limit=200") { rows, error ->
                runOnUiThread {
                    commentList.removeAllViews()
                    if (error != null) text(commentList, "Comments could not be loaded: $error", 12, signalRed, false)
                    else if (rows.length() == 0) text(commentList, "No comments yet. Start the conversation.", 13, muted, false)
                    else for (i in 0 until rows.length()) {
                        val c = rows.getJSONObject(i)
                        text(commentList, c.optString("author_name").ifBlank { "ELIX member" }, 12, gold, true)
                        text(commentList, c.optString("comment_text"), 14, white, false)
                        space(commentList, 8)
                    }
                }
            }
        }
        send.setOnClickListener {
            if (!AuthApi.isSignedIn(this)) { info("Sign in to comment."); return@setOnClickListener }
            val value = input.text.toString().trim()
            if (value.isBlank()) { input.error = "Write a comment first"; return@setOnClickListener }
            send.isEnabled = false
            val body = JSONObject().put("media_id", mediaId).put("user_id", AuthApi.currentUserId(this)).put("author_name", AuthApi.currentName(this).ifBlank { "ELIX member" }).put("comment_text", value)
            SupabaseApi.postWithError(this, "media_comments", body) { e ->
                runOnUiThread {
                    send.isEnabled = true
                    if (e == null) { input.setText(""); loadComments() } else info("Comment could not be posted: $e")
                }
            }
        }
        loadComments()
        dialog.show()
        dialog.window?.setLayout(-1, (resources.displayMetrics.heightPixels * 0.72f).toInt())
    }

    private fun followCurrentCreator() {
        val item = homeFeedItems.getOrNull(homeFeedIndex) ?: return
        val pageId = item.optString("ministry_id")
        if (pageId.isBlank()) { info("This post is not linked to a ministry page yet."); return }
        if (!AuthApi.isSignedIn(this)) { showAccount(); return }
        val userId = AuthApi.currentUserId(this)
        SupabaseApi.getWithError(this, "ministry_follows?select=user_id&user_id=eq.$userId&ministry_id=eq.$pageId&limit=1") { rows, e ->
            if (e != null) { runOnUiThread { info("Could not check follow status: $e") }; return@getWithError }
            if (rows.length() > 0) SupabaseApi.deleteWithError(this, "ministry_follows?user_id=eq.$userId&ministry_id=eq.$pageId") { err ->
                runOnUiThread { info(if (err == null) "Unfollowed creator." else "Could not unfollow: $err") }
            } else SupabaseApi.postWithError(this, "ministry_follows", JSONObject().put("user_id", userId).put("ministry_id", pageId)) { err ->
                runOnUiThread { info(if (err == null) "Following creator." else "Follow could not be saved: $err") }
            }
        }
    }

    private fun subscribeCurrentCreator() {
        val item = homeFeedItems.getOrNull(homeFeedIndex) ?: return
        val creatorId = item.optString("uploaded_by")
        if (creatorId.isBlank()) { info("Creator information is not available for this post."); return }
        if (!AuthApi.isSignedIn(this)) { showAccount(); return }
        val userId = AuthApi.currentUserId(this)
        if (creatorId == userId) { info("This is your own content."); return }
        SupabaseApi.getWithError(this, "creator_subscriptions?select=creator_id&user_id=eq.$userId&creator_id=eq.$creatorId&limit=1") { rows, lookupError ->
            if (lookupError != null) { runOnUiThread { info("Subscription service unavailable: $lookupError") }; return@getWithError }
            if (rows.length() > 0) {
                SupabaseApi.deleteWithError(this, "creator_subscriptions?user_id=eq.$userId&creator_id=eq.$creatorId") { e ->
                    runOnUiThread { info(if (e == null) "Subscription cancelled." else "Could not cancel subscription: $e") }
                }
            } else {
                val body = JSONObject().put("user_id", userId).put("creator_id", creatorId)
                SupabaseApi.postWithError(this, "creator_subscriptions", body) { e ->
                    runOnUiThread { info(if (e == null) "Subscribed to this creator." else "Subscription failed: $e") }
                }
            }
        }
    }

    private var feedTouchY = 0f
    private var feedTransitionRunning = false
    private val profileImageRequestCode = 4108
    private val pageImageRequestCode = 4109
    private var imageTargetPageId: String? = null
    private var feedPlayButton: Button? = null

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
        if (homeFeedItems.isEmpty() || feedTransitionRunning) return
        val next = (homeFeedIndex + direction).coerceIn(0, homeFeedItems.lastIndex)
        if (next == homeFeedIndex) return
        val frame = homeFeedFrame
        if (frame == null || frame.height <= 0) {
            homeFeedIndex = next
            displayHomeFeedItem()
            return
        }
        // Slide the current full-screen item away, then bring the next item
        // in from the opposite edge for a continuous TikTok-style feed.
        feedTransitionRunning = true
        val travel = frame.height.toFloat()
        val outgoingY = if (direction > 0) -travel else travel
        val incomingY = -outgoingY
        frame.animate().cancel()
        frame.animate().translationY(outgoingY).setDuration(240L).withEndAction {
            if (homeFeedFrame !== frame || isFinishing) {
                feedTransitionRunning = false
                return@withEndAction
            }
            homeFeedIndex = next
            displayHomeFeedItem()
            frame.translationY = incomingY
            frame.animate().translationY(0f).setDuration(280L)
                .withEndAction { feedTransitionRunning = false }.start()
        }.start()
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
        feedPlayButton?.text = "Ⅱ  PAUSE"
        refreshFeedActionCounts(item)
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

    private fun toggleFeedPlayback() {
        val video = homeVideo
        if (video != null && video.visibility == View.VISIBLE) {
            if (video.isPlaying) { video.pause(); feedPlayButton?.text = "▶  PLAY" } else { video.start(); feedPlayButton?.text = "Ⅱ  PAUSE" }
            return
        }
        try {
            val player = mediaPlayer ?: return
            if (player.isPlaying) { player.pause(); feedPlayButton?.text = "▶  PLAY" } else { player.start(); feedPlayButton?.text = "Ⅱ  PAUSE" }
        } catch (_: Exception) { info("Playback is not ready yet.") }
    }

    private fun seekFeed(deltaMs: Int) {
        try {
            val video = homeVideo
            if (video != null && video.visibility == View.VISIBLE) { video.seekTo((video.currentPosition + deltaMs).coerceAtLeast(0)); return }
            val player = mediaPlayer ?: return
            player.seekTo((player.currentPosition + deltaMs).coerceAtLeast(0))
        } catch (_: Exception) { info("Seeking is not available for this item.") }
    }

    private fun refreshFeedActionCounts(item: JSONObject) {
        val views = homeFeedFrame?.tag as? List<*> ?: return
        val mediaId = item.optString("id")
        val likeView = views.getOrNull(0) as? TextView
        val commentView = views.getOrNull(1) as? TextView
        val shareView = views.getOrNull(2) as? TextView
        SupabaseApi.getWithError(this, "media_engagement_events?select=id&media_id=eq.$mediaId&event_type=eq.like&limit=1000") { rows, _ -> runOnUiThread { if (homeFeedItems.getOrNull(homeFeedIndex) === item) likeView?.text = "♡\\n" + rows.length() } }
        SupabaseApi.getWithError(this, "media_comments?select=id&media_id=eq.$mediaId&limit=1000") { rows, _ -> runOnUiThread { if (homeFeedItems.getOrNull(homeFeedIndex) === item) commentView?.text = "▢\\n" + rows.length() } }
        SupabaseApi.getWithError(this, "media_engagement_events?select=id&media_id=eq.$mediaId&event_type=eq.share&limit=1000") { rows, _ -> runOnUiThread { if (homeFeedItems.getOrNull(homeFeedIndex) === item) shareView?.text = "↗\\n" + rows.length() } }
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

    private fun openMedia(path: String, mediaType: String = "video") {
        if (path.isBlank()) { info("This upload has no file path."); return }
        val url = BuildConfig.SUPABASE_URL.trimEnd('/') + "/storage/v1/object/public/elix-media/" + Uri.encode(path, "/")
        when (mediaType) {
            "video" -> {
                val player = VideoView(this)
                val box = FrameLayout(this).apply { setBackgroundColor(Color.BLACK); addView(player, FrameLayout.LayoutParams(-1, dp(260), Gravity.CENTER)) }
                val dialog = android.app.AlertDialog.Builder(this).setView(box).setNegativeButton("Close") { _, _ -> player.stopPlayback() }.create()
                player.setVideoURI(Uri.parse(url))
                player.setOnPreparedListener { it.isLooping = false; player.start() }
                player.setOnErrorListener { _, _, _ -> info("This video could not be played."); dialog.dismiss(); true }
                dialog.setOnDismissListener { player.stopPlayback() }
                dialog.show()
            }
            "image" -> {
                val image = ImageView(this).apply { scaleType = ImageView.ScaleType.FIT_CENTER; setBackgroundColor(Color.BLACK) }
                val dialog = android.app.AlertDialog.Builder(this).setView(image).setPositiveButton("Close", null).create()
                loadRemoteImage(url, image)
                dialog.show()
            }
            "pdf" -> {
                val web = android.webkit.WebView(this).apply { settings.javaScriptEnabled = false; setBackgroundColor(Color.WHITE); loadUrl("https://docs.google.com/gview?embedded=1&url=" + Uri.encode(url)) }
                android.app.AlertDialog.Builder(this).setTitle("ELIX Book Reader").setView(web).setPositiveButton("Close", null).show()
            }
            else -> info("This media format is not supported yet.")
        }
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
        if ((requestCode == profileImageRequestCode || requestCode == pageImageRequestCode) && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            val pageId = if (requestCode == pageImageRequestCode) imageTargetPageId else null
            UploadApi.uploadProfileImage(this, uri, AuthApi.currentUserId(this)) { result ->
                runOnUiThread {
                    if (!result.first) { info("Picture upload failed: " + result.second); return@runOnUiThread }
                    val endpoint = if (pageId.isNullOrBlank()) "profiles?id=eq." + AuthApi.currentUserId(this) else "ministries?id=eq." + pageId
                    val field = if (pageId.isNullOrBlank()) "avatar_url" else "logo_url"
                    SupabaseApi.patchWithError(this, endpoint, JSONObject().put(field, result.second)) { error ->
                        runOnUiThread { info(if (error == null) "Profile picture updated." else "Picture update failed: $error"); if (pageId.isNullOrBlank()) showAccount() else showMyPages() }
                    }
                }
            }
            return
        }
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
                                if (selectedUploadType == "video" && !result.filePath.isNullOrBlank()) {
                                    Thread {
                                        try {
                                            val retriever = MediaMetadataRetriever()
                                            retriever.setDataSource(this, uri)
                                            val frame = retriever.getFrameAtTime(1000000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                                            retriever.release()
                                            if (frame != null) {
                                                val output = ByteArrayOutputStream()
                                                frame.compress(android.graphics.Bitmap.CompressFormat.JPEG, 82, output)
                                                UploadApi.uploadThumbnailBytes(this, output.toByteArray(), AuthApi.currentUserId(this)) { thumbnail ->
                                                    if (thumbnail.first) SupabaseApi.patchWithError(this,
                                                        "media_items?uploaded_by=eq." + AuthApi.currentUserId(this) + "&file_path=eq." + Uri.encode(result.filePath!!),
                                                        JSONObject().put("thumbnail_url", thumbnail.second)) { _ -> }
                                                }
                                            }
                                        } catch (_: Exception) { }
                                    }.start()
                                }
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
        base("Account", "YOUR ELIX PROFILE", "", "")
        content.setPadding(dp(14), dp(12), dp(14), dp(24))
        content.setBackgroundColor(Color.WHITE)
        if (!AuthApi.isSignedIn(this)) {
            text(content, "Welcome to ELIX", 25, white, true)
            text(content, "Sign in to manage your profile, publish content and connect with creators.", 14, muted, false)
            button(content, "CREATE ACCOUNT") { showAuthDialog(true) }
            button(content, "SIGN IN") { showAuthDialog(false) }
            return
        }
        val userId = AuthApi.currentUserId(this)
        val displayName = AuthApi.currentName(this).ifBlank { "ELIX member" }
        val handle = displayName.lowercase().replace(Regex("[^a-z0-9]+"), ".").trim('.')
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        text(top, "elix", 28, Color.BLACK, true)
        val spacer = View(this); top.addView(spacer, LinearLayout.LayoutParams(0, dp(42), 1f))
        val add = TextView(this).apply { text = "＋"; textSize = 29f; setTextColor(Color.BLACK); gravity = Gravity.CENTER; setOnClickListener { chooseUploadFile() } }
        val settings = TextView(this).apply { text = "☰"; textSize = 25f; setTextColor(Color.BLACK); gravity = Gravity.CENTER; setOnClickListener { editProfileDialog() } }
        top.addView(add, LinearLayout.LayoutParams(dp(42), dp(42))); top.addView(settings, LinearLayout.LayoutParams(dp(42), dp(42)))
        content.addView(top, fullWidth())
        val handleText = TextView(this).apply { text = handle; textSize = 14f; setTextColor(Color.BLACK); setTypeface(null, Typeface.BOLD) }
        content.addView(handleText)
        val profileRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(14), 0, dp(12)) }
        val avatarFrame = FrameLayout(this)
        val avatarFallback = TextView(this).apply { text = displayName.take(1).uppercase(); textSize = 28f; gravity = Gravity.CENTER; setTextColor(Color.WHITE); background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.rgb(195, 80, 145)) } }
        avatarFrame.addView(avatarFallback, FrameLayout.LayoutParams(-1,-1))
        val avatarImage = ImageView(this).apply { scaleType = ImageView.ScaleType.CENTER_CROP; background = GradientDrawable().apply { shape = GradientDrawable.OVAL } }
        avatarFrame.addView(avatarImage, FrameLayout.LayoutParams(-1,-1))
        avatarFrame.setOnClickListener { chooseProfileImage(null) }
        profileRow.addView(avatarFrame, LinearLayout.LayoutParams(dp(88),dp(88)))
        SupabaseApi.getWithError(this, "profiles?select=avatar_url&id=eq.$userId&limit=1") { rows, _ ->
            val url = if (rows.length()>0) rows.getJSONObject(0).optString("avatar_url") else ""
            if (url.isNotBlank()) loadRemoteImage(url, avatarImage)
        }
        val stats = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; weightSum = 3f }
        fun stat(label: String): TextView {
            val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER }
            val value = TextView(this).apply { text = "…"; textSize = 19f; setTextColor(Color.BLACK); setTypeface(null, Typeface.BOLD) }
            val caption = TextView(this).apply { text = label; textSize = 12f; setTextColor(Color.DKGRAY) }
            col.addView(value); col.addView(caption); stats.addView(col, LinearLayout.LayoutParams(0,-2,1f))
            return value
        }
        val postCount = stat("posts"); val followerCount = stat("followers"); val followingCount = stat("following")
        profileRow.addView(stats, LinearLayout.LayoutParams(0,-2,1f))
        content.addView(profileRow, fullWidth())
        text(content, displayName, 14, Color.BLACK, true)
        text(content, "ELIX Christian Media creator", 13, Color.DKGRAY, false)
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; setPadding(0,dp(12),0,dp(10)) }
        fun profileAction(label: String, action: () -> Unit) {
            val b = Button(this).apply { text=label; isAllCaps=false; textSize=12f; setTextColor(Color.BLACK); background=GradientDrawable().apply { setColor(Color.rgb(239,239,239)); cornerRadius=dp(7).toFloat(); setStroke(dp(1),Color.rgb(225,225,225)) }; setOnClickListener { action() } }
            actions.addView(b,LinearLayout.LayoutParams(0,dp(40),1f).apply { marginEnd=dp(5) })
        }
        profileAction("Edit profile") { editProfileDialog() }
        profileAction("Share profile") {
            val invite = "elix://invite/$userId"
            val share = Intent(Intent.ACTION_SEND).apply { type="text/plain"; putExtra(Intent.EXTRA_TEXT,"Join me on ELIX Christian Media Network. My personal invitation link: $invite") }
            startActivity(Intent.createChooser(share,"Share your ELIX invitation"))
        }
        content.addView(actions,fullWidth())
        SupabaseApi.getWithError(this,"media_items?select=id&uploaded_by=eq.$userId&status=eq.approved&limit=1000") { posts, _ ->
            SupabaseApi.getWithError(this,"creator_follower_counts?select=follower_count&creator_id=eq.$userId&limit=1") { followers, _ ->
                SupabaseApi.getWithError(this,"creator_follows?select=creator_id&follower_id=eq.$userId") { following, _ ->
                    runOnUiThread {
                        postCount.text=posts.length().toString()
                        followerCount.text=if(followers.length()>0) followers.getJSONObject(0).optLong("follower_count").toString() else "0"
                        followingCount.text=following.length().toString()
                    }
                }
            }
        }
        val tabs = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER }
        val postsTab = TextView(this).apply { text="▦"; textSize=25f; gravity=Gravity.CENTER; setTextColor(Color.BLACK); setPadding(0,dp(10),0,dp(10)) }
        tabs.addView(postsTab,LinearLayout.LayoutParams(0,dp(46),1f))
        val aboutTab = TextView(this).apply { text="♙"; textSize=23f; gravity=Gravity.CENTER; setTextColor(Color.GRAY); setPadding(0,dp(10),0,dp(10)); setOnClickListener { info("ELIX creator profile · $displayName") } }
        tabs.addView(aboutTab,LinearLayout.LayoutParams(0,dp(46),1f))
        content.addView(tabs,fullWidth())
        content.addView(View(this).apply { setBackgroundColor(Color.LTGRAY) },LinearLayout.LayoutParams(-1,dp(1)))
        val grid=android.widget.GridLayout(this).apply { columnCount=3; useDefaultMargins=false }
        content.addView(grid,fullWidth())
        SupabaseApi.getWithError(this,"media_items?select=id,title,media_type,file_path,thumbnail_url&uploaded_by=eq.$userId&status=eq.approved&order=created_at.desc&limit=90") { rows,error ->
            runOnUiThread {
                grid.removeAllViews()
                if(error!=null) { text(content,"Your posts could not load: $error",12,signalRed,false); return@runOnUiThread }
                val width=(resources.displayMetrics.widthPixels-dp(30))/3
                if(rows.length()==0) {
                    val empty=TextView(this).apply { text="＋\nShare your first post"; textSize=14f; gravity=Gravity.CENTER; setTextColor(Color.GRAY); setPadding(dp(12),dp(30),dp(12),dp(30)) }
                    grid.addView(empty,android.widget.GridLayout.LayoutParams().apply { this.width=width*3; height=dp(150) })
                }
                for(i in 0 until rows.length()) {
                    val item=rows.getJSONObject(i); val kind=item.optString("media_type")
                    val tile=FrameLayout(this).apply { setBackgroundColor(Color.rgb(238,238,238)); layoutParams=android.widget.GridLayout.LayoutParams().apply { this.width=width; height=width; setMargins(dp(1),dp(1),dp(1),dp(1)) }; setOnClickListener { if(kind=="audio") playAudio(item.optString("title"),item.optString("file_path")) else openMedia(item.optString("file_path"),kind) } }
                    val preview=ImageView(this).apply { scaleType=ImageView.ScaleType.CENTER_CROP }
                    tile.addView(preview,FrameLayout.LayoutParams(-1,-1))
                    val thumb=item.optString("thumbnail_url")
                    if(thumb.isNotBlank()) loadRemoteImage(thumb,preview)
                    else if(kind=="image") loadRemoteImage(BuildConfig.SUPABASE_URL.trimEnd('/')+"/storage/v1/object/public/elix-media/"+Uri.encode(item.optString("file_path"),"/"),preview)
                    else {
                        val icon=TextView(this).apply { text=if(kind=="video") "▶" else if(kind=="audio") "♫" else "PDF"; textSize=29f; gravity=Gravity.CENTER; setTextColor(Color.DKGRAY) }
                        tile.addView(icon,FrameLayout.LayoutParams(-1,-1))
                    }
                    grid.addView(tile)
                }
            }
        }
        space(content,12)
        button(content,"＋  CREATE A POST") { chooseUploadFile() }
        button(content,"MY MINISTRY & PAGE ACCOUNTS") { showMyPages() }
        button(content,"SIGN OUT") { AuthApi.signOut(this); showWelcome() }
    }

    private fun showMyPages() {
        base("Account", "Your channels", "MY PAGES", "Manage your ministry and public-figure profiles.")
        val pages = card(content)
        SupabaseApi.getWithError(this, "ministries?select=id,name,description,city,page_type,approved&owner_id=eq.${AuthApi.currentUserId(this)}&order=created_at.desc") { rows, error ->
            runOnUiThread {
                pages.removeAllViews()
                if (error != null) text(pages, "Could not load pages: $error", 12, signalRed, false)
                else if (rows.length() == 0) text(pages, "No pages created yet.", 13, muted, false)
                else for (i in 0 until rows.length()) {
                    val p = rows.getJSONObject(i)
                    tile(pages, p.optString("name"), p.optString("page_type").replace("_", " ").uppercase() + if (p.optBoolean("approved")) " • ACTIVE PAGE" else " • ACTIVE PAGE") { showPageManager(p) }
                    button(pages, "OPEN PAGE ACCOUNT & CONTENT STUDIO") { showPageManager(p) }
                    button(pages, "EDIT PAGE PROFILE") { editPageDialog(p) }
                }
            }
        }
        button(content, "＋ CREATE PAGE") { createPageDialog() }
        button(content, "BACK TO PROFILE") { showAccount() }
    }

    private fun showCreatorDirectory() {
        base("Account", "ELIX creator community", "DISCOVER CREATORS", "Find Christian creators, follow their accounts and explore everything they publish.")
        val list = card(content)
        text(list, "Loading creators…", 13, muted, false)
        SupabaseApi.getWithError(this, "profiles?select=id,display_name,created_at&order=created_at.desc&limit=200") { rows, error ->
            runOnUiThread {
                list.removeAllViews()
                if (error != null) text(list, "Creator directory could not load: $error", 12, signalRed, false)
                else if (rows.length() == 0) text(list, "No creator profiles found yet.", 13, muted, false)
                else for (i in 0 until rows.length()) {
                    val creator = rows.getJSONObject(i)
                    val id = creator.optString("id")
                    if (id == AuthApi.currentUserId(this)) continue
                    val name = creator.optString("display_name").ifBlank { "ELIX Creator" }
                    tile(list, name, "Open creator profile") { showCreatorProfile(id, name) }
                }
            }
        }
        button(content, "BACK TO MY ACCOUNT") { showAccount() }
    }

    private fun showCreatorProfile(creatorId: String, creatorName: String) {
        base("Account", "Creator profile", creatorName, "Follow this creator and browse their published Christian content.")
        val profile = card(content)
        text(profile, "Loading profile…", 13, muted, false)
        val follow = Button(this).apply { isAllCaps = false }
        if (AuthApi.isSignedIn(this) && creatorId != AuthApi.currentUserId(this)) content.addView(follow, fullWidth())
        fun refresh() {
            SupabaseApi.getWithError(this, "creator_follower_counts?select=follower_count&creator_id=eq.$creatorId&limit=1") { counts, countError ->
                SupabaseApi.getWithError(this, "creator_follows?select=follower_id&creator_id=eq.$creatorId") { followers, _ ->
                    SupabaseApi.getWithError(this, "creator_follows?select=creator_id&creator_id=eq.$creatorId&follower_id=eq.\${AuthApi.currentUserId(this)}&limit=1") { mine, mineError ->
                        runOnUiThread {
                            profile.removeAllViews()
                            text(profile, creatorName, 22, white, true)
                            val total = if (countError == null && counts.length() > 0) counts.getJSONObject(0).optLong("follower_count") else followers.length().toLong()
                            text(profile, "$total followers", 13, gold, true)
                            if (AuthApi.isSignedIn(this) && creatorId != AuthApi.currentUserId(this)) {
                                val following = mineError == null && mine.length() > 0
                                follow.text = if (following) "FOLLOWING · UNFOLLOW" else "＋ FOLLOW"
                                follow.setOnClickListener {
                                    if (following) SupabaseApi.deleteWithError(this, "creator_follows?follower_id=eq.\${AuthApi.currentUserId(this)}&creator_id=eq.$creatorId") { e -> runOnUiThread { info(if (e == null) "Unfollowed $creatorName." else "Could not unfollow: $e"); refresh() } }
                                    else SupabaseApi.postWithError(this, "creator_follows", JSONObject().put("follower_id", AuthApi.currentUserId(this)).put("creator_id", creatorId)) { e -> runOnUiThread { info(if (e == null) "You are now following $creatorName." else "Could not follow: $e"); refresh() } }
                                }
                            }
                        }
                    }
                }
            }
        }
        refresh()
        text(content, "PUBLISHED CONTENT", 16, white, true)
        val uploads = card(content)
        SupabaseApi.getWithError(this, "media_items?select=id,title,description,kind,media_type,file_path,status&uploaded_by=eq.$creatorId&status=eq.approved&order=created_at.desc&limit=100") { rows, error ->
            runOnUiThread {
                uploads.removeAllViews()
                if (error != null) text(uploads, "Could not load creator content: $error", 12, signalRed, false)
                else if (rows.length() == 0) text(uploads, "This creator has not published anything yet.", 13, muted, false)
                else for (i in 0 until rows.length()) {
                    val item = rows.getJSONObject(i)
                    text(uploads, item.optString("title"), 15, gold, true)
                    text(uploads, item.optString("media_type").uppercase() + " • " + item.optString("kind").uppercase(), 11, muted, false)
                    button(uploads, if (item.optString("media_type") == "audio") "▶ PLAY AUDIO" else "OPEN CONTENT") {
                        if (item.optString("media_type") == "audio") playAudio(item.optString("title"), item.optString("file_path")) else openMedia(item.optString("file_path"), mediaType)
                    }
                }
            }
        }
        button(content, "BACK TO CREATORS") { showCreatorDirectory() }
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
                            if (result.ok && AuthApi.isSignedIn(this)) { nav.visibility = View.VISIBLE; showHome() }
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
                setStroke(dp(1), Color.rgb(230, 230, 230))
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
                setColor(Color.rgb(245, 245, 245))
                cornerRadius = dp(14).toFloat()
                setStroke(dp(1), Color.rgb(230, 230, 230))
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
            background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(Color.rgb(0, 149, 246), Color.rgb(0, 110, 210))).apply {
                cornerRadius = dp(14).toFloat()
                setStroke(dp(1), Color.rgb(0, 149, 246))
            }
            setOnClickListener { action() }
        }
        parent.addView(b, LinearLayout.LayoutParams(-1, dp(52)))
    }

    private fun text(parent: LinearLayout, value: String, size: Int, color: Int, bold: Boolean) {
        val v = TextView(this).apply {
            text=value; textSize=size.toFloat(); setTextColor(color)
            typeface = Typeface.create(if (bold) "sans-serif-medium" else "sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
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