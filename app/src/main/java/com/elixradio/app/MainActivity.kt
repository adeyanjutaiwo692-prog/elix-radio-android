package com.elixradio.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

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
        listOf("Home", "Live", "Library", "Ministries", "Upcoming", "Account").forEach { label ->
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
        base("Library", "On-demand content", "ELIX MEDIA LIBRARY", "Sermons, worship, videos, podcasts and Christian resources.")
        if (AuthApi.isSignedIn(this)) {
            button(content, "＋  UPLOAD A FILE") { chooseUploadFile() }
            space(content, 12)
        } else {
            notice(content, "Sign in or create an account to upload audio, video, PDF or image files.")
        }
        text(content, "RECENT UPLOADS", 17, white, true)
        space(content, 8)
        val list = card(content)
        text(list, "Loading published media…", 14, muted, false)
        SupabaseApi.get("media_items?select=id,title,media_type,file_path,created_at&status=eq.approved&order=created_at.desc&limit=50") { rows ->
            runOnUiThread {
                list.removeAllViews()
                if (rows.length() == 0) text(list, "No files have been uploaded yet.", 14, muted, false)
                for (i in 0 until rows.length()) {
                    val item = rows.getJSONObject(i)
                    val title = item.optString("title", "ELIX media")
                    val kind = item.optString("media_type", "media").uppercase()
                    val path = item.optString("file_path")
                    text(list, title, 16, gold, true)
                    text(list, kind + " · Tap to open", 12, muted, false)
                    val open = Button(this).apply {
                        text = "OPEN FILE"
                        isAllCaps = false
                        setOnClickListener {
                            val url = BuildConfig.SUPABASE_URL.trimEnd('/') + "/storage/v1/object/public/elix-media/" + Uri.encode(path, "/")
                            try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                            catch (_: Exception) { info("Could not open this file.") }
                        }
                    }
                    list.addView(open, fullWidth())
                    space(list, 10)
                }
            }
        }
        notice(content, "By uploading, you confirm that you own the content or have permission to share it. Non-Christian or inappropriate content may be removed by ELIX administrators.")
    }

    private fun chooseUploadFile() {
        if (!AuthApi.isSignedIn(this)) {
            info("Please sign in first.")
            showAccount()
            return
        }
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("audio/*", "video/mp4", "video/quicktime", "application/pdf", "image/jpeg", "image/png", "image/webp"))
        }
        startActivityForResult(intent, uploadRequestCode)
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
                .setMessage("Your file will be published immediately for everyone to access.")
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
                    info("Uploading file…")
                    UploadApi.upload(this, uri, title) { result ->
                        runOnUiThread {
                            if (result.ok) {
                                info(result.message)
                                showLibrary()
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
            }
            dialog.show()
        }
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
        base("Account", "Your ELIX experience", "MY ACCOUNT", "Sign in or create your free ELIX listener account.")
        if (AuthApi.isSignedIn(this)) {
            text(content, "Signed in as " + AuthApi.currentName(this).ifBlank { "ELIX listener" }, 16, gold, true)
            text(content, "Account role: " + AuthApi.currentRole(this).replace('_', ' ').uppercase(), 12, muted, true)
            space(content, 12)
            if (AuthApi.currentRole(this) == "super_admin") {
                button(content, "⚙  OPEN DEVELOPER DASHBOARD") { showDeveloperDashboard() }
                space(content, 10)
            }
            val roleBeforeRefresh = AuthApi.currentRole(this)
            AuthApi.fetchRole(this) { role ->
                runOnUiThread {
                    if (role == "super_admin" && roleBeforeRefresh != role && !isFinishing) showAccount()
                }
            }
            button(content, "SIGN OUT") {
                AuthApi.signOut(this)
                showAccount()
            }
            return
        }
        button(content, "CREATE ACCOUNT") { showAuthDialog(true) }
        space(content, 10)
        button(content, "SIGN IN") { showAuthDialog(false) }
        space(content, 14)
        notice(content, "Use your email address and a password. If email confirmation is enabled, confirm your address before signing in.")
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
