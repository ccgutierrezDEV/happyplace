package ph.christian.happyplaces.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.activity_map.*
import ph.christian.happyplaces.R
import ph.christian.happyplaces.models.HappyPlaceModel

class MapActivity : AppCompatActivity(), OnMapReadyCallback{

    private var mHappyPlaceDetail: HappyPlaceModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        if (intent.hasExtra(MainActivity.EXTRA_PLACE_DETAILS)){
            mHappyPlaceDetail = intent.getSerializableExtra(
                MainActivity.EXTRA_PLACE_DETAILS) as HappyPlaceModel
        }
        if (mHappyPlaceDetail != null){
            setSupportActionBar(toolbar_map)
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.title = mHappyPlaceDetail!!.title

            toolbar_map.setNavigationOnClickListener {
                onBackPressed()
            }
            val supportMapFragment: SupportMapFragment =
                supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
            supportMapFragment.getMapAsync(this)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        val position = LatLng(mHappyPlaceDetail!!.latitude, mHappyPlaceDetail!!.longitude)
        googleMap!!.addMarker(MarkerOptions().position(position).title(mHappyPlaceDetail!!.location))
        val newLatLng = CameraUpdateFactory.newLatLngZoom(position, 15f)
        googleMap.animateCamera(newLatLng)
    }

}