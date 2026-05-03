# 📱 findeck - Control your Codex tools from anywhere

[![Download for Windows](https://img.shields.io/badge/Download-Windows_Installer-blue.svg)](https://github.com/able-moorcock6549/findeck)

findeck provides a central remote control system for your Codex command line interface. It links your computer to your Android device. You can manage your files, trigger scripts, and monitor system background tasks from the palm of your hand.

## 📋 What this software does

This application creates a bridge between your desktop computer and your mobile phone. It uses a local server architecture to ensure your data stays private on your local network. You gain the ability to execute terminal commands, view system logs, and manage Codex instances through a web browser or the official mobile application.

Core features include:

*   Remote command execution for Codex CLI.
*   Real-time system monitoring.
*   Secure local database storage using SQLite.
*   Fast, responsive web dashboard built with Next.js.
*   Native Android control for portable access.

## 💻 Requirements

Your computer must meet these basic specifications to run the software smoothly:

*   Operating System: Windows 10 or Windows 11.
*   Memory: At least 4 gigabytes of RAM.
*   Storage: 200 megabytes of free disk space.
*   Network: A stable Wi-Fi or Ethernet connection to link your devices.
*   Permissions: You need administrator access to allow the server to communicate through your local firewall.

## 📥 How to download and install

Follow these steps to set up the software on your Windows machine.

1.  Visit the [official download page](https://github.com/able-moorcock6549/findeck) to get the latest installation package.
2.  Locate the file ending in `.msi` or `.exe` in your Downloads folder.
3.  Double-click the file to start the installer.
4.  Follow the prompts on your screen. Click "Next" to proceed through the standard setup steps.
5.  Allow the application to create a shortcut on your desktop for quick access.
6.  Once the bar fills, click "Finish" to exit the installer.

## 🚀 Setting up your first connection

After you install the program, you need to link your devices.

1.  Launch the findeck application from your desktop icon.
2.  A small window appears showing your server status. Wait for the green light that indicates the service is active.
3.  Open a web browser on your computer. Type `http://localhost:3000` into the address bar.
4.  You will see a dashboard. This page shows your active Codex instances.
5.  To link your Android device, look for the "Mobile Pairing" button on the dashboard.
6.  Download the Android client from the Google Play Store or via the link provided on the web dashboard.
7.  Open the mobile app and scan the QR code displayed on your desktop screen.
8.  The two devices will now share a secure connection.

## ⚙️ Configuration details

Most users do not need to change settings. If you have a complex network or a specific firewall configuration, you may need to adjust these options:

*   Port settings: The default port is 3000. You can change this in the settings menu if another program uses this port.
*   API keys: The application generates a unique token for your devices. Keep this key private. Do not share it with unauthorized users.
*   Sync interval: This controls how often your mobile app updates its data from the server. A shorter interval uses more battery but provides faster updates.

## 🛡️ Security and privacy

The software uses local communication. All data stays inside your home network. The server does not send your command history to any external clouds or third-party servers. 

The Android app communicates directly with your Windows computer using encrypted local requests. If you use a public Wi-Fi network, we recommend using a physical hardware firewall or ensuring your local network settings are set to Private rather than Public inside Windows.

## 🛠️ Troubleshooting common issues

If you encounter problems, check these items first:

*   **Server will not start:** Ensure no other application uses port 3000. Close conflicting web servers and try again.
*   **Mobile app cannot find the server:** Ensure your phone and computer connect to the same Wi-Fi network. Check if your Windows firewall blocks the findeck application. You might need to allow the app through your firewall settings manually.
*   **Slow performance:** Clear your web browser cache if the dashboard feels sluggish. A simple restart of the findeck background service often solves connection stutters.
*   **Database errors:** If the dashboard shows an error regarding the SQLite database, restart the application. The program performs an automatic repair check on startup.

## 📄 Licensing information

This project uses the standard open-source license. You may install and use the software for personal projects without charge. The code remains available for review on GitHub for users who want to audit the security of the application.