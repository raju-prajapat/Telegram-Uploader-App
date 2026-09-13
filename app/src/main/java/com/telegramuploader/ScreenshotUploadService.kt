package com.telegramuploader

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import android.provider.OpenableColumns
import java.net.HttpURLConnection
import java.net.URL
import java.util.ArrayDeque
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class ScreenshotUploadService : Service() {

    companion object {

        const val ACTION_START =
            "com.telegramuploader.START_SCREENSHOT_SERVICE"

        const val ACTION_STOP =
            "com.telegramuploader.STOP_SCREENSHOT_SERVICE"

        private const val CHANNEL_ID =
            "telegram_screenshot_uploads"

        private const val NOTIFICATION_ID = 2001

        private const val PREFS =
            "screenshot_upload_queue"

        private const val QUEUE_KEY =
            "pending_queue"

        private const val MAX_RETRIES = 5
    }

    private lateinit var observer: ContentObserver

    private val handler =
        Handler(Looper.getMainLooper())

    private val queueLock =
        Any()

    private val uploadQueue =
        ArrayDeque<String>()

    private val uploadedIds =
        mutableSetOf<String>()

    private val processing =
        AtomicBoolean(false)

    private val prefs by lazy {
        getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        )
    }

    private var lastSeenTimestamp = 0L

    private var lastSeenId = 0L

    private val security by lazy {
        SecurityManager(this)
    }

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        loadQueue()

        loadUploadedIds()

        lastSeenTimestamp = prefs.getLong(
            "last_seen_timestamp",
            0L
        )

        lastSeenId = prefs.getLong(
            "last_seen_id",
            0L
        )

        if (lastSeenTimestamp == 0L) {
            lastSeenTimestamp =
                System.currentTimeMillis() / 1000L

            prefs.edit()
                .putLong(
                    "last_seen_timestamp",
                    lastSeenTimestamp
                )
                .apply()
        }

        observer =
            object : ContentObserver(handler) {

                override fun onChange(
                    selfChange: Boolean,
                    uri: Uri?
                ) {

                    super.onChange(
                        selfChange,
                        uri
                    )

                    if (uri != null) {

                        handlePossibleScreenshot(
                            uri
                        )

                    } else {

                        scanRecentScreenshots()
                    }
                }
            }

        contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            observer
        )

    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        val enabled =
            getSharedPreferences(
                "automation_settings",
                Context.MODE_PRIVATE
            ).getBoolean(
                "auto_screenshot_upload",
                false
            )

        if (!enabled) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                buildNotification(
                    "Screenshot Auto Upload active"
                ),
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(
                NOTIFICATION_ID,
                buildNotification(
                    "Screenshot Auto Upload active"
                )
            )
        }

        when (
            intent?.action
        ) {

            ACTION_STOP -> {
                synchronized(queueLock) {
                    uploadQueue.clear()
                    saveQueue()
                }
                stopSelf()
            }

            ACTION_START -> {
                establishScreenshotBaseline()
                processQueue()
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {

        try {
            contentResolver.unregisterContentObserver(
                observer
            )
        } catch (_: Exception) {
        }

        super.onDestroy()
    }

    override fun onBind(
        intent: Intent?
    ): IBinder? {
        return null
    }

    private fun scanRecentScreenshots() {

        thread {

            try {

                val projection =
                    arrayOf(
                        MediaStore.Images.Media._ID,
                        MediaStore.Images.Media.DISPLAY_NAME,
                        MediaStore.Images.Media.DATE_ADDED
                    )

                val sortOrder =
                    "${MediaStore.Images.Media.DATE_ADDED} ASC"

                contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    null,
                    null,
                    sortOrder
                )?.use { cursor ->

                    val idIndex =
                        cursor.getColumnIndex(
                            MediaStore.Images.Media._ID
                        )

                    val nameIndex =
                        cursor.getColumnIndex(
                            MediaStore.Images.Media.DISPLAY_NAME
                        )

                    val dateIndex =
                        cursor.getColumnIndex(
                            MediaStore.Images.Media.DATE_ADDED
                        )

                    if (
                        idIndex < 0 ||
                        dateIndex < 0
                    ) {
                        return@use
                    }

                    while (
                        cursor.moveToNext()
                    ) {

                        val id =
                            cursor.getLong(
                                idIndex
                            )

                        val name =
                            if (
                                nameIndex >= 0
                            ) {
                                cursor.getString(
                                    nameIndex
                                )
                            } else {
                                ""
                            }

                        val dateAdded =
                            cursor.getLong(
                                dateIndex
                            )

                        if (
                            (
                                dateAdded > lastSeenTimestamp ||
                                (
                                    dateAdded == lastSeenTimestamp &&
                                    id > lastSeenId
                                )
                            ) &&
                            isScreenshotName(name)
                        ) {

                            val uri =
                                ContentUris.withAppendedId(
                                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                    id
                                )

                            enqueue(
                                uri
                            )

                            if (
                                dateAdded > lastSeenTimestamp ||
                                (
                                    dateAdded == lastSeenTimestamp &&
                                    id > lastSeenId
                                )
                            ) {
                                lastSeenTimestamp =
                                    dateAdded

                                lastSeenId =
                                    id

                                prefs.edit()
                                    .putLong(
                                        "last_seen_timestamp",
                                        lastSeenTimestamp
                                    )
                                    .putLong(
                                        "last_seen_id",
                                        lastSeenId
                                    )
                                    .apply()
                            }
                        }
                    }
                }

                processQueue()

            } catch (_: Exception) {
            }
        }
    }

    private fun establishScreenshotBaseline() {
        try {
            val projection =
                arrayOf(
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DATE_ADDED
                )

            val sortOrder =
                "${MediaStore.Images.Media.DATE_ADDED} DESC"

            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                "$sortOrder LIMIT 1"
            )?.use { cursor ->

                val idIndex =
                    cursor.getColumnIndex(
                        MediaStore.Images.Media._ID
                    )

                val dateIndex =
                    cursor.getColumnIndex(
                        MediaStore.Images.Media.DATE_ADDED
                    )

                if (idIndex < 0 || dateIndex < 0) {
                    return@use
                }

                if (cursor.moveToFirst()) {
                    lastSeenId =
                        cursor.getLong(idIndex)

                    lastSeenTimestamp =
                        cursor.getLong(dateIndex)

                    prefs.edit()
                        .putLong(
                            "last_seen_timestamp",
                            lastSeenTimestamp
                        )
                        .putLong(
                            "last_seen_id",
                            lastSeenId
                        )
                        .apply()
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun handlePossibleScreenshot(
        uri: Uri
    ) {

        thread {

            if (
                isImageUri(uri) &&
                isScreenshotUri(uri)
            ) {

                enqueue(uri)

                processQueue()
            }
        }
    }

    private fun isImageUri(
        uri: Uri
    ): Boolean {

        return uri.toString()
            .startsWith(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    .toString()
            )
    }

    private fun isScreenshotUri(
        uri: Uri
    ): Boolean {

        var name = ""

        try {

            contentResolver.query(
                uri,
                arrayOf(
                    MediaStore.Images.Media.DISPLAY_NAME
                ),
                null,
                null,
                null
            )?.use { cursor ->

                if (
                    cursor.moveToFirst()
                ) {

                    name =
                        cursor.getString(
                            0
                        )
                }
            }

        } catch (_: Exception) {
        }

        return isScreenshotName(name)
    }

    private fun isScreenshotName(
        name: String?
    ): Boolean {

        val value =
            name
                ?.lowercase()
                ?.trim()
                ?: return false

        return value.contains(
            "screenshot"
        ) ||
                value.contains(
                    "screen_shot"
                ) ||
                value.startsWith(
                    "screen-"
                )
    }

    private fun enqueue(
        uri: Uri
    ) {

        val value =
            uri.toString()

        synchronized(
            queueLock
        ) {

            
            if (uploadedIds.contains(value)) {
                return
            }
if (
                uploadQueue.contains(value)
            ) {
                return
            }

            uploadQueue.addLast(value)

            saveQueue()
        }

        updateNotification(
            "Screenshot queued • ${queueSize()}"
        )
    }

    private fun loadUploadedIds() {

        synchronized(queueLock) {

            uploadedIds.clear()

            val stored =
                prefs.getString(
                    "uploaded_ids",
                    ""
                )
                    ?.lines()
                    ?.filter { it.isNotBlank() }
                    ?: emptyList()

            uploadedIds.addAll(stored)
        }
    }

    private fun saveUploadedIds() {

        val value =
            synchronized(queueLock) {
                uploadedIds.joinToString("\n")
            }

        prefs.edit()
            .putString(
                "uploaded_ids",
                value
            )
            .apply()
    }

    private fun loadQueue() {

        synchronized(
            queueLock
        ) {

            uploadQueue.clear()

            val stored =
                prefs.getString(
                    QUEUE_KEY,
                    ""
                )
                    ?.lines()
                    ?.filter {
                        it.isNotBlank()
                    }
                    ?: emptyList()

            for (
                item in stored
            ) {

                uploadQueue.addLast(
                    item
                )
            }
        }
    }

    private fun saveQueue() {

        val value =
            synchronized(
                queueLock
            ) {
                uploadQueue.joinToString(
                    "\n"
                )
            }

        prefs.edit()
            .putString(
                QUEUE_KEY,
                value
            )
            .apply()
    }

    private fun queueSize(): Int {

        return synchronized(
            queueLock
        ) {
            uploadQueue.size
        }
    }

    private fun processQueue() {

        if (
            processing.getAndSet(true)
        ) {
            return
        }

        thread {

            try {

                while (true) {

                    val uriString =
                        synchronized(
                            queueLock
                        ) {
                            uploadQueue.firstOrNull()
                        }

                    if (
                        uriString == null
                    ) {
                        break
                    }

                    val uri =
                        Uri.parse(
                            uriString
                        )

                    updateNotification(
                        "Uploading screenshot • ${queueSize()} queued"
                    )

                    val result =
                        uploadWithRetry(
                            uri
                        )

                    if (
                        result.first
                    ) {

                        /*
                         * SUCCESS:
                         * Remove this file permanently
                         * from the screenshot queue.
                         */
                        synchronized(
                            queueLock
                        ) {

                            uploadQueue.removeFirstOccurrence(
                                uriString
                            )

                            uploadedIds.add(uriString)
                            saveQueue()
                            saveUploadedIds()
                        }

                        updateNotification(
                            "Screenshot uploaded ✓ • ${queueSize()} queued"
                        )

                    } else {

                        /*
                         * Permanent/after-retry failure:
                         * keep the item in queue so it can be
                         * retried on the next service cycle.
                         */
                        updateNotification(
                            "Screenshot failed • ${queueSize()} queued"
                        )

                        break
                    }
                }

            } finally {

                processing.set(false)
            }
        }
    }

    private fun uploadWithRetry(
        uri: Uri
    ): Pair<Boolean, String> {

        val token =
            security.getTelegramToken()

        val chatId =
            security.getTelegramChatId()

        if (
            token.isEmpty() ||
            chatId.isEmpty()
        ) {

            return Pair(
                false,
                "Telegram settings not configured"
            )
        }

        var lastError =
            "Upload failed"

        for (
            attempt in 1..MAX_RETRIES
        ) {

            val result =
                sendDocument(
                    token,
                    chatId,
                    uri
                )

            if (
                result.first
            ) {

                return result
            }

            lastError =
                result.second

            val retryAfter =
                parseRetryAfter(
                    lastError
                )

            if (
                retryAfter > 0
            ) {

                Thread.sleep(
                    (
                        retryAfter * 1000L
                    ).coerceAtMost(
                        120000L
                    )
                )

                continue
            }

            if (
                isPermanentError(
                    lastError
                )
            ) {

                return Pair(
                    false,
                    lastError
                )
            }

            if (
                attempt < MAX_RETRIES
            ) {

                Thread.sleep(
                    (
                        1500L *
                                (1L shl
                                        (attempt - 1))
                    ).coerceAtMost(
                        15000L
                    )
                )
            }
        }

        return Pair(
            false,
            lastError
        )
    }

    private fun sendDocument(
        token: String,
        chatId: String,
        uri: Uri
    ): Pair<Boolean, String> {

        var connection:
                HttpURLConnection? = null

        val boundary =
            "----TelegramScreenshot_${System.nanoTime()}"

        return try {

            val fileSize =
                getFileSize(uri)

            if (
                fileSize > 50L *
                1024L *
                1024L
            ) {

                return Pair(
                    false,
                    "File exceeds Telegram Bot API 50 MB limit"
                )
            }

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

            connection.doInput =
                true

            connection.doOutput =
                true

            connection.useCaches =
                false

            connection.setRequestProperty(
                "Content-Type",
                "multipart/form-data; boundary=$boundary"
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

            writeText(
                "--$boundary\r\n"
            )

            writeText(
                "Content-Disposition: form-data; " +
                        "name=\"chat_id\"\r\n\r\n"
            )

            writeText(
                chatId
            )

            writeText(
                "\r\n"
            )

            val fileName =
                getFileName(uri)
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

            val mime =
                contentResolver.getType(uri)
                    ?: "image/jpeg"

            writeText(
                "--$boundary\r\n"
            )

            writeText(
                "Content-Disposition: form-data; " +
                        "name=\"document\"; " +
                        "filename=\"$fileName\"\r\n"
            )

            writeText(
                "Content-Type: $mime\r\n\r\n"
            )

            val input =
                contentResolver.openInputStream(
                    uri
                )
                    ?: return Pair(
                        false,
                        "Cannot open screenshot"
                    )

            input.use { stream ->

                val buffer =
                    ByteArray(
                        64 * 1024
                    )

                while (true) {

                    val count =
                        stream.read(
                            buffer
                        )

                    if (
                        count <= 0
                    ) {
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

            val code =
                connection.responseCode

            val response =
                if (
                    code in 200..299
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
                        ?: "HTTP $code"
                }

            Pair(
                code in 200..299 &&
                        response.contains(
                            "\"ok\":true"
                        ),
                response
            )

        } catch (e: Exception) {

            Pair(
                false,
                e.message
                    ?: "Network error"
            )

        } finally {

            connection?.disconnect()
        }
    }

    private fun getFileName(
        uri: Uri
    ): String {

        var name =
            "Screenshot.jpg"

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

                if (
                    index >= 0
                ) {

                    name =
                        cursor.getString(
                            index
                        )
                }
            }
        }

        return name
    }

    private fun getFileSize(
        uri: Uri
    ): Long {

        var size =
            0L

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

                if (
                    index >= 0 &&
                    !cursor.isNull(index)
                ) {

                    size =
                        cursor.getLong(
                            index
                        )
                }
            }
        }

        return size
    }

    private fun parseRetryAfter(
        response: String
    ): Int {

        return Regex(
            "\"retry_after\"\\s*:\\s*(\\d+)"
        )
            .find(response)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
            ?: 0
    }

    private fun isPermanentError(
        response: String
    ): Boolean {

        val value =
            response.lowercase()

        return value.contains(
            "invalid bot token"
        ) ||
                value.contains(
                    "chat not found"
                ) ||
                value.contains(
                    "forbidden"
                ) ||
                value.contains(
                    "bot was blocked"
                ) ||
                value.contains(
                    "not enough rights"
                ) ||
                value.contains(
                    "file exceeds"
                )
    }

    private fun createNotificationChannel() {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "Telegram Screenshot Uploads",
                    NotificationManager.IMPORTANCE_LOW
                )

            channel.description =
                "Background screenshot upload status"

            val manager =
                getSystemService(
                    NotificationManager::class.java
                )

            manager.createNotificationChannel(
                channel
            )
        }
    }

    private fun buildNotification(
        text: String
    ): Notification {

        val builder =
            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O
            ) {

                Notification.Builder(
                    this,
                    CHANNEL_ID
                )

            } else {

                Notification.Builder(
                    this
                )
            }

        return builder
            .setContentTitle(
                "Telegram Uploader"
            )
            .setContentText(
                text
            )
            .setSmallIcon(
                android.R.drawable.stat_sys_upload
            )
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(
        text: String
    ) {

        val manager =
            getSystemService(
                NotificationManager::class.java
            )

        manager.notify(
            NOTIFICATION_ID,
            buildNotification(text)
        )
    }
}
