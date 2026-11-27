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
    private val storyRequestCode = 4110
    private var selectedUploadType = "audio"
    private var selectedPageId: String? = null
    private var libraryType = "all"
    private var mediaPlayer: MediaPlayer? = null
    private val homeFeedItems = ArrayList<JSONObject>()
    private var homeFeedIndex = 0
    private var activeFeedMediaId = ""
    private var pendingInviteCreatorId = ""
    private var feedItemStartedAt = 0L
    private var homeVideo: VideoView? = null
    private var homeImage: ImageView? = null
    private var homeAudioPanel: LinearLayout? = null
    private var homeFeedTitle: TextView? = null
    private var homeFeedCategory: TextView? = null
    private var homeFeedPosition: TextView? = null
    private var homeFeedFrame: FrameLayout? = null
    private val homeVideoPlayers = ArrayList<VideoView>()
    private var activeHomeVideo: VideoView? = null
    private var homeVideosMuted = true
    private val homeVideoMediaPlayers = java.util.WeakHashMap<VideoView, MediaPlayer>()

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
        pendingInviteCreatorId = if (intent?.data?.scheme == "elix" && intent.data?.host == "invite") intent.data?.lastPathSegment.orEmpty() else ""
        if (AuthApi.isSignedIn(this)) {
            if (pendingInviteCreatorId.isNotBlank()) {
                val invited = pendingInviteCreatorId
                pendingInviteCreatorId = ""
                showCreatorProfile(invited, "ELIX member")
            } else showHome()
        } else showWelcome()
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
        button(content, "Log in") { val e=email.text.toString().trim(); val p=password.text.toString(); if(e.isBlank()||p.length<6) info("Enter your email and password.") else AuthApi.signIn(this,e,p) { result -> runOnUiThread { info(result.message); if(result.ok && AuthApi.isSignedIn(this)) {
                            nav.visibility=View.VISIBLE
                            if(pendingInviteCreatorId.isNotBlank()) {
                                val invited=pendingInviteCreatorId
                                pendingInviteCreatorId=""
                                showCreatorProfile(invited,"ELIX member")
                            } else showHome()
                        } } } }
        space(content, 12)
        val create = TextView(this).apply { text = "Create new account"; textSize = 15f; gravity = Gravity.CENTER; setTextColor(Color.rgb(0,149,246)); setPadding(0, dp(14), 0, dp(14)); setOnClickListener { showAuthDialog(true) } }
        content.addView(create, fullWidth())
        space(content, 20)
        text(content, "By continuing, you agree to the ELIX Terms and Christian Content Policy.", 11, muted, false)
    }

    private fun setNav(active: String) {
        nav.removeAllViews()
        listOf("Home", "Discover", "Ecosystem", "Account").forEach { label ->
            val symbol = when(label) { "Home" -> "⌂"; "Discover" -> "▶"; "Ecosystem" -> "✦"; else -> "◉" }
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
                        "Ecosystem" -> showExploreHub()
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
        pauseAllHomeVideos()
        homeVideoPlayers.clear()
        activeHomeVideo = null
        try { homeVideo?.stopPlayback() } catch (_: Exception) { }
        stopAudio()
        content.removeAllViews()
        content.setPadding(0, dp(4), 0, dp(18))
        setNav("Home")

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(12))
        }
        val logo = TextView(this).apply {
            text = "ELIX"
            textSize = 27f
            letterSpacing = -0.06f
            typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
            setTextColor(Color.rgb(20, 20, 20))
        }
        header.addView(logo, LinearLayout.LayoutParams(0, -2, 1f))
        val createPost = TextView(this).apply {
            text = "＋"
            textSize = 30f
            gravity = Gravity.CENTER
            setTextColor(white)
            setOnClickListener { if (AuthApi.isSignedIn(this@MainActivity)) showAccount() else showAuthDialog(false) }
        }
        header.addView(createPost, LinearLayout.LayoutParams(dp(42), dp(42)))
        val messages = TextView(this).apply {
            text = "♡"
            textSize = 27f
            gravity = Gravity.CENTER
            setTextColor(white)
            setOnClickListener { showCommunity() }
        }
        header.addView(messages, LinearLayout.LayoutParams(dp(42), dp(42)))
        content.addView(header, fullWidth())

        val storyScroll = android.widget.HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            clipToPadding = false
            setPadding(dp(12), dp(2), dp(12), dp(10))
        }
        val stories = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        storyScroll.addView(stories)
        content.addView(storyScroll, fullWidth())

        fun addStory(name: String, imageUrl: String, isOwn: Boolean = false, onOpen: (() -> Unit)? = null) {
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(5), dp(3), dp(5), dp(3))
                setOnClickListener { if (isOwn) { if (AuthApi.isSignedIn(this@MainActivity)) chooseStoryUpload() else showAuthDialog(false) } else onOpen?.invoke() }
            }
            val ring = FrameLayout(this).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.WHITE)
                    setStroke(dp(2), if (isOwn) Color.rgb(205,205,205) else Color.rgb(220,35,105))
                }
                setPadding(dp(3), dp(3), dp(3), dp(3))
            }
            val avatar = ImageView(this).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.rgb(230,230,230)) }
            }
            ring.addView(avatar, FrameLayout.LayoutParams(-1, -1))
            if (imageUrl.isNotBlank()) loadRemoteImage(imageUrl, avatar)
            if (isOwn) {
                val plus = TextView(this).apply {
                    text = "+"
                    textSize = 17f
                    gravity = Gravity.CENTER
                    setTextColor(Color.WHITE)
                    background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.rgb(0,149,246)); setStroke(dp(2), Color.WHITE) }
                }
                val p = FrameLayout.LayoutParams(dp(22), dp(22), Gravity.BOTTOM or Gravity.RIGHT)
                ring.addView(plus, p)
            }
            item.addView(ring, LinearLayout.LayoutParams(dp(66), dp(66)))
            val label = TextView(this).apply {
                text = name
                textSize = 10.5f
                setTextColor(white)
                gravity = Gravity.CENTER
                maxWidth = dp(76)
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                setPadding(0, dp(4), 0, 0)
            }
            item.addView(label)
            stories.addView(item, LinearLayout.LayoutParams(dp(76), -2))
        }
        addStory("Your story", "", true)
        SupabaseApi.getWithError(this, "media_stories?select=id,title,media_type,file_path,expires_at&expires_at=gt.now()&order=created_at.desc&limit=30") { storyRows, storyError ->
            runOnUiThread {
                if (storyError == null) for (i in 0 until storyRows.length()) {
                    val story = storyRows.getJSONObject(i)
                    val path = story.optString("file_path")
                    val type = story.optString("media_type")
                    val thumb = if (type == "image") BuildConfig.SUPABASE_URL.trimEnd('/') + "/storage/v1/object/public/elix-media/" + Uri.encode(path, "/") else ""
                    addStory(story.optString("title", "ELIX story"), thumb) { openStoryViewer(story) }
                }
            }
        }
        SupabaseApi.getWithError(this, "ministries?select=id,name,logo_url&order=name.asc&limit=30") { pages, error ->
            runOnUiThread {
                if (error == null) for (i in 0 until pages.length()) {
                    val p = pages.getJSONObject(i)
                    addStory(p.optString("name", "Ministry"), p.optString("logo_url"))
                }
            }
        }

        val divider = View(this).apply { setBackgroundColor(Color.rgb(235,235,235)) }
        content.addView(divider, LinearLayout.LayoutParams(-1, dp(1)))
        val feedHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(10))
        }
        val feedTitle = TextView(this).apply {
            text = "For you"
            textSize = 17f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(white)
        }
        feedHeader.addView(feedTitle, LinearLayout.LayoutParams(0, -2, 1f))
        val following = TextView(this).apply {
            text = "Following"
            textSize = 14f
            setTextColor(muted)
        }
        feedHeader.addView(following)
        content.addView(feedHeader, fullWidth())

        val feed = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(feed, fullWidth())
        SupabaseApi.getWithError(this, "media_items?select=id,title,kind,media_type,file_path,ministry_id,uploaded_by,ministries(name,logo_url)&status=eq.approved&order=created_at.desc&limit=40") { rows, error ->
            runOnUiThread {
                feed.removeAllViews()
                if (error != null) {
                    text(feed, "Your feed could not be loaded. " + error, 13, muted, false)
                    return@runOnUiThread
                }
                if (rows.length() == 0) {
                    text(feed, "No posts yet. Follow ministries and creators as they publish sermons, worship and updates.", 14, muted, false)
                    return@runOnUiThread
                }
                homeFeedItems.clear()
                for (i in 0 until rows.length()) homeFeedItems.add(rows.getJSONObject(i))
                for (i in 0 until rows.length()) {
                    val post = rows.getJSONObject(i)
                    val page = post.optJSONObject("ministries")
                    val creator = page?.optString("name").orEmpty().ifBlank { "ELIX creator" }
                    val card = LinearLayout(this@MainActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        setBackgroundColor(Color.WHITE)
                    }
                    val identity = LinearLayout(this@MainActivity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(dp(14), dp(10), dp(14), dp(10))
                    }
                    val avatar = ImageView(this@MainActivity).apply {
                        scaleType = ImageView.ScaleType.CENTER_CROP
                        background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.LTGRAY) }
                    }
                    identity.addView(avatar, LinearLayout.LayoutParams(dp(38), dp(38)))
                    if (page?.optString("logo_url").orEmpty().isNotBlank()) loadRemoteImage(page!!.optString("logo_url"), avatar)
                    val identityText = LinearLayout(this@MainActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(dp(10), 0, 0, 0)
                    }
                    text(identityText, creator, 14, white, true)
                    text(identityText, post.optString("kind").replace("_", " ").uppercase() + " · ELIX", 10, muted, false)
                    identity.addView(identityText, LinearLayout.LayoutParams(0, -2, 1f))
                    val more = TextView(this@MainActivity).apply { text = "•••"; textSize = 18f; setTextColor(white) }
                    identity.addView(more)
                    card.addView(identity, fullWidth())

                    val media = FrameLayout(this@MainActivity).apply { setBackgroundColor(Color.rgb(18,18,18)) }
                    val mediaType = post.optString("media_type", "video")
                    val filePath = post.optString("file_path")
                    val mediaUrl = BuildConfig.SUPABASE_URL.trimEnd('/') + "/storage/v1/object/public/elix-media/" + Uri.encode(filePath, "/")
                    when (mediaType) {
                        "image" -> {
                            val imageView = ImageView(this@MainActivity).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
                            media.addView(imageView, FrameLayout.LayoutParams(-1, dp(340)))
                            if (filePath.isNotBlank()) Thread {
                                try {
                                    val bitmap = URL(mediaUrl).openStream().use { BitmapFactory.decodeStream(it) }
                                    runOnUiThread { if (bitmap != null && imageView.parent != null) imageView.setImageBitmap(bitmap) }
                                } catch (_: Exception) { }
                            }.start()
                        }
                        "video" -> {
                            val player = VideoView(this@MainActivity)
                            val videoHeight = dp(340)
                            media.addView(player, FrameLayout.LayoutParams(-1, videoHeight, Gravity.CENTER))
                            homeVideoPlayers.add(player)
                            val mute = TextView(this@MainActivity).apply {
                                text = if (homeVideosMuted) "🔇" else "🔊"
                                textSize = 17f; gravity = Gravity.CENTER
                                setTextColor(Color.WHITE)
                                background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(0x88000000.toInt()) }
                                setOnClickListener {
                                    homeVideosMuted = !homeVideosMuted
                                    homeVideoMediaPlayers.values.forEach { mp -> try { mp.setVolume(if (homeVideosMuted) 0f else 1f, if (homeVideosMuted) 0f else 1f) } catch (_: Exception) { } }
                                    text = if (homeVideosMuted) "🔇" else "🔊"
                                }
                            }
                            media.addView(mute, FrameLayout.LayoutParams(dp(42), dp(42), Gravity.RIGHT or Gravity.TOP).apply { setMargins(0, dp(10), dp(10), 0) })
                            player.setOnClickListener { if (player.isPlaying) player.pause() else playOnlyVisibleHomeVideo(player) }
                            player.setOnLongClickListener { try { player.seekTo((player.currentPosition + 10000).coerceAtLeast(0)) } catch (_: Exception) { }; true }
                            if (filePath.isNotBlank()) {
                                player.setVideoURI(Uri.parse(mediaUrl))
                                player.setOnPreparedListener { mp ->
                                    mp.isLooping = true
                                    homeVideoMediaPlayers[player] = mp
                                    mp.setVolume(if (homeVideosMuted) 0f else 1f, if (homeVideosMuted) 0f else 1f)
                                    val vw = mp.videoWidth.coerceAtLeast(1); val vh = mp.videoHeight.coerceAtLeast(1)
                                    val screenWidth = resources.displayMetrics.widthPixels
                                    val targetHeight = (screenWidth.toFloat() * vh / vw).toInt().coerceIn(dp(220), dp(520))
                                    player.layoutParams = FrameLayout.LayoutParams(-1, targetHeight, Gravity.CENTER)
                                    playOnlyVisibleHomeVideo(player)
                                }
                                player.setOnErrorListener { _, _, _ -> true }
                            }
                        }
                        "audio" -> {
                            val audio = LinearLayout(this@MainActivity).apply {
                                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
                                setPadding(dp(20), dp(20), dp(20), dp(20))
                                background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(Color.rgb(8,25,58), Color.rgb(0,110,170)))
                            }
                            val icon = TextView(this@MainActivity).apply { text = "♫"; textSize = 52f; gravity = Gravity.CENTER; setTextColor(Color.WHITE) }
                            audio.addView(icon, LinearLayout.LayoutParams(-1, dp(120)))
                            val label = TextView(this@MainActivity).apply { text = "ELIX AUDIO"; textSize = 13f; gravity = Gravity.CENTER; setTextColor(Color.WHITE) }
                            audio.addView(label)
                            media.addView(audio, FrameLayout.LayoutParams(-1, dp(240)))
                            if (i == 0 && filePath.isNotBlank()) playAudio(post.optString("title"), filePath)
                        }
                        else -> {
                            val placeholder = TextView(this@MainActivity).apply { text = "ELIX MEDIA"; textSize = 24f; gravity = Gravity.CENTER; setTextColor(Color.WHITE) }
                            media.addView(placeholder, FrameLayout.LayoutParams(-1, dp(240)))
                        }
                    }
                    card.addView(media, fullWidth())

                    val actions = LinearLayout(this@MainActivity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(dp(14), dp(8), dp(14), dp(4))
                    }
                    fun action(label: String, click: () -> Unit): TextView {
                        return TextView(this@MainActivity).apply {
                            text = label
                            textSize = 24f
                            setTextColor(white)
                            setPadding(0, 0, dp(18), 0)
                            setOnClickListener { click() }
                        }
                    }
                    actions.addView(action("♡") {
                        if (!AuthApi.isSignedIn(this@MainActivity)) { showAuthDialog(false); return@action }
                        toggleLikeForItem(post)
                    })
                    actions.addView(action("▢") {
                        homeFeedIndex = homeFeedItems.indexOfFirst { it.optString("id") == post.optString("id") }.coerceAtLeast(0)
                        openCommentsForCurrentItem()
                    })
                    actions.addView(action("↗") {
                        homeFeedIndex = homeFeedItems.indexOfFirst { it.optString("id") == post.optString("id") }.coerceAtLeast(0)
                        shareCurrentFeedItem()
                    })
                    val follow = TextView(this@MainActivity).apply {
                        text = "＋ Follow"
                        textSize = 13f
                        typeface = Typeface.DEFAULT_BOLD
                        setTextColor(Color.rgb(0,149,246))
                        setOnClickListener {
                            homeFeedIndex = homeFeedItems.indexOfFirst { it.optString("id") == post.optString("id") }.coerceAtLeast(0)
                            followCurrentCreator()
                            text = "Following"
                        }
                    }
                    actions.addView(View(this@MainActivity), LinearLayout.LayoutParams(0, 1, 1f))
                    actions.addView(follow)
                    card.addView(actions, fullWidth())
                    text(card, post.optString("title", "ELIX post"), 14, white, true)
                    card.setPadding(0, 0, 0, dp(12))
                    val margin = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) }
                    feed.addView(card, margin)
                }
                val scrollParent = content.parent
                if (scrollParent is ScrollView) {
                    scrollParent.setOnScrollChangeListener { _, _, _, _, _ -> updateVisibleHomeVideo() }
                    content.post { updateVisibleHomeVideo() }
                }
            }
        }
    }

    private fun showExploreHub() {
        base("Ecosystem", "ONE HOME FOR CHRISTIAN LIFE", "The ELIX ecosystem", "Watch, listen, learn, connect and grow — all in one place.")

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(22), dp(20), dp(22))
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(7, 18, 43), Color.rgb(0, 91, 155), Color.rgb(14, 39, 75))).apply {
                cornerRadius = dp(22).toFloat()
            }
        }
        text(hero, "FAITH. TRUTH. INSPIRATION.", 10, Color.rgb(150, 213, 255), true)
        space(hero, 8)
        text(hero, "Your Christian digital home.", 24, Color.WHITE, true)
        space(hero, 5)
        text(hero, "A connected place for ministries, Christian media, spiritual growth and fellowship.", 13, Color.rgb(225, 235, 248), false)
        space(hero, 14)
        val exploreButton = TextView(this).apply {
            text = "▶  EXPLORE ELIX"
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(11), dp(16), dp(11))
            background = GradientDrawable().apply { setColor(Color.rgb(0, 149, 246)); cornerRadius = dp(24).toFloat() }
            setOnClickListener { showDiscover() }
        }
        hero.addView(exploreButton, LinearLayout.LayoutParams(-2, dp(42)))
        content.addView(hero, fullWidth())
        space(content, 22)

        text(content, "EXPLORE THE ELIX NETWORK", 11, gold, true)
        space(content, 5)
        text(content, "Eight connected experiences. One platform.", 19, white, true)
        text(content, "Choose where you want to go.", 13, muted, false)
        space(content, 14)

        fun feature(row: LinearLayout, symbol: String, title: String, detail: String, status: String, colors: IntArray, action: () -> Unit) {
            val tile = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.TOP
                setPadding(dp(13), dp(14), dp(13), dp(13))
                background = GradientDrawable(GradientDrawable.Orientation.TL_BR, colors).apply {
                    cornerRadius = dp(18).toFloat()
                }
                isClickable = true
                isFocusable = true
                setOnClickListener { action() }
            }
            val icon = TextView(this).apply {
                text = symbol
                textSize = 27f
                setTextColor(Color.WHITE)
            }
            tile.addView(icon, LinearLayout.LayoutParams(-1, dp(38)))
            space(tile, 6)
            text(tile, title, 16, Color.WHITE, true)
            space(tile, 3)
            text(tile, detail, 11, Color.rgb(235, 241, 250), false)
            val tag = TextView(this).apply {
                text = status.uppercase()
                textSize = 9f
                letterSpacing = 0.06f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                setPadding(dp(8), dp(5), dp(8), dp(5))
                background = GradientDrawable().apply { setColor(0x33000000); cornerRadius = dp(12).toFloat() }
            }
            val tagParams = LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(10) }
            tile.addView(tag, tagParams)
            val params = LinearLayout.LayoutParams(0, dp(174), 1f).apply {
                if (row.childCount > 0) marginStart = dp(8)
            }
            row.addView(tile, params)
        }

        fun featureRow(
            a: Triple<String, String, String>, aStatus: String, aColors: IntArray, aAction: () -> Unit,
            b: Triple<String, String, String>, bStatus: String, bColors: IntArray, bAction: () -> Unit
        ) {
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            content.addView(row, fullWidth())
            feature(row, a.first, a.second, a.third, aStatus, aColors, aAction)
            feature(row, b.first, b.second, b.third, bStatus, bColors, bAction)
            space(content, 9)
        }

        featureRow(
            Triple("▶", "ELIX Social", "Discover videos, follow creators and connect with ministries."), "Available",
            intArrayOf(Color.rgb(26, 64, 126), Color.rgb(11, 31, 68)), { showDiscover() },
            Triple("◉", "ELIX Live", "Find live audio and video broadcasts."), "Live hub",
            intArrayOf(Color.rgb(130, 37, 59), Color.rgb(65, 20, 48)), { showBroadcasts() }
        )
        featureRow(
            Triple("♫", "ELIX Radio", "Listen to sermons, gospel audio and programmes."), "Available",
            intArrayOf(Color.rgb(15, 112, 112), Color.rgb(8, 57, 72)), { showRadio() },
            Triple("▤", "ELIX Word", "Bible reading, devotionals and study plans."), "Coming soon",
            intArrayOf(Color.rgb(104, 75, 25), Color.rgb(58, 43, 20)), { showEcosystemPreview("ELIX Word", "A dedicated space for Scripture, daily devotionals, Bible reading plans, study notes and reminders.") }
        )
        featureRow(
            Triple("▣", "ELIX Library", "Explore Christian books and published media."), "Available",
            intArrayOf(Color.rgb(74, 59, 130), Color.rgb(36, 29, 77)), { showLibrary() },
            Triple("♡", "ELIX Community", "Prayer groups, testimonies and fellowship."), "Coming soon",
            intArrayOf(Color.rgb(19, 111, 87), Color.rgb(9, 61, 52)), { showEcosystemPreview("ELIX Community", "A safe place for prayer requests, testimonies, fellowship groups, Christian discussions and encouragement.") }
        )
        featureRow(
            Triple("▦", "ELIX Events", "Discover conferences, crusades and programmes."), "Preview",
            intArrayOf(Color.rgb(123, 68, 34), Color.rgb(70, 35, 24)), { showUpcoming() },
            Triple("✦", "ELIX Academy", "Bible courses and ministry development."), "Coming soon",
            intArrayOf(Color.rgb(35, 88, 119), Color.rgb(19, 46, 78)), { showEcosystemPreview("ELIX Academy", "A learning platform for Bible courses, discipleship, leadership development and ministry training.") }
        )

        val ministry = card(content)
        text(ministry, "ARE YOU A MINISTRY OR CREATOR?", 11, gold, true)
        space(ministry, 5)
        text(ministry, "Bring your ministry into ELIX.", 18, white, true)
        text(ministry, "Create a page, publish your messages and help your community discover your work.", 13, muted, false)
        space(ministry, 10)
        button(ministry, "EXPLORE MINISTRIES") { showHome() }
    }

    private fun showEcosystemPreview(section: String, description: String) {
        when (section) {
            "ELIX Word" -> { showWord(); return }
            "ELIX Community" -> { showCommunity(); return }
            "ELIX Academy" -> { showAcademy(); return }
        }
        base("Ecosystem", "ELIX ECOSYSTEM", section, description)
        val preview = card(content)
        text(preview, "COMING SOON", 11, gold, true)
        space(preview, 7)
        text(preview, "This experience is part of the ELIX roadmap.", 17, white, true)
        text(preview, "We are building it as an integrated feature of ELIX, with its own tools and content while sharing the same account.", 13, muted, false)
        button(content, "BACK TO ELIX ECOSYSTEM") { showExploreHub() }
    }

    private fun requireMember(action: () -> Unit) {
        if (!AuthApi.isSignedIn(this)) {
            info("Sign in to use this ELIX feature.")
            showAuthDialog(false)
        } else action()
    }

    private fun showWord() {
        base("ELIX Word", "SCRIPTURE & SPIRITUAL GROWTH", "Grow in the Word", "Bible reading plans, daily devotionals and personal study.")
        val devotional = card(content)
        text(devotional, "DAILY DEVOTIONAL", 11, gold, true)
        val devotionalBody = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        devotional.addView(devotionalBody, fullWidth())
        SupabaseApi.getWithError(this, "devotionals?select=id,title,scripture_reference,reflection,prayer,created_at&is_published=eq.true&order=created_at.desc&limit=1") { rows, error ->
            runOnUiThread {
                devotionalBody.removeAllViews()
                if (error != null) text(devotionalBody, "Devotional content is not available yet. Please try again later.", 13, muted, false)
                else if (rows.length() == 0) text(devotionalBody, "Your daily devotional will appear here when published by ELIX.", 13, muted, false)
                else {
                    val item = rows.getJSONObject(0)
                    text(devotionalBody, item.optString("title"), 19, white, true)
                    space(devotionalBody, 6)
                    text(devotionalBody, item.optString("scripture_reference"), 12, gold, true)
                    space(devotionalBody, 8)
                    text(devotionalBody, item.optString("reflection"), 14, muted, false)
                    if (item.optString("prayer").isNotBlank()) {
                        space(devotionalBody, 8)
                        text(devotionalBody, "PRAYER", 11, gold, true)
                        text(devotionalBody, item.optString("prayer"), 13, muted, false)
                    }
                }
            }
        }
        space(content, 8)
        text(content, "BIBLE READING PLANS", 16, white, true)
        val plans = card(content)
        SupabaseApi.getWithError(this, "reading_plans?select=id,title,description,duration_days&is_published=eq.true&order=created_at.desc") { rows, error ->
            runOnUiThread {
                plans.removeAllViews()
                if (error != null) text(plans, "Could not load reading plans: $error", 13, signalRed, false)
                else if (rows.length() == 0) text(plans, "Reading plans will appear here soon.", 13, muted, false)
                else for (i in 0 until rows.length()) {
                    val plan = rows.getJSONObject(i)
                    text(plans, plan.optString("title"), 16, gold, true)
                    text(plans, plan.optString("description"), 13, muted, false)
                    text(plans, plan.optInt("duration_days").toString() + " day plan", 11, muted, false)
                    button(plans, "OPEN READING PLAN") { openReadingPlan(plan.optString("id"), plan.optString("title")) }
                    space(plans, 8)
                }
            }
        }
        notice(content, "ELIX reading plans provide Scripture references and guided reflections. Use a Bible translation you have permission to access.")
    }

    private fun openReadingPlan(planId: String, title: String) {
        base("Reading plan", "ELIX WORD", title, "Follow each day's Scripture reading and mark your progress.")
        if (!AuthApi.isSignedIn(this)) notice(content, "Sign in to save your reading progress.")
        val days = card(content)
        SupabaseApi.getWithError(this, "reading_plan_days?select=id,day_number,scripture_reference,reflection&plan_id=eq.$planId&order=day_number.asc") { rows, error ->
            runOnUiThread {
                days.removeAllViews()
                if (error != null) text(days, "Could not load this plan: $error", 13, signalRed, false)
                else if (rows.length() == 0) text(days, "This plan has no daily readings yet.", 13, muted, false)
                else for (i in 0 until rows.length()) {
                    val day = rows.getJSONObject(i)
                    text(days, "DAY " + day.optInt("day_number"), 11, gold, true)
                    text(days, day.optString("scripture_reference"), 16, white, true)
                    if (day.optString("reflection").isNotBlank()) text(days, day.optString("reflection"), 13, muted, false)
                    if (AuthApi.isSignedIn(this)) button(days, "MARK DAY COMPLETE") { markReadingDay(planId, day.optInt("day_number")) }
                    space(days, 10)
                }
            }
        }
        button(content, "BACK TO ELIX WORD") { showWord() }
    }

    private fun markReadingDay(planId: String, dayNumber: Int) {
        requireMember {
            val body = JSONObject().put("user_id", AuthApi.currentUserId(this)).put("plan_id", planId).put("day_number", dayNumber)
            SupabaseApi.postWithError(this, "reading_plan_progress", body) { error ->
                runOnUiThread { info(if (error == null) "Reading progress saved." else "Could not save progress: $error") }
            }
        }
    }

    private fun showCommunity() {
        base("Community", "PRAYER, TESTIMONIES & FELLOWSHIP", "Grow together", "Share prayer needs, encourage others and find Christian fellowship.")
        val actions = card(content)
        text(actions, "YOUR VOICE MATTERS", 11, gold, true)
        space(actions, 7)
        button(actions, "＋  SHARE A PRAYER REQUEST") { composeCommunityPost("prayer") }
        button(actions, "＋  SHARE A TESTIMONY") { composeCommunityPost("testimony") }
        space(content, 8)
        text(content, "PRAYER WALL", 16, white, true)
        val prayers = card(content)
        SupabaseApi.getWithError(this, "prayer_requests?select=id,title,body,created_at,profiles(display_name)&visibility=eq.public&status=eq.published&order=created_at.desc&limit=50") { rows, error ->
            runOnUiThread {
                prayers.removeAllViews()
                if (error != null) text(prayers, "Prayer wall is not available yet. Apply the ELIX Community database update.", 13, muted, false)
                else if (rows.length() == 0) text(prayers, "No public prayer requests yet. You can be the first to share.", 13, muted, false)
                else for (i in 0 until rows.length()) {
                    val item = rows.getJSONObject(i)
                    text(prayers, item.optString("title", "Prayer request"), 15, gold, true)
                    text(prayers, item.optString("body"), 13, muted, false)
                    val profile = item.optJSONObject("profiles")
                    text(prayers, "Shared by " + (profile?.optString("display_name")?.takeIf { !it.isNullOrBlank() } ?: "ELIX member"), 10, muted, false)
                    if (AuthApi.isSignedIn(this)) button(prayers, "I'M PRAYING") { info("Thank you for standing with this person in prayer.") }
                    space(prayers, 10)
                }
            }
        }
        space(content, 8)
        text(content, "TESTIMONIES", 16, white, true)
        val testimonies = card(content)
        SupabaseApi.getWithError(this, "testimonies?select=id,title,body,created_at,profiles(display_name)&status=eq.published&order=created_at.desc&limit=40") { rows, error ->
            runOnUiThread {
                testimonies.removeAllViews()
                if (error != null) text(testimonies, "Testimonies are not available yet. Apply the ELIX Community database update.", 13, muted, false)
                else if (rows.length() == 0) text(testimonies, "Approved testimonies will appear here.", 13, muted, false)
                else for (i in 0 until rows.length()) {
                    val item = rows.getJSONObject(i)
                    text(testimonies, item.optString("title"), 15, gold, true)
                    text(testimonies, item.optString("body"), 13, muted, false)
                    space(testimonies, 8)
                }
            }
        }
        space(content, 8)
        text(content, "FELLOWSHIP GROUPS", 16, white, true)
        val groups = card(content)
        SupabaseApi.getWithError(this, "community_groups?select=id,name,description&is_active=eq.true&order=name.asc") { rows, error ->
            runOnUiThread {
                groups.removeAllViews()
                if (error != null) text(groups, "Groups are not available yet. Apply the ELIX Community database update.", 13, muted, false)
                else if (rows.length() == 0) text(groups, "Fellowship groups will appear here when created.", 13, muted, false)
                else for (i in 0 until rows.length()) {
                    val group = rows.getJSONObject(i)
                    text(groups, group.optString("name"), 15, gold, true)
                    text(groups, group.optString("description"), 12, muted, false)
                    button(groups, "JOIN GROUP") { joinCommunityGroup(group.optString("id")) }
                }
            }
        }
    }

    private fun composeCommunityPost(type: String) {
        requireMember {
            val titleInput = android.widget.EditText(this).apply { hint = if (type == "prayer") "Prayer request title" else "Testimony title"; setTextColor(white); setHintTextColor(muted) }
            val bodyInput = android.widget.EditText(this).apply { hint = "Write your message"; minLines = 4; setTextColor(white); setHintTextColor(muted) }
            val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(8), dp(20), 0); addView(titleInput); addView(bodyInput) }
            android.app.AlertDialog.Builder(this).setTitle(if (type == "prayer") "Share a prayer request" else "Share your testimony").setView(layout)
                .setPositiveButton("Submit") { _, _ ->
                    val title = titleInput.text.toString().trim()
                    val body = bodyInput.text.toString().trim()
                    if (title.isBlank() || body.isBlank()) info("Enter a title and message.")
                    else {
                        val table = if (type == "prayer") "prayer_requests" else "testimonies"
                        val data = JSONObject().put("user_id", AuthApi.currentUserId(this)).put("title", title).put("body", body)
                        if (type == "prayer") data.put("visibility", "public")
                        SupabaseApi.postWithError(this, table, data) { error ->
                            runOnUiThread { info(if (error == null) "Submitted. It will appear after review." else "Submission failed: $error"); if (error == null) showCommunity() }
                        }
                    }
                }.setNegativeButton("Cancel", null).show()
        }
    }

    private fun joinCommunityGroup(groupId: String) {
        requireMember {
            val data = JSONObject().put("group_id", groupId).put("user_id", AuthApi.currentUserId(this))
            SupabaseApi.postWithError(this, "community_group_members", data) { error ->
                runOnUiThread { info(if (error == null) "You joined this fellowship group." else "Could not join group: $error") }
            }
        }
    }

    private fun showAcademy() {
        base("Academy", "BIBLE & MINISTRY TRAINING", "ELIX Academy", "Learn Scripture, discipleship, leadership and practical ministry.")
        val intro = card(content)
        text(intro, "LEARN. GROW. SERVE.", 11, gold, true)
        space(intro, 7)
        text(intro, "Training for a life of faith and service.", 18, white, true)
        text(intro, "Explore available courses, study lessons and track your learning progress.", 13, muted, false)
        space(content, 8)
        text(content, "AVAILABLE COURSES", 16, white, true)
        val courses = card(content)
        SupabaseApi.getWithError(this, "academy_courses?select=id,title,description,instructor,difficulty,duration_label&is_published=eq.true&order=created_at.desc") { rows, error ->
            runOnUiThread {
                courses.removeAllViews()
                if (error != null) text(courses, "Academy catalogue is not available. Apply the ELIX Academy database update.", 13, muted, false)
                else if (rows.length() == 0) text(courses, "New courses are being prepared for ELIX Academy.", 13, muted, false)
                else for (i in 0 until rows.length()) {
                    val course = rows.getJSONObject(i)
                    text(courses, course.optString("title"), 16, gold, true)
                    text(courses, course.optString("description"), 13, muted, false)
                    text(courses, course.optString("difficulty", "All levels") + " • " + course.optString("duration_label"), 11, muted, false)
                    button(courses, "VIEW COURSE") { openAcademyCourse(course.optString("id"), course.optString("title")) }
                    space(courses, 8)
                }
            }
        }
        notice(content, "Course certificates will only be issued for courses with a verified completion process.")
    }

    private fun openAcademyCourse(courseId: String, title: String) {
        base("Course", "ELIX ACADEMY", title, "Course lessons and learning progress.")
        val enrollment = card(content)
        button(enrollment, "ENROL IN THIS COURSE") {
            requireMember {
                val data = JSONObject().put("user_id", AuthApi.currentUserId(this)).put("course_id", courseId)
                SupabaseApi.postWithError(this, "course_enrollments", data) { error ->
                    runOnUiThread { info(if (error == null) "You are enrolled." else "Could not enrol: $error") }
                }
            }
        }
        val lessons = card(content)
        SupabaseApi.getWithError(this, "academy_lessons?select=id,title,content,lesson_number&course_id=eq.$courseId&is_published=eq.true&order=lesson_number.asc") { rows, error ->
            runOnUiThread {
                lessons.removeAllViews()
                if (error != null) text(lessons, "Could not load lessons: $error", 13, signalRed, false)
                else if (rows.length() == 0) text(lessons, "Lessons for this course are being prepared.", 13, muted, false)
                else for (i in 0 until rows.length()) {
                    val lesson = rows.getJSONObject(i)
                    text(lessons, "LESSON " + lesson.optInt("lesson_number") + "  •  " + lesson.optString("title"), 15, gold, true)
                    button(lessons, "OPEN LESSON") { openAcademyLesson(courseId, lesson.optString("id"), lesson.optString("title"), lesson.optString("content")) }
                    space(lessons, 7)
                }
            }
        }
        button(content, "BACK TO ACADEMY") { showAcademy() }
    }

    private fun openAcademyLesson(courseId: String, lessonId: String, title: String, lessonContent: String) {
        base("Lesson", "ELIX ACADEMY", title, "Study at your own pace.")
        val lesson = card(content)
        text(lesson, lessonContent, 14, muted, false)
        button(content, "MARK LESSON COMPLETE") {
            requireMember {
                val data = JSONObject().put("user_id", AuthApi.currentUserId(this)).put("lesson_id", lessonId)
                SupabaseApi.postWithError(this, "lesson_progress", data) { error ->
                    runOnUiThread { info(if (error == null) "Lesson marked complete." else "Could not save progress: $error") }
                }
            }
        }
        button(content, "BACK TO COURSE") { openAcademyCourse(courseId, "Course") }
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
        val pageHeader = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(8), 0, dp(16)) }
        val pageAvatarFrame = FrameLayout(this)
        val pageAvatarFallback = TextView(this).apply {
            text = page.optString("name").take(1).uppercase()
            textSize = 28f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.rgb(130, 70, 160)) }
        }
        pageAvatarFrame.addView(pageAvatarFallback, FrameLayout.LayoutParams(-1, -1))
        val pageAvatar = ImageView(this).apply { scaleType = ImageView.ScaleType.CENTER_CROP; background = GradientDrawable().apply { shape = GradientDrawable.OVAL } }
        pageAvatarFrame.addView(pageAvatar, FrameLayout.LayoutParams(-1, -1))
        if (page.optString("logo_url").isNotBlank()) loadRemoteImage(page.optString("logo_url"), pageAvatar)
        pageHeader.addView(pageAvatarFrame, LinearLayout.LayoutParams(dp(84), dp(84)))
        val pageIdentity = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), 0, 0, 0) }
        text(pageIdentity, page.optString("name"), 19, white, true)
        text(pageIdentity, page.optString("page_type").replace("_", " ").uppercase(), 11, gold, true)
        text(pageIdentity, page.optString("city"), 12, muted, false)
        pageHeader.addView(pageIdentity, LinearLayout.LayoutParams(0, -2, 1f))
        pageHeader.setOnClickListener { chooseProfileImage(pageId) }
        content.addView(pageHeader, fullWidth())
        button(content, "CHANGE PAGE PROFILE PICTURE") { chooseProfileImage(pageId) }
        button(content, "EDIT PAGE PROFILE") { editPageDialog(page) }
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
        val request = if (pageId.isNullOrBlank()) profileImageRequestCode else pageImageRequestCode
        val gallery = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
        val camera = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
        val chooser = Intent.createChooser(gallery, "Choose profile picture")
        if (camera.resolveActivity(packageManager) != null) chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(camera))
        startActivityForResult(chooser, request)
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
        feedPlayButton = null
        val actionRail = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(2), dp(5), dp(2), dp(5))
            background = GradientDrawable().apply { setColor(Color.TRANSPARENT) }
        }
        fun railAction(symbol: String, label: String, action: () -> Unit): TextView {
            val wrap = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(0, dp(5), 0, dp(5))
            }
            val icon = TextView(this).apply {
                text = symbol
                gravity = Gravity.CENTER
                textSize = 31f
                setTextColor(Color.WHITE)
                typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                setShadowLayer(dp(3).toFloat(), 0f, dp(1).toFloat(), Color.BLACK)
                setOnClickListener { action() }
            }
            wrap.addView(icon, LinearLayout.LayoutParams(dp(54), dp(42)))
            val count = TextView(this).apply {
                text = label
                gravity = Gravity.CENTER
                textSize = 11f
                setTextColor(Color.WHITE)
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setShadowLayer(dp(3).toFloat(), 0f, dp(1).toFloat(), Color.BLACK)
                setOnClickListener { action() }
            }
            wrap.addView(count, LinearLayout.LayoutParams(dp(58), dp(20)))
            actionRail.addView(wrap, LinearLayout.LayoutParams(dp(60), -2))
            return count
        }
        val followAction = railAction("＋", "Follow") { followCurrentCreator() }
        val likeAction = railAction("♡", "0") {
            val item = homeFeedItems.getOrNull(homeFeedIndex)
            if (item != null) {
                if (!AuthApi.isSignedIn(this@MainActivity)) { showAuthDialog(false); return@railAction }
                val mediaId = item.optString("id")
                val userId = AuthApi.currentUserId(this@MainActivity)
                SupabaseApi.getWithError(this@MainActivity, "media_engagement_events?select=id&media_id=eq." + mediaId + "&user_id=eq." + userId + "&event_type=eq.like&limit=1") { rows, error ->
                    if (error != null) runOnUiThread { info("Like service unavailable: " + error) }
                    else if (rows.length() > 0) {
                        val eventId = rows.getJSONObject(0).optString("id")
                        SupabaseApi.deleteWithError(this@MainActivity, "media_engagement_events?id=eq." + eventId) { e ->
                            runOnUiThread { if (e != null) info("Could not remove like: " + e); refreshFeedActionCounts(item) }
                        }
                    } else {
                        val body = JSONObject().put("user_id", userId).put("media_id", mediaId).put("event_type", "like").put("watch_seconds", 0)
                        SupabaseApi.postWithError(this@MainActivity, "media_engagement_events", body) { saveError ->
                            runOnUiThread {
                                if (saveError != null) info("Like could not be saved: $saveError")
                                refreshFeedActionCounts(item)
                            }
                        }
                    }
                }
            }
        }
        val commentAction = railAction("▢", "0") { openCommentsForCurrentItem() }
        val shareAction = railAction("↗", "Share") { shareCurrentFeedItem() }
        homeFeedFrame?.tag = listOf(likeAction, commentAction, shareAction, followAction)
        val railParams = FrameLayout.LayoutParams(dp(64), -2, Gravity.RIGHT or Gravity.CENTER_VERTICAL)
        railParams.rightMargin = dp(5)
        frame.addView(actionRail, railParams)

        frame.addView(overlay, FrameLayout.LayoutParams(-1, -1))
        val discoverMute = TextView(this).apply {
            text = "🔇"; textSize = 18f; gravity = Gravity.CENTER; setTextColor(Color.WHITE)
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(0x88000000.toInt()) }
            setOnClickListener { discoverMuted = !discoverMuted; applyDiscoverMute(); text = if (discoverMuted) "🔇" else "🔊" }
        }
        frame.addView(discoverMute, FrameLayout.LayoutParams(dp(44), dp(44), Gravity.RIGHT or Gravity.TOP).apply { topMargin = dp(54); rightMargin = dp(14) })
        val detector = android.view.GestureDetector(this, object : android.view.GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: android.view.MotionEvent): Boolean = true
            override fun onSingleTapConfirmed(e: android.view.MotionEvent): Boolean { toggleFeedPlayback(); return true }
            override fun onLongPress(e: android.view.MotionEvent) { seekFeed(10000) }
            override fun onFling(e1: android.view.MotionEvent?, e2: android.view.MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (e1 == null) return false
                val delta = e2.y - e1.y
                if (kotlin.math.abs(delta) > dp(45) && kotlin.math.abs(velocityY) > dp(70)) {
                    moveFeed(if (delta < 0) 1 else -1)
                    return true
                }
                return false
            }
        })
        val touch = View.OnTouchListener { _, event -> detector.onTouchEvent(event) }
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


    private var sortCommentsOldest = false

    private fun openCommentsForCurrentItem() {
        val item = homeFeedItems.getOrNull(homeFeedIndex) ?: return
        val mediaId = item.optString("id")
        var reloadComments: (() -> Unit)? = null
        val dialog = android.app.Dialog(this)
        val sheet = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackground(GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadii = floatArrayOf(dp(24).toFloat(), dp(24).toFloat(), dp(24).toFloat(), dp(24).toFloat(), 0f, 0f, 0f, 0f)
            })
            setPadding(dp(16), dp(8), dp(16), dp(8))
        }
        val handleWrap = FrameLayout(this)
        val handle = View(this).apply { background = GradientDrawable().apply { setColor(Color.LTGRAY); cornerRadius = dp(4).toFloat() } }
        handleWrap.addView(handle, FrameLayout.LayoutParams(dp(38), dp(4), Gravity.CENTER))
        sheet.addView(handleWrap, LinearLayout.LayoutParams(-1, dp(22)))

        val heading = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(4), dp(2), dp(4), dp(10)) }
        val title = TextView(this).apply { text = "Comments"; textSize = 17f; typeface = Typeface.create("sans-serif", Typeface.BOLD); setTextColor(Color.rgb(22,22,22)) }
        heading.addView(title, LinearLayout.LayoutParams(0, -2, 1f))
        val sort = TextView(this).apply {
            text = "Recent  ⌄"; textSize = 12f; setTextColor(Color.DKGRAY); setPadding(dp(10), dp(8), dp(10), dp(8))
            setOnClickListener {
                android.widget.PopupMenu(this@MainActivity, this).apply {
                    menu.add("Recent"); menu.add("Oldest")
                    setOnMenuItemClickListener { selected ->
                        sortCommentsOldest = selected.title.toString() == "Oldest"
                        reloadComments?.invoke()
                        true
                    }
                }.show()
            }
        }
        heading.addView(sort)
        sheet.addView(heading, LinearLayout.LayoutParams(-1, -2))
        sheet.addView(View(this).apply { setBackgroundColor(Color.rgb(238,238,238)) }, LinearLayout.LayoutParams(-1, dp(1)))

        val scroll = ScrollView(this).apply { isFillViewport = false; overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS }
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(2), dp(8), dp(2), dp(16)) }
        scroll.addView(list)
        sheet.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        sheet.addView(View(this).apply { setBackgroundColor(Color.rgb(238,238,238)) }, LinearLayout.LayoutParams(-1, dp(1)))

        val composer = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(10), 0, dp(4)) }
        val avatar = TextView(this).apply {
            text = AuthApi.currentName(this@MainActivity).take(1).uppercase().ifBlank { "E" }
            textSize = 13f; gravity = Gravity.CENTER; setTextColor(Color.WHITE)
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.rgb(0,149,246)) }
        }
        composer.addView(avatar, LinearLayout.LayoutParams(dp(38), dp(38)))
        val input = android.widget.EditText(this).apply {
            hint = "Add a comment…"; textSize = 14f; setTextColor(Color.rgb(25,25,25)); setHintTextColor(Color.GRAY)
            minLines = 1; maxLines = 4; setPadding(dp(14), dp(9), dp(14), dp(9))
            background = GradientDrawable().apply { setColor(Color.rgb(246,246,246)); cornerRadius = dp(24).toFloat(); setStroke(dp(1), Color.rgb(232,232,232)) }
        }
        composer.addView(input, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(10); marginEnd = dp(8) })
        val post = TextView(this).apply { text = "Post"; textSize = 14f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.rgb(0,149,246)); gravity = Gravity.CENTER }
        post.setOnClickListener {
            if (!AuthApi.isSignedIn(this@MainActivity)) { showAuthDialog(false); return@setOnClickListener }
            val value = input.text.toString().trim()
            if (value.isBlank()) { input.error = "Write a comment first"; return@setOnClickListener }
            post.isEnabled = false
            val body = JSONObject().put("media_id", mediaId).put("user_id", AuthApi.currentUserId(this@MainActivity))
                .put("author_name", AuthApi.currentName(this@MainActivity).ifBlank { "ELIX member" }).put("comment_text", value)
            SupabaseApi.postWithError(this@MainActivity, "media_comments", body) { error ->
                runOnUiThread {
                    post.isEnabled = true
                    if (error == null) { input.setText(""); reloadComments?.invoke(); refreshFeedActionCounts(item) }
                    else info("Comment could not be posted: $error")
                }
            }
        }
        composer.addView(post, LinearLayout.LayoutParams(dp(48), dp(42)))
        sheet.addView(composer, LinearLayout.LayoutParams(-1, -2))

        reloadComments = {
            list.removeAllViews()
            val loading = TextView(this).apply { text = "Loading comments…"; textSize = 13f; setTextColor(Color.GRAY); gravity = Gravity.CENTER; setPadding(0, dp(36), 0, dp(36)) }
            list.addView(loading)
            val order = if (sortCommentsOldest) "asc" else "desc"
            SupabaseApi.getWithError(this, "media_comments?select=id,comment_text,author_name,created_at&media_id=eq.$mediaId&order=created_at.$order&limit=200") { rows, error ->
                runOnUiThread {
                    list.removeAllViews()
                    if (error != null) {
                        val failed = TextView(this).apply { text = "Comments couldn't load. Reopen to try again."; textSize = 13f; setTextColor(Color.GRAY); gravity = Gravity.CENTER; setPadding(0, dp(28), 0, dp(28)) }
                        list.addView(failed)
                    } else {
                        title.text = "Comments · ${rows.length()}"
                        if (rows.length() == 0) {
                            val empty = TextView(this).apply { text = "No comments yet\nBe the first to share your thoughts."; textSize = 14f; setTextColor(Color.GRAY); gravity = Gravity.CENTER; setPadding(0, dp(54), 0, dp(54)) }
                            list.addView(empty)
                        } else for (i in 0 until rows.length()) {
                            val row = rows.getJSONObject(i)
                            val line = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.TOP; setPadding(dp(2), dp(11), dp(2), dp(11)) }
                            val userAvatar = TextView(this).apply {
                                text = row.optString("author_name").take(1).uppercase().ifBlank { "E" }; textSize = 12f; gravity = Gravity.CENTER; setTextColor(Color.WHITE)
                                background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.rgb(120 + i % 50, 130 + i % 45, 150 + i % 40)) }
                            }
                            line.addView(userAvatar, LinearLayout.LayoutParams(dp(38), dp(38)))
                            val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(11), 0, dp(8), 0) }
                            val author = TextView(this).apply { text = row.optString("author_name").ifBlank { "ELIX member" }; textSize = 12f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.rgb(28,28,28)) }
                            val message = TextView(this).apply { text = row.optString("comment_text"); textSize = 13f; setTextColor(Color.rgb(45,45,45)); setPadding(0, dp(3), 0, dp(4)); setLineSpacing(dp(2).toFloat(), 1f) }
                            val reply = TextView(this).apply { text = "Reply"; textSize = 11f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.GRAY) }
                            body.addView(author); body.addView(message); body.addView(reply)
                            line.addView(body, LinearLayout.LayoutParams(0, -2, 1f))
                            val heart = TextView(this).apply { text = "♡"; textSize = 21f; setTextColor(Color.GRAY); gravity = Gravity.CENTER; setPadding(dp(8), 0, dp(3), 0) }
                            line.addView(heart, LinearLayout.LayoutParams(dp(34), dp(38)))
                            list.addView(line, fullWidth())
                        }
                    }
                }
            }
        }

        dialog.setContentView(sheet)
        dialog.show()
        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setLayout(-1, (resources.displayMetrics.heightPixels * 0.91f).toInt())
            setGravity(Gravity.BOTTOM)
            setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            attributes = attributes.apply { dimAmount = 0.45f }
        }
        reloadComments?.invoke()
    }

    private fun chooseStoryUpload() {
        if (!AuthApi.isSignedIn(this)) { showAuthDialog(false); return }
        val picker = Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*"))
        }
        startActivityForResult(Intent.createChooser(picker, "Choose a photo or video for your story"), storyRequestCode)
    }

    private fun publishStory(uri: Uri) {
        val mime = contentResolver.getType(uri).orEmpty()
        val type = if (mime.startsWith("video/")) "video" else if (mime.startsWith("image/")) "image" else ""
        if (type.isBlank()) { info("Choose an image or video."); return }
        val title = AuthApi.currentName(this).ifBlank { "ELIX member" } + "'s story"
        info("Uploading your story…")
        UploadApi.upload(this, uri, title, type, "story", null) { uploaded ->
            if (!uploaded.ok || uploaded.filePath.isNullOrBlank()) {
                runOnUiThread { info("Story upload failed: " + uploaded.message) }
                return@upload
            }
            val expiryFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }
            val body = JSONObject()
                .put("file_path", uploaded.filePath)
                .put("media_type", type)
                .put("title", title)
                .put("created_by", AuthApi.currentUserId(this))
                .put("expires_at", expiryFormat.format(java.util.Date(System.currentTimeMillis() + 24L * 60L * 60L * 1000L)))
            SupabaseApi.postWithError(this, "media_stories", body) { saveError ->
                runOnUiThread {
                    if (saveError == null) { info("Your story is live for 24 hours."); showHome() }
                    else info("Media uploaded, but story publishing failed: $saveError")
                }
            }
        }
    }

    private fun openStoryViewer(media: JSONObject) {
        val path = media.optString("file_path")
        val type = media.optString("media_type")
        if (path.isBlank()) return
        val url = BuildConfig.SUPABASE_URL.trimEnd('/') + "/storage/v1/object/public/elix-media/" + Uri.encode(path, "/")
        val viewer = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        if (type == "video") {
            val player = VideoView(this)
            viewer.addView(player, FrameLayout.LayoutParams(-1, -1))
            player.setVideoURI(Uri.parse(url))
            player.setOnPreparedListener { mp -> mp.isLooping = true; player.start() }
        } else {
            val image = ImageView(this).apply { scaleType = ImageView.ScaleType.FIT_CENTER }
            viewer.addView(image, FrameLayout.LayoutParams(-1, -1))
            Thread {
                try {
                    val bitmap = URL(url).openStream().use { BitmapFactory.decodeStream(it) }
                    runOnUiThread { if (bitmap != null) image.setImageBitmap(bitmap) }
                } catch (_: Exception) { }
            }.start()
        }
        val caption = TextView(this).apply {
            text = media.optString("title", "ELIX story")
            textSize = 15f; setTextColor(Color.WHITE); setPadding(dp(16), dp(24), dp(16), dp(16))
            background = GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, intArrayOf(0x99000000.toInt(), 0x00000000))
        }
        viewer.addView(caption, FrameLayout.LayoutParams(-1, dp(90), Gravity.BOTTOM))
        val dialog = android.app.Dialog(this)
        dialog.setContentView(viewer)
        dialog.show()
        dialog.window?.apply { setBackgroundDrawableResource(android.R.color.black); setLayout(-1, -1) }
    }

    private fun toggleLikeForItem(item: JSONObject) {
        if (!AuthApi.isSignedIn(this)) { showAuthDialog(false); return }
        val mediaId = item.optString("id")
        val userId = AuthApi.currentUserId(this)
        if (mediaId.isBlank() || userId.isBlank()) return
        SupabaseApi.getWithError(this, "media_engagement_events?select=id&media_id=eq.$mediaId&user_id=eq.$userId&event_type=eq.like&limit=1") { rows, error ->
            if (error != null) { runOnUiThread { info("Like service unavailable: $error") }; return@getWithError }
            if (rows.length() > 0) {
                val id = rows.getJSONObject(0).optString("id")
                SupabaseApi.deleteWithError(this, "media_engagement_events?id=eq.$id") { e ->
                    runOnUiThread { if (e != null) info("Could not remove like: $e") }
                }
            } else {
                val body = JSONObject().put("user_id", userId).put("media_id", mediaId).put("event_type", "like").put("watch_seconds", 0)
                SupabaseApi.postWithError(this, "media_engagement_events", body) { e ->
                    runOnUiThread { if (e != null) info("Like could not be saved: $e") }
                }
            }
        }
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
                runOnUiThread { info(if (err == null) "Unfollowed creator." else "Could not unfollow: $err"); if (err == null) refreshFeedActionCounts(item) }
            } else SupabaseApi.postWithError(this, "ministry_follows", JSONObject().put("user_id", userId).put("ministry_id", pageId)) { err ->
                runOnUiThread { info(if (err == null) "Following creator." else "Follow could not be saved: $err"); if (err == null) refreshFeedActionCounts(item) }
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
                player.setOnPreparedListener { mp -> mp.isLooping = true; mp.setVolume(if (discoverMuted) 0f else 1f, if (discoverMuted) 0f else 1f); mp.start() }
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

    private var discoverMuted = true

    private fun applyDiscoverMute() {
        try { mediaPlayer?.setVolume(if (discoverMuted) 0f else 1f, if (discoverMuted) 0f else 1f) } catch (_: Exception) { }
        try { homeVideoMediaPlayers[homeVideo]?.setVolume(if (discoverMuted) 0f else 1f, if (discoverMuted) 0f else 1f) } catch (_: Exception) { }
    }

    private fun pauseAllHomeVideos() {
        homeVideoPlayers.forEach { player -> try { if (player.isPlaying) player.pause() } catch (_: Exception) { } }
        activeHomeVideo = null
    }

    private fun playOnlyVisibleHomeVideo(candidate: VideoView) {
        val rect = android.graphics.Rect()
        if (!candidate.getGlobalVisibleRect(rect) || rect.height() < dp(100)) return
        homeVideoPlayers.forEach { player ->
            if (player !== candidate) try { if (player.isPlaying) player.pause() } catch (_: Exception) { }
        }
        try {
            if (!candidate.isPlaying) candidate.start()
            activeHomeVideo = candidate
        } catch (_: Exception) { }
    }

    private fun updateVisibleHomeVideo() {
        val candidate = homeVideoPlayers.firstOrNull { player ->
            val rect = android.graphics.Rect()
            player.getGlobalVisibleRect(rect) && rect.height() >= dp(180)
        }
        if (candidate == null) pauseAllHomeVideos() else playOnlyVisibleHomeVideo(candidate)
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
        val followView = views.getOrNull(3) as? TextView
        SupabaseApi.getWithError(this, "media_like_counts?select=like_count&media_id=eq." + mediaId) { rows, _ -> runOnUiThread { if (homeFeedItems.getOrNull(homeFeedIndex) === item) likeView?.text = if (rows.length() > 0) rows.getJSONObject(0).optString("like_count", "0") else "0" } }
        SupabaseApi.getWithError(this, "media_comments?select=id&media_id=eq." + mediaId + "&limit=1000") { rows, _ -> runOnUiThread { if (homeFeedItems.getOrNull(homeFeedIndex) === item) commentView?.text = rows.length().toString() } }
        SupabaseApi.getWithError(this, "media_share_counts?select=share_count&media_id=eq." + mediaId) { rows, _ -> runOnUiThread { if (homeFeedItems.getOrNull(homeFeedIndex) === item) shareView?.text = if (rows.length() > 0) rows.getJSONObject(0).optString("share_count", "0") else "0" } }
        val pageId = item.optString("ministry_id")
        val userId = AuthApi.currentUserId(this)
        if (followView != null && pageId.isNotBlank() && userId.isNotBlank()) {
            SupabaseApi.getWithError(this, "ministry_follows?select=user_id&user_id=eq.$userId&ministry_id=eq.$pageId&limit=1") { rows, _ ->
                runOnUiThread { if (homeFeedItems.getOrNull(homeFeedIndex) === item) followView.text = if (rows.length() > 0) "Following" else "Follow" }
            }
        } else followView?.text = "Follow"
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
                            if (mediaType == "audio") playAudio(item.optString("title", "ELIX Radio"), path) else openMedia(path, mediaType)
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
            android.app.AlertDialog.Builder(this).setTitle("Publish under which account?").setItems(labels.toTypedArray()){_,idx -> selectedPageId=ids[idx]; android.app.AlertDialog.Builder(this).setTitle("Choose media type").setItems(arrayOf("Audio","Video","Images","Book (PDF)")){_,which -> selectedUploadType=arrayOf("audio","video","image","pdf")[which]; val type=if(selectedUploadType=="audio")"audio/*" else if(selectedUploadType=="video")"video/*" else if(selectedUploadType=="pdf")"application/pdf" else "image/*"; val gallery=Intent(Intent.ACTION_GET_CONTENT).apply{addCategory(Intent.CATEGORY_OPENABLE);this.type=type}; val camera=when(selectedUploadType){"video"->Intent(android.provider.MediaStore.ACTION_VIDEO_CAPTURE);"image"->Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);else->null}; val chooser=Intent.createChooser(gallery,"Choose from gallery or camera"); if(camera!=null && camera.resolveActivity(packageManager)!=null) chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS,arrayOf(camera)); startActivityForResult(chooser,uploadRequestCode)}.show()}.show()
        } }
    }
    @Deprecated("Use platform activity result APIs")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == storyRequestCode && resultCode == RESULT_OK) {
            val storyUri = data?.data
            if (storyUri != null) publishStory(storyUri)
            else info("Could not read the selected story file.")
            return
        }
        if ((requestCode == profileImageRequestCode || requestCode == pageImageRequestCode) && resultCode == RESULT_OK) {
            var uri = data?.data
            if (uri == null) {
                val bitmap = data?.extras?.get("data") as? android.graphics.Bitmap ?: return
                uri = try { Uri.parse(android.provider.MediaStore.Images.Media.insertImage(contentResolver, bitmap, "ELIX profile", null)) } catch (_: Exception) { null }
            }
            val selectedUri = uri ?: return
            val pageId = if (requestCode == pageImageRequestCode) imageTargetPageId else null
            UploadApi.uploadProfileImage(this, selectedUri, AuthApi.currentUserId(this)) { result ->
                runOnUiThread {
                    if (!result.first) { info("Picture upload failed: " + result.second); return@runOnUiThread }
                    val savePicture: (String?) -> Unit = { error ->
                        runOnUiThread {
                            info(if (error == null) "Profile picture updated successfully." else "Picture saved to storage but could not be linked to the account: $error")
                            if (error == null) {
                                if (pageId.isNullOrBlank()) showAccount() else showMyPages()
                            }
                        }
                    }
                    if (pageId.isNullOrBlank()) {
                        SupabaseApi.patchWithError(this, "profiles?id=eq.${AuthApi.currentUserId(this)}", JSONObject().put("avatar_url", result.second), savePicture)
                    } else {
                        SupabaseApi.postWithError(this, "rpc/set_ministry_logo", JSONObject().put("target_ministry", pageId).put("new_logo_url", result.second), savePicture)
                    }
                }
            }
            return
        }
        if (requestCode == uploadRequestCode && resultCode == RESULT_OK) {
            var uri = data?.data
            if (uri == null && selectedUploadType == "image") {
                val bitmap = data?.extras?.get("data") as? android.graphics.Bitmap ?: return
                uri = try { Uri.parse(android.provider.MediaStore.Images.Media.insertImage(contentResolver, bitmap, "ELIX upload", null)) } catch (_: Exception) { null }
            }
            val selectedUri = uri ?: return
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
                            UploadApi.upload(this, selectedUri, title, selectedUploadType, categoryKeys[index], selectedPageId) { result ->
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
        val stats = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; weightSum = 4f }
        fun stat(label: String): TextView {
            val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER }
            val value = TextView(this).apply { text = "…"; textSize = 19f; setTextColor(Color.BLACK); setTypeface(null, Typeface.BOLD) }
            val caption = TextView(this).apply { text = label; textSize = 12f; setTextColor(Color.DKGRAY) }
            col.addView(value); col.addView(caption); stats.addView(col, LinearLayout.LayoutParams(0,-2,1f))
            return value
        }
        val postCount = stat("posts"); val followerCount = stat("followers"); val followingCount = stat("following"); val likesCount = stat("likes")
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
                    SupabaseApi.getWithError(this,"media_engagement_events?select=id,media_items!inner(uploaded_by)&event_type=eq.like&media_items.uploaded_by=eq.$userId&limit=1000") { likes, _ ->
                        runOnUiThread {
                            postCount.text=posts.length().toString()
                            followerCount.text=if(followers.length()>0) followers.getJSONObject(0).optLong("follower_count").toString() else "0"
                            followingCount.text=following.length().toString()
                            likesCount.text=likes.length().toString()
                        }
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
        SupabaseApi.getWithError(this, "ministries?select=id,name,description,city,page_type,approved,logo_url&owner_id=eq.${AuthApi.currentUserId(this)}&order=created_at.desc") { rows, error ->
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
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val avatarFrame = FrameLayout(this)
        val fallback = TextView(this).apply {
            text = creatorName.take(1).uppercase()
            textSize = 28f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.rgb(130, 70, 160)) }
        }
        avatarFrame.addView(fallback, FrameLayout.LayoutParams(-1, -1))
        val avatar = ImageView(this).apply { scaleType = ImageView.ScaleType.CENTER_CROP; background = GradientDrawable().apply { shape = GradientDrawable.OVAL } }
        avatarFrame.addView(avatar, FrameLayout.LayoutParams(-1, -1))
        header.addView(avatarFrame, LinearLayout.LayoutParams(dp(76), dp(76)))
        val identity = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), 0, 0, 0) }
        text(identity, creatorName, 20, white, true)
        text(identity, "ELIX CREATOR", 11, gold, true)
        header.addView(identity, LinearLayout.LayoutParams(0, -2, 1f))
        profile.addView(header, fullWidth())
        SupabaseApi.getWithError(this, "profiles?select=avatar_url,display_name&id=eq.$creatorId&limit=1") { rows, _ ->
            if (rows.length() > 0) {
                val url = rows.getJSONObject(0).optString("avatar_url")
                if (url.isNotBlank()) loadRemoteImage(url, avatar)
            }
        }
        val follow = Button(this).apply { isAllCaps = false }
        if (AuthApi.isSignedIn(this) && creatorId != AuthApi.currentUserId(this)) content.addView(follow, fullWidth())
        val share = Button(this).apply {
            text = "SHARE CREATOR PROFILE"
            isAllCaps = false
            setOnClickListener {
                val invite = "elix://invite/$creatorId"
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "Discover $creatorName on ELIX Christian Media Network: $invite")
                }
                startActivity(Intent.createChooser(send, "Share ELIX creator"))
            }
        }
        content.addView(share, fullWidth())
        fun refresh() {
            SupabaseApi.getWithError(this, "creator_follower_counts?select=follower_count&creator_id=eq.$creatorId&limit=1") { counts, countError ->
                SupabaseApi.getWithError(this, "creator_follows?select=follower_id&creator_id=eq.$creatorId") { followers, _ ->
                    val currentUser = AuthApi.currentUserId(this)
                    if (currentUser.isBlank() || creatorId == currentUser) {
                        runOnUiThread {
                            val total = if (countError == null && counts.length() > 0) counts.getJSONObject(0).optLong("follower_count") else followers.length().toLong()
                            text(profile, "$total followers", 13, gold, true)
                        }
                    } else {
                        SupabaseApi.getWithError(this, "creator_follows?select=creator_id&creator_id=eq.$creatorId&follower_id=eq.$currentUser&limit=1") { mine, mineError ->
                            runOnUiThread {
                                val total = if (countError == null && counts.length() > 0) counts.getJSONObject(0).optLong("follower_count") else followers.length().toLong()
                                text(profile, "$total followers", 13, gold, true)
                                val following = mineError == null && mine.length() > 0
                                follow.text = if (following) "FOLLOWING · UNFOLLOW" else "＋ FOLLOW"
                                follow.setOnClickListener {
                                    if (following) SupabaseApi.deleteWithError(this, "creator_follows?follower_id=eq.$currentUser&creator_id=eq.$creatorId") { e ->
                                        runOnUiThread { info(if (e == null) "Unfollowed $creatorName." else "Could not unfollow: $e"); refresh() }
                                    } else SupabaseApi.postWithError(this, "creator_follows", JSONObject().put("follower_id", currentUser).put("creator_id", creatorId)) { e ->
                                        runOnUiThread { info(if (e == null) "You are now following $creatorName." else "Could not follow: $e"); refresh() }
                                    }
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
        SupabaseApi.getWithError(this, "media_items?select=id,title,description,kind,media_type,file_path,thumbnail_url,status&uploaded_by=eq.$creatorId&status=eq.approved&order=created_at.desc&limit=100") { rows, error ->
            runOnUiThread {
                uploads.removeAllViews()
                if (error != null) text(uploads, "Could not load creator content: $error", 12, signalRed, false)
                else if (rows.length() == 0) text(uploads, "This creator has not published anything yet.", 13, muted, false)
                else for (i in 0 until rows.length()) {
                    val item = rows.getJSONObject(i)
                    text(uploads, item.optString("title"), 15, gold, true)
                    text(uploads, item.optString("media_type").uppercase() + " • " + item.optString("kind").uppercase(), 11, muted, false)
                    val mediaType = item.optString("media_type")
                    button(uploads, if (mediaType == "audio") "▶ PLAY AUDIO" else "OPEN CONTENT") {
                        if (mediaType == "audio") playAudio(item.optString("title"), item.optString("file_path"))
                        else openMedia(item.optString("file_path"), mediaType)
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
                            if (result.ok && AuthApi.isSignedIn(this)) {
                                nav.visibility = View.VISIBLE
                                if (pendingInviteCreatorId.isNotBlank()) {
                                    val invited = pendingInviteCreatorId
                                    pendingInviteCreatorId = ""
                                    showCreatorProfile(invited, "ELIX member")
                                } else showHome()
                            }
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