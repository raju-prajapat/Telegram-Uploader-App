package com.telegramuploader

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Space
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import java.net.HttpURLConnection
import java.net.URL

class SettingsActivity : Activity() {

    private lateinit var security: SecurityManager
    private lateinit var root: LinearLayout

    private val BG = "#080D1A"
    private val CARD = "#111827"
    private val CARD2 = "#0D1726"
    private val WHITE = "#F8FAFC"
    private val MUTED = "#94A3B8"
    private val PURPLE = "#7657FF"
    private val GREEN = "#22C55E"
    private val RED = "#EF4444"
    private val CYAN = "#29D3FF"
    private val BORDER = "#263247"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor =
            Color.parseColor("#050A12")

        window.navigationBarColor =
            Color.parseColor("#050A12")

        security = SecurityManager(this)

        showGate()
    }

    private fun showGate() {

        if (security.hasPassword()) {
            showUnlockScreen()
        } else {
            showCreatePasswordScreen()
        }
    }

    private fun showCreatePasswordScreen() {

        root = baseLayout()

        addHeader(
            "Secure Settings",
            "Create a password to protect your Telegram configuration."
        )

        val card = section()

        card.addView(
            label(
                "Create Password"
            )
        )

        val password =
            passwordInput(
                "Enter password"
            )

        val confirm =
            passwordInput(
                "Confirm password"
            )

        card.addView(password)

        addGap(card, 10)

        card.addView(confirm)

        addGap(card, 14)

        val create =
            primaryButton(
                "🔒  CREATE PASSWORD"
            )

        card.addView(create)

        create.setOnClickListener {

            val first =
                password.text.toString()

            val second =
                confirm.text.toString()

            if (first.length < 4) {

                message(
                    "Password must contain at least 4 characters."
                )

                return@setOnClickListener
            }

            if (first != second) {

                message(
                    "Passwords do not match."
                )

                return@setOnClickListener
            }

            if (
                security.setupPassword(first)
            ) {

                showConfigurationScreen()

            } else {

                message(
                    "Password could not be created."
                )
            }
        }

        root.addView(card)

        setContentView(
            wrap(root)
        )
    }

    private fun showUnlockScreen() {

        root = baseLayout()

        addHeader(
            "Telegram Settings",
            "Password required to access protected configuration."
        )

        val card = section()

        card.addView(
            label(
                "Enter Settings Password"
            )
        )

        val password =
            passwordInput(
                "Password"
            )

        card.addView(password)

        addGap(card, 14)

        val unlock =
            primaryButton(
                "🔓  UNLOCK SETTINGS"
            )

        card.addView(unlock)

        unlock.setOnClickListener {

            unlock.text = "⏳  CHECKING…"
            unlock.isEnabled = false

            val valid =
                security.verifyPassword(
                    password.text.toString()
                )

            if (valid) {

                showConfigurationScreen()

            } else {

                unlock.text =
                    "🔓  UNLOCK SETTINGS"

                unlock.isEnabled = true

                password.text.clear()

                message(
                    "Incorrect password."
                )
            }
        }

        addGap(card, 10)

        val note =
            text(
                "Your Bot Token and Chat ID are protected and are not shown on the main screen.",
                12f,
                MUTED
            )

        card.addView(note)

        addGap(card, 12)

        val forgot =
            secondaryButton(
                "FORGOT PASSWORD?"
            )

        card.addView(forgot)

        forgot.setOnClickListener {
            showForgotPasswordConfirmation()
        }

        root.addView(card)

        setContentView(
            wrap(root)
        )
    }

    private fun showConfigurationScreen() {

        root = baseLayout()

        addHeader(
            "Telegram Settings",
            "Manage your protected Bot Token and destination Chat ID."
        )

        val card = section()

        val token =
            input(
                "Bot Token"
            )

        val chat =
            input(
                "Chat ID / Channel ID"
            )

        val existingToken =
            security.getTelegramToken()

        val existingChat =
            security.getTelegramChatId()

        token.setText(existingToken)
        chat.setText(existingChat)

        card.addView(
            label(
                "Bot Token"
            )
        )

        card.addView(token)

        addGap(card, 10)

        card.addView(
            label(
                "Chat ID / Channel ID"
            )
        )

        card.addView(chat)

        addGap(card, 14)

        val status =
            text(
                "Ready",
                14f,
                MUTED
            )

        status.setPadding(
            0,
            dp(4),
            0,
            dp(10)
        )

        card.addView(status)

        val save =
            primaryButton(
                "✓  SAVE & TEST CONNECTION"
            )

        card.addView(save)

        save.setOnClickListener {

            val botToken =
                normalizeToken(
                    token.text.toString()
                )

            val chatId =
                chat.text.toString().trim()

            if (botToken.isEmpty()) {

                status.text =
                    "Bot Token is required"

                status.setTextColor(
                    Color.parseColor(RED)
                )

                return@setOnClickListener
            }

            if (chatId.isEmpty()) {

                status.text =
                    "Chat ID is required"

                status.setTextColor(
                    Color.parseColor(RED)
                )

                return@setOnClickListener
            }

            save.isEnabled = false
            save.text = "⏳  TESTING CONNECTION…"

            status.text =
                "Connecting to Telegram…"

            status.setTextColor(
                Color.parseColor(CYAN)
            )

            Thread {

                val bot =
                    testBot(botToken)

                val destination =
                    if (bot.first) {
                        testChat(
                            botToken,
                            chatId
                        )
                    } else {
                        Pair(
                            false,
                            bot.second
                        )
                    }

                runOnUiThread {

                    save.isEnabled = true
                    save.text =
                        "✓  SAVE & TEST CONNECTION"

                    if (
                        bot.first &&
                        destination.first
                    ) {

                        security.saveTelegramConfig(
                            botToken,
                            chatId
                        )

                        status.text =
                            "✓ Connected successfully"

                        status.setTextColor(
                            Color.parseColor(GREEN)
                        )

                        Toast.makeText(
                            this,
                            "Telegram settings saved",
                            Toast.LENGTH_SHORT
                        ).show()

                    } else {

                        status.text =
                            "Connection failed: ${destination.second}"

                        status.setTextColor(
                            Color.parseColor(RED)
                        )
                    }
                }

            }.start()
        }

        root.addView(card)

        addGap(root, 14)

        addAutomationSection(root)

        addGap(root, 14)

        val securityCard =
            section()

        securityCard.addView(
            text(
                "Security",
                18f,
                WHITE
            ).apply {
                setTypeface(null, 1)
            }
        )

        addGap(
            securityCard,
            6
        )

        securityCard.addView(
            text(
                "Your Telegram credentials are stored encrypted on this device.",
                13f,
                MUTED
            )
        )

        addGap(
            securityCard,
            12
        )

        val changePassword =
            secondaryButton(
                "🔑  CHANGE PASSWORD"
            )

        securityCard.addView(
            changePassword
        )

        changePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        root.addView(
            securityCard
        )

        addGap(
            root,
            14
        )

        val close =
            secondaryButton(
                "←  BACK TO UPLOADER"
            )

        root.addView(close)

        close.setOnClickListener {
            finish()
        }

        setContentView(
            wrap(root)
        )
    }

    private fun showForgotPasswordConfirmation() {

        val dialog =
            android.app.AlertDialog.Builder(this)
                .setTitle("Reset Protected Settings?")
                .setMessage(
                    "This will permanently delete your Bot Token, " +
                            "Channel/Group ID, password and encrypted Telegram credentials. " +
                            "Pending screenshot uploads will also be cleared. " +
                            "You will then create a new password and enter Telegram settings again."
                )
                .setNegativeButton(
                    "CANCEL",
                    null
                )
                .setPositiveButton(
                    "RESET EVERYTHING"
                ) { _, _ ->

                    val success =
                        security.resetAllCredentials()

                    if (success) {

                        getSharedPreferences(
                            "automation_settings",
                            MODE_PRIVATE
                        )
                            .edit()
                            .clear()
                            .apply()

                        getSharedPreferences(
                            "screenshot_upload_queue",
                            MODE_PRIVATE
                        )
                            .edit()
                            .clear()
                            .apply()

                        stopService(
                            Intent(
                                this,
                                ScreenshotUploadService::class.java
                            )
                        )

                        Toast.makeText(
                            this,
                            "All protected settings deleted.",
                            Toast.LENGTH_SHORT
                        ).show()

                        showCreatePasswordScreen()

                    } else {

                        Toast.makeText(
                            this,
                            "Reset failed. Please try again.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                .create()

        dialog.setOnShowListener {
            dialog.getButton(
                android.app.AlertDialog.BUTTON_POSITIVE
            )?.setTextColor(
                Color.parseColor(RED)
            )
        }

        dialog.show()
    }

    private fun showChangePasswordDialog() {

        val dialog =
            android.app.Dialog(this)

        val box =
            LinearLayout(this)

        box.orientation =
            LinearLayout.VERTICAL

        box.setPadding(
            dp(22),
            dp(22),
            dp(22),
            dp(22)
        )

        box.setBackgroundColor(
            Color.parseColor(CARD)
        )

        val title =
            text(
                "Change Password",
                19f,
                WHITE
            )

        title.setTypeface(null, 1)

        box.addView(title)

        addGap(box, 12)

        val oldPassword =
            passwordInput(
                "Current password"
            )

        val newPassword =
            passwordInput(
                "New password"
            )

        val confirm =
            passwordInput(
                "Confirm new password"
            )

        box.addView(oldPassword)

        addGap(box, 8)

        box.addView(newPassword)

        addGap(box, 8)

        box.addView(confirm)

        addGap(box, 14)

        val save =
            primaryButton(
                "SAVE NEW PASSWORD"
            )

        box.addView(save)

        save.setOnClickListener {

            val valid =
                security.verifyPassword(
                    oldPassword.text.toString()
                )

            if (!valid) {

                message(
                    "Current password is incorrect."
                )

                return@setOnClickListener
            }

            val newValue =
                newPassword.text.toString()

            if (newValue.length < 4) {

                message(
                    "New password must contain at least 4 characters."
                )

                return@setOnClickListener
            }

            if (
                newValue !=
                confirm.text.toString()
            ) {

                message(
                    "New passwords do not match."
                )

                return@setOnClickListener
            }

            security.setupPassword(
                newValue
            )

            dialog.dismiss()

            Toast.makeText(
                this,
                "Password changed successfully",
                Toast.LENGTH_SHORT
            ).show()
        }

        dialog.setContentView(box)

        val window =
            dialog.window

        window?.setBackgroundDrawable(
            rounded(
                CARD,
                20,
                BORDER
            )
        )

        window?.setLayout(
            dp(340),
            android.view.WindowManager.LayoutParams.WRAP_CONTENT
        )

        dialog.show()

        window?.setLayout(
            dp(340),
            android.view.WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private fun isAutoScreenshotEnabled(): Boolean {
        return getSharedPreferences(
            "automation_settings",
            MODE_PRIVATE
        ).getBoolean(
            "auto_screenshot_upload",
            false
        )
    }

    private fun setAutoScreenshotEnabled(
        enabled: Boolean
    ) {
        getSharedPreferences(
            "automation_settings",
            MODE_PRIVATE
        )
            .edit()
            .putBoolean(
                "auto_screenshot_upload",
                enabled
            )
            .apply()
    }

    private fun controlScreenshotService(
        enabled: Boolean
    ) {

        val intent =
            Intent(
                this,
                ScreenshotUploadService::class.java
            )

        intent.action =
            if (enabled) {
                ScreenshotUploadService.ACTION_START
            } else {
                ScreenshotUploadService.ACTION_STOP
            }

        if (
            android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.O
        ) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun addAutomationSection(
        parent: LinearLayout
    ) {

        val automationCard =
            section()

        automationCard.addView(
            text(
                "Automation",
                18f,
                WHITE
            ).apply {
                setTypeface(null, 1)
            }
        )

        automationCard.addView(
            text(
                "Automatically upload new screenshots after they are captured.",
                13f,
                MUTED
            )
        )

        addGap(
            automationCard,
            12
        )

        val row =
            LinearLayout(this)

        row.orientation =
            LinearLayout.HORIZONTAL

        row.gravity =
            Gravity.CENTER_VERTICAL

        val titleColumn =
            LinearLayout(this)

        titleColumn.orientation =
            LinearLayout.VERTICAL

        titleColumn.addView(
            text(
                "⚡ Auto Upload Screenshots",
                15f,
                WHITE
            ).apply {
                setTypeface(null, 1)
            }
        )

        titleColumn.addView(
            text(
                "Only new screenshots are monitored.",
                12f,
                MUTED
            )
        )

        row.addView(
            titleColumn,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        val toggle =
            Switch(this)

        toggle.isChecked =
            isAutoScreenshotEnabled()

        toggle.text =
            if (toggle.isChecked) {
                "ON"
            } else {
                "OFF"
            }

        toggle.setTextColor(
            Color.parseColor(WHITE)
        )

        row.addView(
            toggle,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        automationCard.addView(row)

        toggle.setOnCheckedChangeListener { _, enabled ->

            if (!enabled) {

                setAutoScreenshotEnabled(false)
                controlScreenshotService(false)

                toggle.text = "OFF"

                Toast.makeText(
                    this,
                    "Screenshot auto-upload disabled",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnCheckedChangeListener
            }

            if (
                !security.isTelegramConfigured()
            ) {

                toggle.isChecked = false
                toggle.text = "OFF"

                Toast.makeText(
                    this,
                    "Save and test Telegram settings first",
                    Toast.LENGTH_LONG
                ).show()

                return@setOnCheckedChangeListener
            }

            setAutoScreenshotEnabled(true)

            if (
                android.os.Build.VERSION.SDK_INT >= 33
            ) {

                requestPermissions(
                    arrayOf(
                        "android.permission.POST_NOTIFICATIONS",
                        "android.permission.READ_MEDIA_IMAGES"
                    ),
                    5001
                )
            }

            controlScreenshotService(true)

            toggle.text = "ON"

            Toast.makeText(
                this,
                "Screenshot auto-upload enabled",
                Toast.LENGTH_SHORT
            ).show()
        }

        addGap(
            automationCard,
            10
        )

        automationCard.addView(
            text(
                "Old screenshots already on your phone are ignored. " +
                        "Only screenshots created after activation are queued.",
                11f,
                MUTED
            )
        )

        parent.addView(
            automationCard
        )
    }

    private fun attachPressAnimation(button: Button) {

        button.setOnTouchListener { view, event ->

            when (event.action) {

                android.view.MotionEvent.ACTION_DOWN -> {
                    view.animate()
                        .scaleX(0.96f)
                        .scaleY(0.96f)
                        .alpha(0.82f)
                        .setDuration(90)
                        .start()
                }

                android.view.MotionEvent.ACTION_UP,
                android.view.MotionEvent.ACTION_CANCEL -> {
                    view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .alpha(1f)
                        .setDuration(130)
                        .start()
                }
            }

            false
        }
    }

    private fun testBot(
        token: String
    ): Pair<Boolean, String> {

        return try {

            val connection =
                URL(
                    "https://api.telegram.org/bot$token/getMe"
                ).openConnection()
                        as HttpURLConnection

            connection.requestMethod =
                "GET"

            connection.connectTimeout =
                15000

            connection.readTimeout =
                15000

            val response =
                connection.inputStream
                    .bufferedReader()
                    .use {
                        it.readText()
                    }

            if (
                connection.responseCode in 200..299 &&
                response.contains("\"ok\":true")
            ) {

                val username =
                    Regex(
                        "\"username\":\"([^\"]+)\""
                    )
                        .find(response)
                        ?.groupValues
                        ?.getOrNull(1)

                Pair(
                    true,
                    if (!username.isNullOrEmpty()) {
                        "@$username"
                    } else {
                        "Bot verified"
                    }
                )

            } else {

                Pair(
                    false,
                    "Invalid Bot Token"
                )
            }

        } catch (e: Exception) {

            Pair(
                false,
                e.message ?: "Network error"
            )
        }
    }

    private fun testChat(
        token: String,
        chatId: String
    ): Pair<Boolean, String> {

        return try {

            val encoded =
                java.net.URLEncoder.encode(
                    chatId,
                    "UTF-8"
                )

            val connection =
                URL(
                    "https://api.telegram.org/bot$token/getChat?chat_id=$encoded"
                ).openConnection()
                        as HttpURLConnection

            connection.requestMethod =
                "GET"

            connection.connectTimeout =
                15000

            connection.readTimeout =
                15000

            val response =
                connection.inputStream
                    .bufferedReader()
                    .use {
                        it.readText()
                    }

            if (
                connection.responseCode in 200..299 &&
                response.contains("\"ok\":true")
            ) {

                Pair(
                    true,
                    "Destination verified"
                )

            } else {

                Pair(
                    false,
                    "Chat ID not accessible"
                )
            }

        } catch (e: Exception) {

            Pair(
                false,
                e.message ?: "Network error"
            )
        }
    }

    private fun normalizeToken(
        value: String
    ): String {

        var result =
            value.trim()

        result =
            result.removePrefix(
                "https://api.telegram.org/bot"
            )

        result =
            result.removePrefix(
                "http://api.telegram.org/bot"
            )

        result =
            result.removePrefix("bot")

        return result.trim('/')
    }

    private fun baseLayout():
            LinearLayout {

        return LinearLayout(this).apply {

            orientation =
                LinearLayout.VERTICAL

            setPadding(
                dp(18),
                dp(18),
                dp(18),
                dp(28)
            )

            setBackgroundColor(
                Color.parseColor(BG)
            )
        }
    }

    private fun wrap(
        layout: LinearLayout
    ): ScrollView {

        return ScrollView(this).apply {

            setBackgroundColor(
                Color.parseColor(BG)
            )

            addView(layout)
        }
    }

    private fun addHeader(
        titleText: String,
        subtitleText: String
    ) {

        val title =
            text(
                titleText,
                30f,
                WHITE
            )

        title.setTypeface(
            null,
            1
        )

        val subtitle =
            text(
                subtitleText,
                14f,
                MUTED
            )

        root.addView(title)
        root.addView(subtitle)

        addGap(
            root,
            18
        )
    }

    private fun section():
            LinearLayout {

        return LinearLayout(this).apply {

            orientation =
                LinearLayout.VERTICAL

            setPadding(
                dp(15),
                dp(15),
                dp(15),
                dp(15)
            )

            background =
                rounded(
                    CARD,
                    18,
                    BORDER
                )

            layoutParams =
                LinearLayout.LayoutParams(
                    -1,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
        }
    }

    private fun label(
        value: String
    ): TextView {

        return text(
            value,
            13f,
            MUTED
        ).apply {
            setPadding(
                0,
                0,
                0,
                dp(6)
            )
        }
    }

    private fun input(
        hintText: String
    ): EditText {

        return EditText(this).apply {

            hint =
                hintText

            textSize =
                14f

            setTextColor(
                Color.parseColor(WHITE)
            )

            setHintTextColor(
                Color.parseColor("#64748B")
            )

            isSingleLine =
                true

            setPadding(
                dp(14),
                0,
                dp(14),
                0
            )

            background =
                rounded(
                    CARD2,
                    13,
                    BORDER
                )

            layoutParams =
                LinearLayout.LayoutParams(
                    -1,
                    dp(54)
                )
        }
    }

    private fun passwordInput(
        hintText: String
    ): EditText {

        return input(hintText).apply {

            inputType =
                InputType.TYPE_CLASS_TEXT or
                        InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
    }

    private fun primaryButton(
        value: String
    ): Button {

        return Button(this).apply {

            text =
                value

            textSize =
                13f

            setTextColor(
                Color.WHITE
            )

            setTypeface(
                null,
                1
            )

            isAllCaps =
                false

            background =
                rounded(
                    PURPLE,
                    14,
                    PURPLE
                )

            stateListAnimator = null

            attachPressAnimation(this)

            layoutParams =
                LinearLayout.LayoutParams(
                    -1,
                    dp(54)
                )
        }
    }

    private fun secondaryButton(
        value: String
    ): Button {

        return Button(this).apply {

            text =
                value

            textSize =
                13f

            setTextColor(
                Color.WHITE
            )

            setTypeface(
                null,
                1
            )

            isAllCaps =
                false

            background =
                rounded(
                    CARD2,
                    14,
                    BORDER
                )

            stateListAnimator = null

            attachPressAnimation(this)

            layoutParams =
                LinearLayout.LayoutParams(
                    -1,
                    dp(54)
                )
        }
    }

    private fun text(
        value: String,
        size: Float,
        color: String
    ): TextView {

        return TextView(this).apply {

            text =
                value

            textSize =
                size

            setTextColor(
                Color.parseColor(color)
            )
        }
    }

    private fun addGap(
        parent: LinearLayout,
        size: Int
    ) {

        parent.addView(
            Space(this),
            LinearLayout.LayoutParams(
                -1,
                dp(size)
            )
        )
    }

    private fun message(
        value: String
    ) {

        Toast.makeText(
            this,
            value,
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun rounded(
        fill: String,
        radius: Int,
        stroke: String
    ): GradientDrawable {

        return GradientDrawable().apply {

            setColor(
                Color.parseColor(fill)
            )

            cornerRadius =
                dp(radius).toFloat()

            setStroke(
                dp(1),
                Color.parseColor(stroke)
            )
        }
    }

    private fun dp(
        value: Int
    ): Int {

        return (
            value *
                    resources.displayMetrics.density
            ).toInt()
    }
}
