 package ph.christian.happyplaces.activities

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_main.*
import ph.christian.happyplaces.Database.DatabaseHandler
import ph.christian.happyplaces.R
import ph.christian.happyplaces.adapters.HappyPlacesAdapter
import ph.christian.happyplaces.models.HappyPlaceModel
import ph.christian.happyplaces.utils.SwipeToDeleteCallback
import ph.christian.happyplaces.utils.SwipeToEditCallback


 class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fabAddHappyPlace.setOnClickListener {
            val intent = Intent(this@MainActivity, AddHappyPlaceActivity::class.java)
            startActivityForResult(intent, ADD_PLACE_ACTIVITY_REQUEST_CODE)
        }
        getHappyPlaceListFromLocalDB()
    }

    private fun getHappyPlaceListFromLocalDB(){
        val dbHandler = DatabaseHandler(this)
        val getHappyPlaceList = dbHandler.getHappyPlaceList()

        if(getHappyPlaceList.size > 0){
            rv_happy_place_list.visibility = View.VISIBLE
            tv_no_happy_place.visibility = View.GONE
            setupHappyPlaceRecyclerView(getHappyPlaceList)
        }else{
            rv_happy_place_list.visibility = View.GONE
            tv_no_happy_place.visibility = View.VISIBLE

        }
    }

     private fun setupHappyPlaceRecyclerView(happyPlaceList: ArrayList<HappyPlaceModel>){
         rv_happy_place_list.layoutManager =
             LinearLayoutManager(this)
         rv_happy_place_list.setHasFixedSize(true)

        val placeAdapter = HappyPlacesAdapter(this, happyPlaceList)
         rv_happy_place_list.adapter = placeAdapter

         placeAdapter.setOnclickListener(object : HappyPlacesAdapter.OnClickListener{
             override fun onClick(position: Int, model: HappyPlaceModel) {
                val intent = Intent(this@MainActivity,
                    HappyPlaceDetailActivity::class.java)
                 intent.putExtra(EXTRA_PLACE_DETAILS , model)

                 startActivity(intent)
             }
         })
        val editSwipeHandler = object: SwipeToEditCallback(this) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder,
                                  direction: Int) {
                val adapter = rv_happy_place_list.adapter as HappyPlacesAdapter
                adapter.notifyEditItem(this@MainActivity ,
                    viewHolder.adapterPosition, ADD_PLACE_ACTIVITY_REQUEST_CODE)
            }
        }
         val editItemTouchHelper = ItemTouchHelper(editSwipeHandler)
         editItemTouchHelper.attachToRecyclerView(rv_happy_place_list)

         val deleteSwipeHandler = object: SwipeToDeleteCallback(this) {
             override fun onSwiped(viewHolder: RecyclerView.ViewHolder,
                                   direction: Int) {
                 val adapter = rv_happy_place_list.adapter as HappyPlacesAdapter
                 adapter.removeAt(viewHolder.adapterPosition)

                 getHappyPlaceListFromLocalDB()
             }
         }
         val deleteItemTouchHelper = ItemTouchHelper(deleteSwipeHandler)
         deleteItemTouchHelper.attachToRecyclerView(rv_happy_place_list)

     }

     override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
         super.onActivityResult(requestCode, resultCode, data)
         if (requestCode ==  ADD_PLACE_ACTIVITY_REQUEST_CODE){
             if (resultCode == Activity.RESULT_OK){
                 getHappyPlaceListFromLocalDB()
             }else{
                 Log.e("Activity" , "Cancelled or Back pressed")
             }
         }
     }

     companion object {
         var ADD_PLACE_ACTIVITY_REQUEST_CODE = 1
         var EXTRA_PLACE_DETAILS = "Extra_place_details"
     }
}