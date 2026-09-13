# 📤 Telegram Uploader

A secure Android application for uploading photos, videos, PDFs, and other files directly to Telegram using the Telegram Bot API.

It also includes an automatic background screenshot upload system that detects new screenshots and uploads them to your configured Telegram channel or group.

---

## ✨ Features

- 📤 Upload photos, videos, PDFs and other files directly to Telegram
- 🤖 Telegram Bot API integration
- 📸 Automatic screenshot detection and background upload
- 🔄 Upload queue with retry support
- 🔐 Password-protected Telegram settings
- 🔒 Encrypted Bot Token and Channel/Group ID storage
- ♻️ Forgot Password reset system
- 🧹 Secure credential wipe during password reset
- 🔔 Background upload notifications
- 🌙 Premium dark user interface
- 📱 Android application

---

## 🚀 How It Works

1. Install the app.
2. Open **Telegram Settings**.
3. Create a password.
4. Enter your Telegram Bot Token.
5. Enter your Telegram Channel/Group ID.
6. Save and test the Telegram connection.
7. Select files and upload them directly to Telegram.
8. Enable **Automatic Screenshot Upload** to automatically detect and upload new screenshots.

---

## 📸 Automatic Screenshot Upload

When automatic screenshot upload is enabled, the application runs a background service that monitors new images in the device media storage.

When a new screenshot is detected, the app adds it to the upload queue and sends it to the configured Telegram destination.

---

## 🔐 Security

Telegram credentials are protected using encrypted local storage.

The application provides:

- Password-protected settings
- Encrypted Bot Token storage
- Encrypted Channel/Group ID storage
- Password reset with credential deletion
- Secure reset of protected configuration

### Forgot Password

If the settings password is forgotten, the **Forgot Password** option permanently removes:

- Stored Bot Token
- Stored Channel/Group ID
- Password credentials
- Encrypted settings
- Pending screenshot-upload queue

After the reset, a new password and Telegram configuration must be created.

---

## 🛠️ Requirements

- Android 8.0 (API 26) or higher
- Telegram account
- Telegram Bot
- Telegram Channel or Group
- Bot must have permission to send messages/files to the destination

---

## 📦 Installation

Download the latest APK from the GitHub Releases section:

**Releases → Latest Release → TelegramUploader APK**

Install the APK on your Android device.

> Android may ask for permission to install applications from your browser/file manager.

---
