package ph.christian.happyplaces.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.location.Location
import android.location.LocationManager
import android.location.LocationRequest
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.android.synthetic.main.activity_add_happy_place.*
import kotlinx.android.synthetic.main.activity_happy_place_detail.*
import kotlinx.android.synthetic.main.item_happy_place.*
import ph.christian.happyplaces.Database.DatabaseHandler
import ph.christian.happyplaces.R
import ph.christian.happyplaces.models.HappyPlaceModel
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*


class AddHappyPlaceActivity : AppCompatActivity(), View.OnClickListener {

    private var cal = Calendar.getInstance()
    private lateinit var dateSetListener: DatePickerDialog.OnDateSetListener
    private var saveImageToInternalStorage : Uri? = null
    private var mLatitude: Double = 0.0
    private var mLongitude : Double = 0.0
    private var mHappyPlaceDetail: HappyPlaceModel? = null
    private lateinit var mFusedLocationClient: FusedLocationProviderClient


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_happy_place)
        setSupportActionBar(toolbar_add_place)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        toolbar_add_place?.setNavigationOnClickListener {
            onBackPressed()
        }



        if(!Places.isInitialized()){
            Places.initialize(this@AddHappyPlaceActivity,
                resources.getString(R.string.google_map_api_key))
        }

        if(intent.hasExtra(MainActivity.EXTRA_PLACE_DETAILS)) {
            mHappyPlaceDetail = intent.getSerializableExtra(
                MainActivity.EXTRA_PLACE_DETAILS
            ) as HappyPlaceModel
        }
        dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            cal.set(Calendar.MONTH, month)

            updateDateView()
        }
        updateDateView()

        if (mHappyPlaceDetail != null) {
            supportActionBar?.title = "Edit Happy Place"

            et_title.setText(mHappyPlaceDetail!!.title)
            et_description.setText(mHappyPlaceDetail!!.description)
            et_date.setText(mHappyPlaceDetail!!.date)
            et_location.setText(mHappyPlaceDetail!!.location)
            mLatitude = mHappyPlaceDetail!!.latitude
            mLongitude = mHappyPlaceDetail!!.longitude

            saveImageToInternalStorage = Uri.parse(mHappyPlaceDetail!!.image)

            aiv_photos.setImageURI(saveImageToInternalStorage)

            btn_save.text = "UPDATE"
        }

        et_date.setOnClickListener(this)
        tv_add_image.setOnClickListener(this)
        btn_save.setOnClickListener(this)
        et_location.setOnClickListener(this)
        tv_select_current_location.setOnClickListener(this)
    }
    @SuppressLint("MissingPermission")
    private fun requestNewLocationData(){
        val mLocationRequest = com.google.android.gms.location.LocationRequest()
        mLocationRequest.priority = LocationRequest.QUALITY_HIGH_ACCURACY
        mLocationRequest.interval = 0
        mLocationRequest.numUpdates = 1

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mFusedLocationClient.requestLocationUpdates(
            mLocationRequest, mLocationCallback , Looper.getMainLooper())
        }

    private var mLocationCallback = object : LocationCallback(){
        override fun onLocationResult(locationResult: LocationResult) {
            val mLastLocation : Location = locationResult.lastLocation
            mLatitude = mLastLocation.latitude
            Log.i("Current Latitude", "$mLatitude")
            mLongitude = mLastLocation.longitude
            Log.i("Current Longitude", "$mLongitude")

        }
    }

    private fun isLocationEnabled(): Boolean{
            val locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }


    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.et_date -> {
                DatePickerDialog(
                    this@AddHappyPlaceActivity,
                    dateSetListener,
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
                ).show()
            }
            R.id.tv_add_image -> {
                val pictureDialog = AlertDialog.Builder(this)
                pictureDialog.setTitle("Select Action")
                val pictureDialogItems = arrayOf(
                    "Select photo from Gallery",
                    "Capture photo from Camera"
                )
                pictureDialog.setItems(pictureDialogItems) { _, which ->
                    when (which) {
                        0 -> choosePhotoFromGallery()
                        1 -> takePhotoFromCamera()
                    }
                }
                pictureDialog.show()

            }
            R.id.btn_save -> {
                when {
                    et_title.text.isNullOrEmpty() -> {
                        Toast.makeText(this, "Please Enter a title", Toast.LENGTH_SHORT).show()
                    }
                    et_description.text.isNullOrEmpty() -> {
                        Toast.makeText(this, "Please Enter a description", Toast.LENGTH_SHORT)
                            .show()
                    }
                    et_location.text.isNullOrEmpty() -> {
                        Toast.makeText(this, "Please Enter a location", Toast.LENGTH_SHORT).show()
                    }
                    saveImageToInternalStorage == null -> {
                        Toast.makeText(this, "Please add a image", Toast.LENGTH_SHORT).show()
                    }
                    else -> {

                        val happyPlaceModel = HappyPlaceModel(
                            if (mHappyPlaceDetail == null) 0 else mHappyPlaceDetail!!.id,
                            et_title.text.toString(),
                            saveImageToInternalStorage.toString(),
                            et_description.text.toString(),
                            et_date.text.toString(),
                            et_location.text.toString(),
                            mLatitude,
                            mLongitude
                        )
                        val dbHandler = DatabaseHandler(this)

                        if (mHappyPlaceDetail == null) {
                            val addHappyPlace = dbHandler.addHappyPlace(happyPlaceModel)
                            if (addHappyPlace > 0) {
                                setResult(Activity.RESULT_OK)
                                finish()
                            }
                            } else {
                            val updateHappyPlace = dbHandler.updateHappyPlace(happyPlaceModel)
                            if (updateHappyPlace > 0) {
                                setResult(Activity.RESULT_OK)
                                finish()
                            }
                        }
                    }
                }
            }
            R.id.et_location -> {
                try {
                    val fields = listOf(
                        Place.Field.ID, Place.Field.NAME,Place.Field.LAT_LNG,
                        Place.Field.ADDRESS
                    )
                    val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN,
                    fields).build(this@AddHappyPlaceActivity)
                    startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE)

                }catch (e:Exception){
                    e.printStackTrace()
                }
            }
            R.id.tv_select_current_location -> {
                if (!isLocationEnabled()){
                    Toast.makeText(this, "Your Location provider is turned off", Toast.LENGTH_SHORT)
                        .show()
                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startActivity(intent)
                }else {
                    Dexter.withActivity(this)
                        .withPermissions(
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                        .withListener(object : MultiplePermissionsListener {
                            override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                                if (report!!.areAllPermissionsGranted()) {
                                   requestNewLocationData()
                                }
                            }

                            override fun onPermissionRationaleShouldBeShown(
                                permissions: MutableList<PermissionRequest>?, token: PermissionToken?
                            ) {
                                showRationaleDialogPermissions()
                            }
                        }).onSameThread()
                        .check()
                }
            }
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == GALLERY) {
                if (data != null) {

                    val contentURI = data.data
                    try {
                        @Suppress("DEPRECATION")
                        val selectedImageBitmap = MediaStore.Images.Media.getBitmap(
                            this.contentResolver, contentURI)

                      saveImageToInternalStorage =
                            saveImageToInternalStorage(selectedImageBitmap)
                        Log.e("Saved Image : ", "Path :: $saveImageToInternalStorage")

                       aiv_photos.setImageBitmap(selectedImageBitmap)

                    } catch (e: IOException) {
                        e.printStackTrace()
                        Toast.makeText(
                            this@AddHappyPlaceActivity, "Failed to upload",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                } else if (requestCode == CAMERA) {
                    val thumbnail: Bitmap = data!!.extras!!.get("data") as Bitmap

                saveImageToInternalStorage =
                    saveImageToInternalStorage(thumbnail)
                Log.e("Saved Image : ", "Path :: $saveImageToInternalStorage")

                     aiv_photos.setImageBitmap(thumbnail)
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
            Log.e("Cancelled", "Cancelled")

        }else if (resultCode == PLACE_AUTOCOMPLETE_REQUEST_CODE){
            val place :Place = Autocomplete.getPlaceFromIntent(data!!)
            mLatitude = place.latLng!!.latitude
            mLongitude = place.latLng!!.longitude
        }
    }

    private fun updateDateView(){
        val myFormat = "dd.MM.yyyy"
        val sdf = SimpleDateFormat(myFormat, Locale.getDefault())
        et_date.setText(sdf.format(cal.time).toString())

    }
    private fun choosePhotoFromGallery() {
        Dexter.withActivity(this)
            .withPermissions(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {

                    // Here after all the permission are granted launch the gallery to select and image.
                    if (report!!.areAllPermissionsGranted()) {

                        val galleryIntent = Intent(
                            Intent.ACTION_PICK,
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        )

                        startActivityForResult(galleryIntent, GALLERY)
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?, token: PermissionToken?
                ) {
                    showRationaleDialogPermissions()
                }
            }).onSameThread()
            .check()
    }

    private fun takePhotoFromCamera() {
        Dexter.withActivity(this)
            .withPermissions(
                Manifest.permission.CAMERA
            )
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {

                    if (report!!.areAllPermissionsGranted()) {
                        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                        @Suppress("DEPRECATION")
                        startActivityForResult(cameraIntent, CAMERA)
                    }
                }
                override fun onPermissionRationaleShouldBeShown(
                    permissions: List<PermissionRequest>?, token: PermissionToken?
                ) {
                    showRationaleDialogPermissions()
                }
            }).onSameThread()
            .check()

    }


    private fun showRationaleDialogPermissions(){
        AlertDialog.Builder(this).setMessage("It looks like you have denied me! Be my guest " +
                "and set it up yourself! Enabled it under Application Settings!")
            .setPositiveButton("Go to Setting"){
                _, _, ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data  = uri
                    startActivity(intent)
                }catch (e : ActivityNotFoundException){
                    e.printStackTrace()
                    Toast.makeText(this, "FAILED!", Toast.LENGTH_SHORT).show()

            }
            }
            .setNegativeButton("Cancel"){dialog, _ ->
                dialog.dismiss()
            }.show()

    }

    private fun saveImageToInternalStorage(bitmap: Bitmap): Uri {

        val wrapper = ContextWrapper(applicationContext)
        var file = wrapper.getDir(IMAGE_DIRECTORY, Context.MODE_PRIVATE)

        file = File(file, "${UUID.randomUUID()}.png")

        try {
            val stream: OutputStream = FileOutputStream(file)

            bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream)

            stream.flush()
            stream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return Uri.parse(file.absolutePath)
    }

    companion object{
        private const val GALLERY = 1
        private const val CAMERA = 2
        private const val IMAGE_DIRECTORY = "HappyPlacesImages"
        private const val PLACE_AUTOCOMPLETE_REQUEST_CODE = 3
    }

}
