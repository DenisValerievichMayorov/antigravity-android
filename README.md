# Antigravity CLI Android Shell / Frontend

This is a beautiful Material 3 native Android application designed to serve as a graphical frontend for the **Antigravity CLI** (`agy`) running in Termux.

## How to build this app in GitHub Actions (Cloud Build)

Since building large Android apps inside Termux can be slow or encounter SSL certificate issues, we configured GitHub Actions for this repository.

### Build Steps:
1. Create a new repository on your GitHub account (e.g., `antigravity-android-app`).
2. Initialize Git inside this directory and push the project to your GitHub:
   ```bash
   git init
   git add .
   git commit -m "Initial commit of Antigravity Shell"
   git remote add origin https://github.com/YOUR_USERNAME/YOUR_REPO_NAME.git
   git branch -M main
   git push -u origin main
   ```
3. Open your repository on the GitHub website.
4. Navigate to the **Actions** tab.
5. You will see a workflow named **Build Android Debug APK** running!
6. Once it completes successfully, click on the workflow run, scroll down to **Artifacts**, and download the `antigravity-shell-apk.zip` containing the finished `app-debug.apk`!

## How it works

1. **User Interface**: Beautiful Material 3 UI built with Jetpack Compose.
2. **File Explorer / Picker**: Use the floating menu to choose files or folders. The app reads their content and automatically attaches them to the prompt context.
3. **Local Bridge Server**: Runs a lightweight API bridge between the Android application and your Termux terminal environment.
   - Start the bridge server in Termux:
     ```bash
     agy --server --port 8080
     ```
   - Connect the app to `http://localhost:8080` (or your device's local IP).
   - Chat in real-time, attach workspace files, and watch Antigravity write code on your system!
