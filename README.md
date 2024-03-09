# user-creation-firebase
A simple approach to perform CRUD operations with user profiles on an Android app and save them in Firebase and a Linux/Apache Web Server.

The approach is demonstrated in the following video:

https://github.com/ro-afonso/user-creation-firebase/assets/93609933/2dc292d8-837d-4cee-b67e-9cd82b78fac8

# How to run

Follow these steps to perform the operations demonstrated in the video:
1) Install [Android Studio](https://developer.android.com/studio) on your computer
2) Create a Firebase account and register your Android app
3) Download the resulting "google-services.json" with your Firestore Database keys and save it in the "app" folder of the Android app
4) Set up a free subdomain on [AwardSpace](https://www.awardspace.com) to create your Linux/Apache web server
5) Move the "download_file.php", "upload_file.php" and "delete_file.php" scripts to your web server
6) Update the "MainActivity.kt" script on the Android app with the paths for each PHP script on your web server
7) Activate developer mode on your Android device and connect it to your computer through a USB cable
8) Run the app to install it on your device and disconnect the cable once finished
9) Turn on Wi-fi/Mobile Data on your device and perform the CRUD operations with user profiles
