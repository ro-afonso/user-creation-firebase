# user-creation-firebase
A simple approach to perform CRUD operations with user profiles on an Android app and save them in Firebase and a Linux/Apache Web Server.

The approach is demonstrated in the following video:

https://github.com/ro-afonso/user-creation-firebase/assets/93609933/2dc292d8-837d-4cee-b67e-9cd82b78fac8

# How to run

Follow these steps to perform the operations demonstrated in the video:
1) Create a Firebase account and register your Android app
2) Download the resulting "google-services.json" with your Firestore Database keys and save it in the "app" folder of the Android app
3) Set up a free subdomain on [AwardSpace](https://www.awardspace.com) to create your Linux/Apache web server
4) Move the "download_file.php", "upload_file.php" and "delete_file.php" scripts to your web server
5) Update the "MainActivity.kt" script on the Android app with the paths for each PHP script on your web server
6) Perform the CRUD operations with user profiles
