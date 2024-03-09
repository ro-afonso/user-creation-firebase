package com.example.myfitnessapp

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.drawToBitmap
import androidx.lifecycle.lifecycleScope
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.myappfirestore.FileDataPart
import com.example.myappfirestore.VolleyFileUploadRequest
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.client.HttpClient
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.client.methods.HttpGet
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.impl.client.DefaultHttpClient
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.sql.Timestamp
import java.time.Instant
import java.util.*


class UserElement(public val id: String, public val firstName: String, public val lastName: String, public val photoReference: String = ""){

    override fun toString(): String {
        return "${lastName}, ${firstName}"
    }
}

class MainActivity : AppCompatActivity() {

    private var idCurrentUser : String? = null
    private var photoReferenceCurrentUser : String? = null
    private var GALLERY_REQUEST_CODE = 13

    private val CAMERA_REQUEST_CODE = 200

    private lateinit var viewImage : ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewImage = findViewById(R.id.imageView)

        //implementation "androidx.lifecycle.lifecycle-runtime-ktx:2.6.8-alpha02"
        //target wsdk 33
        lifecycleScope.launch(){
            //testFireStore()
            showListView()
        }
        findViewById<Button>(R.id.idAdd).setOnClickListener(){
            val uuid = UUID.randomUUID().toString()
            //uploadFile(fileName)
            uploadImage(uuid + ".jpg")

            val firstName = findViewById<TextView>(R.id.idFirstName).text
            val lastName = findViewById<TextView>(R.id.idLastName).text
            //val ageValue = findViewById<TextView>(R.id.idAge).text
            val db = Firebase.firestore
            val user = hashMapOf(
                "firstName" to firstName.toString(),
                "lastName" to lastName.toString(),
                "bornDate" to Timestamp.from(Instant.parse("2002-01-01T08:25:24.00Z")),
                "totalSteps" to 0.0f,
                "totalDistance" to 0.0f,
                "totalCalories" to 0.0,
                "photoReference" to uuid,
                "createdAt" to Timestamp.from(Instant.now()),
                "updateAt" to Timestamp.from(Instant.now())
            )
            lifecycleScope.launch{
                val newUserAdded = db.collection("users").add(user).await()
                idCurrentUser = newUserAdded.id
                photoReferenceCurrentUser = uuid
                showListView()
            }
        }
        findViewById<Button>(R.id.idDelete).setOnClickListener(){
            val db = Firebase.firestore
            if(idCurrentUser != null){
                lifecycleScope.launch{
                    val docRef = db.collection("users").document(idCurrentUser!!).get().await()
                    val photoRef = docRef.get("photoReference").toString()
                    deleteImageFromServer(photoRef)
                    db.collection("users").document(idCurrentUser!!).delete().await()
                    val firstName = findViewById<EditText>(R.id.idFirstName).setText("")
                    val lastName = findViewById<EditText>(R.id.idLastName).setText("")
                    idCurrentUser = null
                    photoReferenceCurrentUser = null
                    viewImage.setImageResource(android.R.color.transparent);
                    showListView()
                }
            }
            else{
                Toast.makeText(applicationContext, "Press an user from the list to delete", Toast.LENGTH_SHORT).show()
            }
        }
        findViewById<Button>(R.id.idUpdate).setOnClickListener(){
            val db = Firebase.firestore
            val firstName = findViewById<EditText>(R.id.idFirstName).text.toString()
            val lastName = findViewById<EditText>(R.id.idLastName).text.toString()
            if(idCurrentUser != null){
                lifecycleScope.launch{
                    //val photoRef = db.collection("users").document(idCurrentUser!!).get().await().get("photoReference").toString()
                    deleteImageFromServer(photoReferenceCurrentUser.toString())

                    uploadImage(photoReferenceCurrentUser + ".jpg")

                    db.collection("users")
                        .document(idCurrentUser!!)
                        .update("firstName", firstName,
                        "lastName", lastName,
                            "photoReference", photoReferenceCurrentUser,
                        "updateAt", Timestamp.from(Instant.now()))
                        .await()
                    showListView()
                }
            }
            else{
                Toast.makeText(applicationContext, "Press an user from the list to edit", Toast.LENGTH_SHORT).show()
            }
        }
        findViewById<Button>(R.id.button_take_photo).setOnClickListener{
            requestPermissions(this, CAMERA_REQUEST_CODE)
        }
        findViewById<Button>(R.id.button_choose_image).setOnClickListener{
            requestPermissions(this, GALLERY_REQUEST_CODE)
        }
    }

    fun requestPermissions(activity: Activity?, requestCode : Int) {
        if( requestCode == GALLERY_REQUEST_CODE){
            ActivityCompat.requestPermissions( activity!!, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), requestCode)
        }
        if(requestCode == CAMERA_REQUEST_CODE){
            ActivityCompat.requestPermissions(activity!!, arrayOf(Manifest.permission.CAMERA), requestCode)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        var granted = true
        if(grantResults.isNotEmpty()){
            grantResults.forEach {
                if (it != PackageManager.PERMISSION_GRANTED){
                    granted = false
                }
            }
        }
        if (!granted){
            Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
        }

        when(requestCode){
            CAMERA_REQUEST_CODE ->{
                //Toast.maketText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
                capturePhotoFromCamera()
                return
            }
            GALLERY_REQUEST_CODE -> {
                //Toast.maketText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
                chooseImageFromGallery()
                return
            }
        }
    }

    private fun capturePhotoFromCamera(){
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE)
    }

    private fun chooseImageFromGallery(){
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        //intent.type = "/image"
        startActivityForResult(intent, GALLERY_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.e("onActivityResult: ","requestCode: ${requestCode} resultCode: ${resultCode}")
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK && requestCode == CAMERA_REQUEST_CODE && data != null){
            viewImage.setImageBitmap(data.extras?.get("data") as Bitmap)
        }

        if(resultCode == Activity.RESULT_OK && requestCode == GALLERY_REQUEST_CODE && data != null){
            val uri: Uri? = data.data
            viewImage.setImageURI(uri)
        }
    }

    suspend fun testFireStore(){
        val db = Firebase.firestore

        val user = hashMapOf(
            "firstName" to "Antonio",
            "lastName" to "Pires",
            "bornDate" to Timestamp.from(Instant.parse("2002-01-01T08:25:24.00Z")),
            "totalSteps" to 0.0f,
            "totalDistance" to 0.0f,
            "totalCalories" to 0.0
        )
        val newUserAdded = db.collection("users").add(user)
        //optional
        .addOnSuccessListener { documentReference ->
            Log.d("store", "DocumentSnapshot added with ID: ${documentReference.id}")
            Toast.makeText(applicationContext, "Success", Toast.LENGTH_SHORT).show()
        }
        .addOnFailureListener { e ->
            Log.d("store","Error adding document", e)
            Toast.makeText(applicationContext, "Error", Toast.LENGTH_SHORT).show()
        }
        .await()

        val queryUser = db.collection("users")
            .whereEqualTo("firstName","Antonio")
            .whereEqualTo("lastName","Pires")
            .get()
            .await()

        val queryUnits = db.collection("measurementType")
            .whereEqualTo("name","Calories")
            .get()
            .await()

        if (queryUser.documents.count() > 0 && queryUnits.documents.count() > 0){

            val idUnit = queryUnits.documents[0].id
            val nameType = queryUnits.documents[0].get("name")
            val units = queryUnits.documents[0].get("units")

            val idUser = queryUser.documents[0].id
            val firstName = queryUser.documents[0].get("name")

            Log.d("fire", "firstName: ${firstName} idUnit:${idUnit} units:${nameType} ${units}")
            val fitnessData = hashMapOf(
                "startDate" to Timestamp.from(Instant.parse("2022-10-18T08:20:01.00Z")),
                "endDate" to Timestamp.from(Instant.parse("2022-10-18T08:35:12.00Z")),
                "typeId" to idUnit,
                "userId" to idUser,
                "value" to 1234
            )
            db.collection("measurements").add(fitnessData).await()
        }
    }

    suspend fun showListView() {
        val db = Firebase.firestore
        val queryUser = db.collection("users")
            .orderBy("updateAt", Query.Direction.DESCENDING)
            .get()
            .await()

        var listItems = ArrayList<UserElement>()

        queryUser.forEach {
            val id = it.id
            val firstName = it.get("firstName").toString()
            val lastName = it.get("lastName").toString()
            val photoReference = it.get("photoReference").toString()
            listItems.add(UserElement(it.id, firstName, lastName, photoReference = photoReference))
        }
        val listView = findViewById<ListView>(R.id.id_listview)

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, listItems)
        listView.adapter = adapter

        listView.setOnItemClickListener() { adapterView, view, position, id ->
            val itemAtPos = adapterView.getItemAtPosition(position)
            val itemIdAtPos = adapterView.getItemIdAtPosition(position)
            val element = adapterView.getItemAtPosition(position) as UserElement
            val myToast = Toast.makeText(
                this,
                "Click on User ${element.firstName} its item id ${element.id}",
                Toast.LENGTH_SHORT
            )
            findViewById<EditText>(R.id.idFirstName).setText(element.firstName)
            findViewById<EditText>(R.id.idLastName).setText(element.lastName)
            loadImageFromServer(element.photoReference)
            idCurrentUser = element.id
            photoReferenceCurrentUser = element.photoReference
            myToast.show()
        }
    }

    fun loadImageFromServer(uuid: String) {
        if(uuid!=="") {
            Thread {
                var mImage: Bitmap?
                val mWebPath = "http://YOUR_SUBDOMAIN.atwebpages.com/download_file.php?id=${uuid}" //Update with your subdomain
                mImage = mLoad(mWebPath)
                var vi = findViewById<ImageView>(R.id.imageView)
                lifecycleScope.launch {
                    vi.setImageBitmap(mImage)
                }
            }.start()
        }
    }
    

    fun deleteImageFromServer(photoRef: String) {
        val posting_url = "http://YOUR_SUBDOMAIN.atwebpages.com/delete_file.php" //Update with your subdomain
        val postRequest: StringRequest = object : StringRequest(
            Method.POST,
            posting_url,
            Response.Listener {
                println("response is: $it")
            },
            Response.ErrorListener {
                println("error is: $it")
            }
        ) {
            //associate params with corresponding tags in the php post
            override fun getParams(): Map<String, String>? {
                val params: MutableMap<String, String> = HashMap()
                params["deleteImage"] = photoRef+".jpg"
                //params.put("2ndParamName","valueoF2ndParam");
                return params
            }
        }
        Volley.newRequestQueue(this).add(postRequest)
    }

    private fun uploadImage(fileName: String) {
        val imageBitMap =  findViewById<ImageView>(R.id.imageView).drawToBitmap()
        val imageData = getFileDataFromDrawable(imageBitMap)

        val postURL = "http://YOUR_SUBDOMAIN.atwebpages.com/upload_file.php" //Update with your subdomain

        val request = object : VolleyFileUploadRequest(
            Method.POST,
            postURL,
            Response.Listener {
                println("response is: $it")
            },
            Response.ErrorListener {
                println("error is: $it")
            }
        ) {
            override fun getByteData(): MutableMap<String, FileDataPart> {
                var params = HashMap<String, FileDataPart>()
                params["file"] = FileDataPart(fileName, imageData!!, "jpeg")

                return params
            }
        }
        Volley.newRequestQueue(this).add(request)
    }

    // Function to establish connection and load image
    private fun mLoad(string: String): Bitmap? {
        val url: URL = mStringToURL(string)!!
        val connection: HttpURLConnection?
        try {
            connection = url.openConnection() as HttpURLConnection
            connection.connect()
            val inputStream: InputStream = connection.inputStream
            val bufferedInputStream = BufferedInputStream(inputStream)
            return BitmapFactory.decodeStream(bufferedInputStream)
        } catch (e: IOException) {
            e.printStackTrace()
            // Toast.makeText(applicationContext, "Error", Toast.LENGTH_LONG).show()
        }
        return null
    }

    fun getFileDataFromDrawable(bitmap: Bitmap): ByteArray? {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 80, byteArrayOutputStream)
        return byteArrayOutputStream.toByteArray()
    }

    // Function to convert string to URL
    private fun mStringToURL(string: String): URL? {
        try {
            return URL(string)
        } catch (e: MalformedURLException) {
            e.printStackTrace()
        }
        return null
    }
}