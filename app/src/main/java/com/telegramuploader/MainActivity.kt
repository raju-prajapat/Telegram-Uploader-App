package com.telegramuploader

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.Space
import android.widget.TextView
import android.widget.Toast
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : Activity() {

    private lateinit var root: LinearLayout
    private lateinit var statusCard: LinearLayout
    private lateinit var statusTitle: TextView
    private lateinit var statusSubtitle: TextView

    private lateinit var fileList: TextView
    private lateinit var statusText: TextView
    private lateinit var progress: ProgressBar

    private lateinit var settingsButton: Button
    private lateinit var selectButton: Button
    private lateinit var uploadButton: Button
    private lateinit var retryButton: Button
    private lateinit var clearButton: Button

    private val security by lazy {
        SecurityManager(this)
    }

    private val selectedFiles = mutableListOf<Uri>()

    private val failedFiles = mutableListOf<Uri>()

    private val PICK_FILES = 100

    private var uploading = false

    private val BG = "#080D1A"
    private val CARD = "#111827"
    private val CARD2 = "#0D1726"
    private val WHITE = "#F8FAFC"
    private val MUTED = "#94A3B8"
    private val PURPLE = "#7657FF"
    private val GREEN = "#22C55E"
    private val CYAN = "#29D3FF"
    private val RED = "#EF4444"
    private val ORANGE = "#F59E0B"
    private val BORDER = "#263247"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor =
            Color.parseColor("#050A12")

        window.navigationBarColor =
            Color.parseColor("#050A12")

        createUi()
        refreshConnectionState()
    }

    override fun onResume() {
        super.onResume()

        if (::statusTitle.isInitialized) {
            refreshConnectionState()
        }
    }

    private fun createUi() {

        val scroll = ScrollView(this)

        scroll.setBackgroundColor(
            Color.parseColor(BG)
        )

        root = LinearLayout(this)

        root.orientation =
            LinearLayout.VERTICAL

        root.setPadding(
            dp(18),
            dp(18),
            dp(18),
            dp(28)
        )

        scroll.addView(root)

        setContentView(scroll)

        createHeader()

        addGap(root, 14)

        createConnectionStatus()

        addGap(root, 14)

        createInfoCards()

        addGap(root, 14)

        createFilesCard()

        addGap(root, 14)

        createUploadCard()

        addGap(root, 16)

        createSocialLinks()

        addGap(root, 16)

        root.addView(
            text(
                "Telegram Uploader • Secure bot-based file delivery",
                11f,
                MUTED
            ).apply {
                gravity = Gravity.CENTER
            }
        )
    }

    private fun createHeader() {

        root.addView(
            text(
                "TELEGRAM BOT • FILE DELIVERY",
                12f,
                CYAN
            ).apply {
                setTypeface(null, 1)
                letterSpacing = 0.08f
            }
        )

        root.addView(
            text(
                "Telegram Uploader",
                31f,
                WHITE
            ).apply {
                setTypeface(null, 1)
            }
        )

        root.addView(
            text(
                "Fast, private and simple file delivery",
                14f,
                MUTED
            )
        )
    }

    private fun createConnectionStatus() {

        statusCard =
            LinearLayout(this)

        statusCard.orientation =
            LinearLayout.VERTICAL

        statusCard.setPadding(
            dp(15),
            dp(14),
            dp(15),
            dp(14)
        )

        statusCard.background =
            rounded(
                CARD,
                18,
                BORDER
            )

        val topRow =
            LinearLayout(this)

        topRow.orientation =
            LinearLayout.HORIZONTAL

        topRow.gravity =
            Gravity.CENTER_VERTICAL

        statusTitle =
            text(
                "Telegram not configured",
                17f,
                ORANGE
            )

        statusTitle.setTypeface(
            null,
            1
        )

        statusSubtitle =
            text(
                "Open Settings to protect your Bot Token and Chat ID.",
                12f,
                MUTED
            )

        val lock =
            text(
                "🔐",
                25f,
                WHITE
            )

        topRow.addView(lock)

        topRow.addView(
            Space(this),
            LinearLayout.LayoutParams(
                dp(10),
                1
            )
        )

        val titleColumn =
            LinearLayout(this)

        titleColumn.orientation =
            LinearLayout.VERTICAL

        titleColumn.addView(
            statusTitle
        )

        titleColumn.addView(
            statusSubtitle
        )

        topRow.addView(
            titleColumn,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        statusCard.addView(topRow)

        addCardGap(
            statusCard,
            12
        )

        settingsButton =
            secondaryButton(
                "🔒  OPEN TELEGRAM SETTINGS"
            )

        statusCard.addView(
            settingsButton
        )

        settingsButton.setOnClickListener {

            animateButton(
                settingsButton,
                "⏳  OPENING…"
            ) {
                startActivity(
                    Intent(
                        this,
                        SettingsActivity::class.java
                    )
                )
            }
        }

        root.addView(
            statusCard
        )
    }

    private fun createInfoCards() {

        val scroll =
            HorizontalScrollView(this)

        scroll.isHorizontalScrollBarEnabled =
            false

        val row =
            LinearLayout(this)

        row.orientation =
            LinearLayout.HORIZONTAL

        row.addView(
            infoCard(
                "⚡",
                "FAST UPLOAD",
                "Direct Bot API",
                PURPLE
            )
        )

        row.addView(
            infoCard(
                "🔐",
                "PRIVATE",
                "Protected settings",
                CYAN
            )
        )

        row.addView(
            infoCard(
                "📦",
                "ANY FILE",
                "Photos • Video • PDF",
                GREEN
            )
        )

        scroll.addView(row)

        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                -1,
                dp(108)
            )
        )
    }

    private fun infoCard(
        icon: String,
        title: String,
        subtitle: String,
        accent: String
    ): View {

        val card =
            LinearLayout(this)

        card.orientation =
            LinearLayout.VERTICAL

        card.setPadding(
            dp(14),
            dp(10),
            dp(14),
            dp(10)
        )

        card.background =
            rounded(
                CARD,
                16,
                BORDER
            )

        val params =
            LinearLayout.LayoutParams(
                dp(190),
                dp(102)
            )

        params.setMargins(
            0,
            0,
            dp(10),
            0
        )

        card.layoutParams =
            params

        card.addView(
            text(
                icon,
                21f,
                WHITE
            )
        )

        card.addView(
            text(
                title,
                12f,
                accent
            ).apply {
                setTypeface(null, 1)
            }
        )

        card.addView(
            text(
                subtitle,
                12f,
                MUTED
            )
        )

        return card
    }

    private fun createFilesCard() {

        val card =
            section()

        card.addView(
            text(
                "Files",
                19f,
                WHITE
            ).apply {
                setTypeface(null, 1)
            }
        )

        card.addView(
            text(
                "Select one or multiple files. Successfully uploaded files will never be retried.",
                13f,
                MUTED
            )
        )

        addCardGap(
            card,
            12
        )

        selectButton =
            secondaryButton(
                "＋  SELECT FILES"
            )

        card.addView(
            selectButton
        )

        selectButton.setOnClickListener {
            openFilePicker()
        }

        addCardGap(
            card,
            10
        )

        fileList =
            text(
                "No files selected",
                13f,
                MUTED
            )

        fileList.setPadding(
            dp(12),
            dp(12),
            dp(12),
            dp(12)
        )

        fileList.background =
            rounded(
                CARD2,
                14,
                BORDER
            )

        card.addView(
            fileList
        )

        addCardGap(
            card,
            8
        )

        clearButton =
            textButton(
                "CLEAR SELECTION"
            )

        card.addView(
            clearButton
        )

        clearButton.setOnClickListener {

            if (uploading) {
                return@setOnClickListener
            }

            selectedFiles.clear()
            failedFiles.clear()

            updateFileDisplay()

            statusTextIfReady(
                "Selection cleared",
                MUTED
            )
        }

        retryButton =
            secondaryButton(
                "↻  RETRY FAILED FILES"
            )

        retryButton.visibility =
            View.GONE

        card.addView(
            retryButton
        )

        retryButton.setOnClickListener {

            if (
                !uploading &&
                failedFiles.isNotEmpty()
            ) {

                val retryList =
                    ArrayList(failedFiles)

                selectedFiles.clear()
                selectedFiles.addAll(
                    retryList
                )

                failedFiles.clear()

                updateFileDisplay()

                uploadQueue(
                    retryList,
                    true
                )
            }
        }

        root.addView(
            card
        )
    }

    private fun createUploadCard() {

        val card =
            section()

        card.addView(
            text(
                "Upload Queue",
                19f,
                WHITE
            ).apply {
                setTypeface(null, 1)
            }
        )

        card.addView(
            text(
                "Files are processed one-by-one with automatic retry for temporary failures.",
                13f,
                MUTED
            )
        )

        addCardGap(
            card,
            12
        )

        progress =
            ProgressBar(
                this,
                null,
                android.R.attr.progressBarStyleHorizontal
            )

        progress.max = 100
        progress.progress = 0
        progress.visibility = View.GONE

        card.addView(
            progress,
            LinearLayout.LayoutParams(
                -1,
                dp(8)
            )
        )

        addCardGap(
            card,
            10
        )

        statusText =
            text(
                "Ready",
                14f,
                MUTED
            )

        card.addView(
            statusText
        )

        addCardGap(
            card,
            10
        )

        uploadButton =
            primaryUploadButton(
                "☁  UPLOAD TO TELEGRAM"
            )

        uploadButton.isEnabled = false
        uploadButton.alpha = 0.45f

        card.addView(
            uploadButton
        )

        uploadButton.setOnClickListener {

            if (
                selectedFiles.isNotEmpty() &&
                !uploading
            ) {

                val uploadList =
                    ArrayList(selectedFiles)

                uploadQueue(
                    uploadList,
                    false
                )
            }
        }

        root.addView(
            card
        )
    }

    private fun createSocialLinks() {

        val card =
            section()

        card.addView(
            text(
                "Developer",
                17f,
                WHITE
            ).apply {
                setTypeface(null, 1)
            }
        )

        card.addView(
            text(
                "Connect with me",
                12f,
                MUTED
            )
        )

        addCardGap(
            card,
            10
        )

        val row =
            LinearLayout(this)

        row.orientation =
            LinearLayout.HORIZONTAL

        val github =
            secondaryButton(
                "◉  GitHub"
            )

        val linkedin =
            secondaryButton(
                "in  LinkedIn"
            )

        row.addView(
            github,
            LinearLayout.LayoutParams(
                0,
                dp(52),
                1f
            )
        )

        row.addView(
            Space(this),
            LinearLayout.LayoutParams(
                dp(10),
                1
            )
        )

        row.addView(
            linkedin,
            LinearLayout.LayoutParams(
                0,
                dp(52),
                1f
            )
        )

        github.setOnClickListener {

            openLink(
                "https://github.com/raju-prajapat"
            )
        }

        linkedin.setOnClickListener {

            openLink(
                "https://www.linkedin.com/in/raju-ram-839b49394"
            )
        }

        card.addView(row)

        root.addView(card)
    }

    private fun openFilePicker() {

        val intent =
            Intent(
                Intent.ACTION_OPEN_DOCUMENT
            ).apply {

                addCategory(
                    Intent.CATEGORY_OPENABLE
                )

                type = "*/*"

                putExtra(
                    Intent.EXTRA_ALLOW_MULTIPLE,
                    true
                )
            }

        startActivityForResult(
            intent,
            PICK_FILES
        )
    }

    @Deprecated("Old Android result API")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {

        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )

        if (
            requestCode != PICK_FILES ||
            resultCode != RESULT_OK ||
            data == null
        ) {
            return
        }

        selectedFiles.clear()

        val clip =
            data.clipData

        if (clip != null) {

            for (
                index in 0 until clip.itemCount
            ) {

                val uri =
                    clip
                        .getItemAt(index)
                        .uri

                if (
                    !selectedFiles.contains(uri)
                ) {

                    selectedFiles.add(uri)
                }
            }

        } else {

            data.data?.let {
                selectedFiles.add(it)
            }
        }

        updateFileDisplay()

        statusTextIfReady(
            "${selectedFiles.size} file(s) selected",
            CYAN
        )
    }

    private fun updateFileDisplay() {

        if (
            selectedFiles.isEmpty()
        ) {

            fileList.text =
                "No files selected"

            fileList.setTextColor(
                Color.parseColor(MUTED)
            )

            retryButton.visibility =
                if (failedFiles.isNotEmpty()) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

            updateUploadButton()
            return
        }

        val builder =
            StringBuilder()

        val visible =
            minOf(
                selectedFiles.size,
                8
            )

        for (
            index in 0 until visible
        ) {

            builder
                .append("• ")
                .append(
                    getFileName(
                        selectedFiles[index]
                    )
                )
                .append("\n")
        }

        if (
            selectedFiles.size >
            visible
        ) {

            builder
                .append("• +")
                .append(
                    selectedFiles.size - visible
                )
                .append(" more file(s)")
        }

        fileList.text =
            builder
                .toString()
                .trim()

        fileList.setTextColor(
            Color.parseColor(WHITE)
        )

        retryButton.visibility =
            if (failedFiles.isNotEmpty()) {
                View.VISIBLE
            } else {
                View.GONE
            }

        updateUploadButton()
    }

    private fun refreshConnectionState() {

        if (
            security.isTelegramConfigured()
        ) {

            statusTitle.text =
                "Telegram configured"

            statusTitle.setTextColor(
                Color.parseColor(GREEN)
            )

            statusSubtitle.text =
                "Protected Bot Token and Chat ID are saved."

        } else {

            statusTitle.text =
                "Telegram not configured"

            statusTitle.setTextColor(
                Color.parseColor(ORANGE)
            )

            statusSubtitle.text =
                "Open Settings to protect your Bot Token and Chat ID."
        }

        updateUploadButton()
    }

    private fun updateUploadButton() {

        val enabled =
            !uploading &&
                    security.isTelegramConfigured() &&
                    selectedFiles.isNotEmpty()

        uploadButton.isEnabled =
            enabled

        uploadButton.alpha =
            if (enabled) 1f else 0.45f
    }

    private fun statusTextIfReady(
        message: String,
        color: String
    ) {

        if (::statusText.isInitialized) {

            statusText.text =
                message

            statusText.setTextColor(
                Color.parseColor(color)
            )
        }
    }


    private fun uploadQueue(
        queue: ArrayList<Uri>,
        isRetry: Boolean
    ) {

        if (uploading) {
            return
        }

        if (queue.isEmpty()) {

            statusTextIfReady(
                "No files to upload",
                RED
            )

            return
        }

        if (!security.isTelegramConfigured()) {

            statusTextIfReady(
                "Open Telegram Settings first",
                RED
            )

            Toast.makeText(
                this,
                "Telegram settings are not configured",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val token =
            security.getTelegramToken()

        val chatId =
            security.getTelegramChatId()

        if (
            token.isEmpty() ||
            chatId.isEmpty()
        ) {

            statusTextIfReady(
                "Telegram configuration is incomplete",
                RED
            )

            return
        }

        uploading = true

        uploadButton.isEnabled = false
        uploadButton.alpha = 0.45f

        selectButton.isEnabled = false
        selectButton.alpha = 0.45f

        clearButton.isEnabled = false
        clearButton.alpha = 0.45f

        retryButton.isEnabled = false

        progress.visibility =
            View.VISIBLE

        progress.progress = 0

        val failedThisRun =
            mutableListOf<Uri>()

        thread {

            var uploaded = 0
            var failed = 0

            val total =
                queue.size

            for (index in queue.indices) {

                val uri =
                    queue[index]

                val current =
                    index + 1

                runOnUiThread {

                    statusTextIfReady(
                        "⏳ Uploading $current/$total\n${getFileName(uri)}",
                        CYAN
                    )

                    progress.progress =
                        (
                            (index.toFloat() /
                                    total.toFloat()) *
                                    100f
                            ).toInt()
                }

                val result =
                    sendFileWithRetry(
                        token,
                        chatId,
                        uri
                    )

                if (result.first) {

                    uploaded++

                } else {

                    failed++

                    failedThisRun.add(uri)
                }

                runOnUiThread {

                    progress.progress =
                        (
                            (
                                (index + 1).toFloat() /
                                        total.toFloat()
                                ) * 100f
                            ).toInt()
                }
            }

            runOnUiThread {

                uploading = false

                selectButton.isEnabled = true
                selectButton.alpha = 1f

                clearButton.isEnabled = true
                clearButton.alpha = 1f

                failedFiles.clear()
                failedFiles.addAll(
                    failedThisRun
                )

                /*
                 * IMPORTANT:
                 *
                 * After an upload run, selectedFiles
                 * contains ONLY files that failed.
                 *
                 * Successfully uploaded files are removed
                 * from the active queue and therefore can
                 * never be retried accidentally.
                 */
                selectedFiles.clear()
                selectedFiles.addAll(
                    failedThisRun
                )

                updateFileDisplay()

                progress.progress = 100

                if (failed == 0) {

                    retryButton.visibility =
                        View.GONE

                    statusTextIfReady(
                        "✓ All $uploaded files uploaded successfully",
                        GREEN
                    )

                    Toast.makeText(
                        this,
                        if (isRetry) {
                            "All failed files uploaded successfully"
                        } else {
                            "Upload completed successfully"
                        },
                        Toast.LENGTH_SHORT
                    ).show()

                } else {

                    retryButton.visibility =
                        View.VISIBLE

                    retryButton.isEnabled =
                        true

                    statusTextIfReady(
                        "Uploaded: $uploaded   •   Failed: $failed",
                        RED
                    )

                    Toast.makeText(
                        this,
                        "$failed file(s) need retry",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                updateUploadButton()
            }
        }
    }

    private fun sendFileWithRetry(
        token: String,
        chatId: String,
        uri: Uri
    ): Pair<Boolean, String> {

        val maxAttempts = 5

        var lastMessage =
            "Upload failed"

        for (attempt in 1..maxAttempts) {

            val result =
                sendFile(
                    token,
                    chatId,
                    uri
                )

            if (result.first) {
                return result
            }

            lastMessage =
                result.second

            /*
             * Telegram 429 response can contain
             * retry_after. Wait for the requested time.
             */
            val retryAfter =
                parseRetryAfter(
                    lastMessage
                )

            if (retryAfter > 0) {

                Thread.sleep(
                    (retryAfter * 1000L)
                        .coerceAtMost(120000L)
                )

                continue
            }

            /*
             * Retry network errors and 5xx/server errors.
             * Do not waste retries on obvious permanent
             * errors such as an invalid chat, invalid token,
             * or file-size limitation.
             */
            if (
                isPermanentTelegramError(
                    lastMessage
                )
            ) {

                return Pair(
                    false,
                    lastMessage
                )
            }

            if (
                attempt < maxAttempts
            ) {

                val delay =
                    (
                        1500L *
                                (1L shl
                                    (attempt - 1))
                        ).coerceAtMost(15000L)

                Thread.sleep(delay)
            }
        }

        return Pair(
            false,
            lastMessage
        )
    }

    private fun sendFile(
        token: String,
        chatId: String,
        uri: Uri
    ): Pair<Boolean, String> {

        val size =
            getFileSize(uri)

        /*
         * Standard Telegram Bot API currently has a
         * 50 MB upload limit for a bot file.
         */
        if (
            size > 50L * 1024L * 1024L
        ) {

            return Pair(
                false,
                "File is larger than Telegram Bot API 50 MB limit"
            )
        }

        val boundary =
            "----TelegramUploader_${System.nanoTime()}"

        var connection:
                HttpURLConnection? = null

        return try {

            connection =
                URL(
                    "https://api.telegram.org/bot$token/sendDocument"
                ).openConnection()
                        as HttpURLConnection

            connection.requestMethod =
                "POST"

            connection.connectTimeout =
                30000

            connection.readTimeout =
                120000

            connection.useCaches =
                false

            connection.doInput =
                true

            connection.doOutput =
                true

            connection.setRequestProperty(
                "Content-Type",
                "multipart/form-data; boundary=$boundary"
            )

            connection.setRequestProperty(
                "Accept",
                "application/json"
            )

            val output =
                connection.outputStream

            fun writeText(
                value: String
            ) {

                output.write(
                    value.toByteArray(
                        Charsets.UTF_8
                    )
                )
            }

            /*
             * chat_id field
             */
            writeText(
                "--$boundary\r\n"
            )

            writeText(
                "Content-Disposition: " +
                        "form-data; " +
                        "name=\"chat_id\"\r\n\r\n"
            )

            writeText(
                chatId
            )

            writeText(
                "\r\n"
            )

            /*
             * document field
             */
            val fileName =
                safeFileName(
                    getFileName(uri)
                )

            val mimeType =
                contentResolver.getType(uri)
                    ?: "application/octet-stream"

            writeText(
                "--$boundary\r\n"
            )

            writeText(
                "Content-Disposition: " +
                        "form-data; " +
                        "name=\"document\"; " +
                        "filename=\"$fileName\"\r\n"
            )

            writeText(
                "Content-Type: $mimeType\r\n\r\n"
            )

            val input =
                contentResolver.openInputStream(
                    uri
                )

            if (input == null) {

                output.close()

                return Pair(
                    false,
                    "Cannot open selected file"
                )
            }

            input.use { stream ->

                val buffer =
                    ByteArray(64 * 1024)

                while (true) {

                    val count =
                        stream.read(buffer)

                    if (count <= 0) {
                        break
                    }

                    output.write(
                        buffer,
                        0,
                        count
                    )
                }
            }

            writeText(
                "\r\n--$boundary--\r\n"
            )

            output.flush()
            output.close()

            val responseCode =
                connection.responseCode

            val responseText =
                if (
                    responseCode in 200..299
                ) {

                    connection.inputStream
                        .bufferedReader()
                        .use {
                            it.readText()
                        }

                } else {

                    connection.errorStream
                        ?.bufferedReader()
                        ?.use {
                            it.readText()
                        }
                        ?: "HTTP $responseCode"
                }

            val successful =
                responseCode in 200..299 &&
                        responseText.contains(
                            "\"ok\":true"
                        )

            if (successful) {

                Pair(
                    true,
                    "Uploaded"
                )

            } else {

                Pair(
                    false,
                    responseText.ifBlank {
                        "HTTP $responseCode"
                    }
                )
            }

        } catch (e: Exception) {

            Pair(
                false,
                e.message ?: "Network/upload error"
            )

        } finally {

            connection?.disconnect()
        }
    }

    private fun parseRetryAfter(
        response: String
    ): Int {

        return try {

            Regex(
                "\"retry_after\"\\s*:\\s*(\\d+)"
            )
                .find(response)
                ?.groupValues
                ?.getOrNull(1)
                ?.toIntOrNull()
                ?: 0

        } catch (_: Exception) {

            0
        }
    }

    private fun isPermanentTelegramError(
        response: String
    ): Boolean {

        val lower =
            response.lowercase()

        return lower.contains(
            "invalid bot token"
        ) ||
                lower.contains(
                    "chat not found"
                ) ||
                lower.contains(
                    "bot was blocked"
                ) ||
                lower.contains(
                    "not enough rights"
                ) ||
                lower.contains(
                    "forbidden"
                ) ||
                lower.contains(
                    "file is larger than"
                ) ||
                lower.contains(
                    "message is too long"
                ) ||
                lower.contains(
                    "bad request"
                )
    }

    private fun getFileSize(
        uri: Uri
    ): Long {

        var size = 0L

        contentResolver.query(
            uri,
            arrayOf(
                OpenableColumns.SIZE
            ),
            null,
            null,
            null
        )?.use { cursor ->

            if (
                cursor.moveToFirst()
            ) {

                val index =
                    cursor.getColumnIndex(
                        OpenableColumns.SIZE
                    )

                if (index >= 0 &&
                    !cursor.isNull(index)
                ) {

                    size =
                        cursor.getLong(index)
                }
            }
        }

        return size
    }

    private fun getFileName(
        uri: Uri
    ): String {

        var name =
            "Unknown file"

        contentResolver.query(
            uri,
            arrayOf(
                OpenableColumns.DISPLAY_NAME
            ),
            null,
            null,
            null
        )?.use { cursor ->

            if (
                cursor.moveToFirst()
            ) {

                val index =
                    cursor.getColumnIndex(
                        OpenableColumns.DISPLAY_NAME
                    )

                if (index >= 0) {

                    name =
                        cursor.getString(index)
                }
            }
        }

        return name
    }

    private fun safeFileName(
        name: String
    ): String {

        return name
            .replace(
                "\\",
                "_"
            )
            .replace(
                "\"",
                "'"
            )
            .replace(
                "\r",
                "_"
            )
            .replace(
                "\n",
                "_"
            )
    }

    private fun animateButton(
        button: Button,
        temporaryText: String,
        action: () -> Unit
    ) {

        button.isEnabled =
            false

        val oldText =
            button.text.toString()

        button.text =
            temporaryText

        button.animate()
            .alpha(0.65f)
            .setDuration(120)
            .withEndAction {

                action()

                button.postDelayed({

                    button.text =
                        oldText

                    button.alpha = 1f

                    button.isEnabled =
                        true

                }, 250)

            }
            .start()
    }

    private fun openLink(
        url: String
    ) {

        try {

            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(url)
                )
            )

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Unable to open link",
                Toast.LENGTH_SHORT
            ).show()
        }
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

    private fun primaryUploadButton(
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

                rounded(
                    GREEN,
                    14,
                    GREEN
                )

            stateListAnimator = null

            attachPressAnimation(this)

            layoutParams =
                LinearLayout.LayoutParams(
                    -1,
                    dp(56)
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

    private fun textButton(
        value: String
    ): Button {

        return Button(this).apply {

            text =
                value

            textSize =
                11f

            setTextColor(
                Color.parseColor(
                    MUTED
                )
            )

            setTypeface(
                null,
                1
            )

            isAllCaps =
                false

            gravity =
                Gravity.END or
                        Gravity.CENTER_VERTICAL

            background = null
            stateListAnimator = null

            attachPressAnimation(this)

            layoutParams =
                LinearLayout.LayoutParams(
                    -1,
                    dp(38)
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
                Color.parseColor(
                    color
                )
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

    private fun addCardGap(
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

    private fun rounded(
        fill: String,
        radius: Int,
        stroke: String
    ): GradientDrawable {

        return GradientDrawable().apply {

            setColor(
                Color.parseColor(
                    fill
                )
            )

            cornerRadius =
                dp(radius).toFloat()

            setStroke(
                dp(1),
                Color.parseColor(
                    stroke
                )
            )
        }
    }

    private fun dp(
        value: Int
    ): Int {

        return (
            value *
                    resources
                        .displayMetrics
                        .density
            ).toInt()
    }
}
