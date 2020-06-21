package com.androdude.mp3converter.Fragments

import android.Manifest
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.fragment.app.Fragment
import com.aditya.filebrowser.Constants
import com.aditya.filebrowser.FileBrowser
import com.androdude.mp3converter.ModelClass.appMetaData
import com.androdude.mp3converter.ModelClass.appUpdatedData
import com.androdude.mp3converter.R
import com.androdude.mp3converter.RealPathUtil
import com.androdude.mp3converter.Utils.LoadingAnimation
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.InterstitialAd
import com.google.android.gms.ads.MobileAds
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_main.*
import kotlinx.android.synthetic.main.fragment_main.view.*
import kotlinx.android.synthetic.main.update_dialog.view.*
import nl.bravobit.ffmpeg.ExecuteBinaryResponseHandler
import nl.bravobit.ffmpeg.FFmpeg
import nl.bravobit.ffmpeg.exceptions.FFmpegCommandAlreadyRunningException
import java.io.File
import java.util.*

/**
 * A simple [Fragment] subclass.
 */
class MainFragment : Fragment() {

    val REQUEST_CODE = 1000
    val REQUEST_CAMERA_CODE = 2000
    val REQUEST_VIDEO_CODE = 3000
    var videoUri : Uri ?= null
    val pb = LoadingAnimation()
    lateinit var mInterstitialAd: InterstitialAd
    var flag = false


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val layoutView = inflater.inflate(R.layout.fragment_main, container, false)

        pb.get(activity!!)
        createDirIfNotExists("/mp3converter")
        checkIfUpdate()
        loadBanner(layoutView)
        verifyPermissions()
        loadffmpeg(activity!!)
        openAds()


        layoutView.pick_video.setOnClickListener {



            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            {
                if(checkSelfPermission(activity!!,Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(activity!!,Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(activity!!,Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED )
                {
                    pickVideos()
                }else
                {
                    verifyPermissions()
                }
            }
            else
            {
                pickVideos()
            }


        }
        layoutView.open_camera.setOnClickListener{


            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            {
                if(checkSelfPermission(activity!!,Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(activity!!,Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(activity!!,Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED )
                {
                    openVideoCamera()
                }else
                {
                    verifyPermissions()
                }
            }
            else
            {
                openVideoCamera()
            }

        }
        layoutView.about_fragment.setOnClickListener{

            activity!!.viewPager.currentItem=1
        }
        layoutView.convert.setOnClickListener {

            //Load Ads
            if (mInterstitialAd.isLoaded) {
                mInterstitialAd.show()
            } else {
                Log.d("TAG", "The interstitial wasn't loaded yet.")
            }

            if(videoUri != null)
            {
                val rand = Random()
                var num = rand.nextInt(10000)
                num+=1
                val file_name = "converted$num"
                val complexCommand = arrayOf("-y", "-i", RealPathUtil().getRealPath(activity!!,videoUri!!), "/storage/emulated/0/mp3converter/${file_name}.mp3")
                execFfmpeg(activity!!,complexCommand)
            }
            else
            {
                Toast.makeText(activity!!,"Please Select A Video",Toast.LENGTH_SHORT).show()
            }
        }

        layoutView.open_folder.setOnClickListener {
            createDirIfNotExists("/mp3converter")
            getMp3Files()

        }


        return layoutView

    }



    //Open Ads
    fun openAds()
    {
        MobileAds.initialize(activity) {}
        mInterstitialAd = InterstitialAd(activity)
        mInterstitialAd.adUnitId = "ca-app-pub-5483101987186950/4134821674"
        mInterstitialAd.loadAd(AdRequest.Builder().build())
        if (mInterstitialAd.isLoaded) {
            mInterstitialAd.show()
        } else {
            Log.d("ADD", "The interstitial wasn't loaded yet.")
        }
    }

    //Load Banner Ads
    fun loadBanner(layoutView: View?)
    {
        MobileAds.initialize(activity) {}
        val mAdView = layoutView!!.adView
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)
    }


    //Opening The Folder Converted The Converted Mp3 Files
    fun getMp3Files()
    {
        val i = Intent(
            activity!!,
            FileBrowser::class.java
        ) //works for all 3 main classes (i.e FileBrowser, FileChooser, FileBrowserWithCustomHandler)

        i.putExtra(
            Constants.INITIAL_DIRECTORY,
            File(
                Environment.getExternalStorageDirectory().absolutePath,
                "mp3converter"
            ).absolutePath
        )

        i.putExtra(Constants.ALLOWED_FILE_EXTENSIONS, "mp3");

        startActivityForResult(i, 5000)


    }

    //Check If Update Is Available
    private fun checkIfUpdate() {
        var version : String ?= null


        try {
            val pInfo: PackageInfo = activity!!.getPackageManager().getPackageInfo(activity!!.packageName, 0)
            version = pInfo.versionName
            val versionNum = pInfo.versionCode
            checkNewVersion(version)



        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
    }

    //Check The App Details
    private fun checkAppDetails(version: String?, app: String?, myref2: DatabaseReference) {
        Log.i("VER",version)
        if(version!=app)
        {
            myref2.addValueEventListener(object : ValueEventListener
            {
                override fun onCancelled(p0: DatabaseError) {

                }

                override fun onDataChange(p0: DataSnapshot) {
                    for(k in p0.children)
                    {
                        val obj = k.getValue(appUpdatedData::class.java)
                        updateDialog(activity!!,obj!!.t1,obj.t2,obj.t3,obj.link)
                    }
                }

            })


        }


    }



    //Opening Intent TO Update The App
    fun openUpdateUrl(context: Context, url : String?)
    {
        var webpage = Uri.parse(url)

        if (!url!!.startsWith("http://") && !url.startsWith("https://")) {
            webpage = Uri.parse("http://$url")
        }

        val intent = Intent(Intent.ACTION_VIEW, webpage)
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        }
    }


    //Update Dialog
    fun updateDialog(context: Context, t1 : String?, t2 : String?, t3 : String?, link : String?)
    {

        val alertDialog = AlertDialog.Builder(context)
        val mView = layoutInflater.inflate(R.layout.update_dialog,null,false)




        alertDialog.setView(mView)
        val AlertDialog = alertDialog.create()


        mView.update_now_bttn.setOnClickListener {
            redirectStore(link!!)
            AlertDialog.dismiss()
        }
        mView.cancel_bttn.setOnClickListener {
            AlertDialog.dismiss()
        }
        mView.t1.setOnClickListener {

        }
        mView.version_num.setOnClickListener {

        }
        mView.view.setOnClickListener {

        }

        mView.update1_tv.setText(t1)
        mView.update2_tv.setText(t2)
        mView.update3_tv.setText(t3)
        AlertDialog.show()


    }

    //Redirect To Google PlatStore
    private fun redirectStore(updateUrl: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    //Open Video Camera
    private fun openVideoCamera() {
        val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        startActivityForResult(intent,REQUEST_VIDEO_CODE)
    }

    //Check Permissions
    fun verifyPermissions() {
       if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
       {
           if(checkSelfPermission(activity!!,Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED ||
               checkSelfPermission(activity!!,Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED ||
                   checkSelfPermission(activity!!,Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED )
           {

           }else
           {
               val permissions = arrayOf(Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE)
               requestPermissions(permissions,5000)
           }
       }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == 5000)  {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                flag=true
                Toast.makeText(activity!!, "Permission GRANTED", Toast.LENGTH_SHORT).show();
            } else {
                flag=false
                Toast.makeText(activity!!, "Please Enable The Permissions", Toast.LENGTH_SHORT).show();
            }
        }
    }

    //Load FFMPEG Library
    fun loadffmpeg(context: Context?) {
        if (FFmpeg.getInstance(activity!!).isSupported()) {
            // ffmpeg is supported
        } else {
            // ffmpeg is not supported
        }
    }
    //Exec FFMPEG Library
    fun execFfmpeg(context: Context?, cmd: Array<String?>?
    ) {
        val ffmpeg = FFmpeg.getInstance(context)
        try {
            // to execute "ffmpeg -version" command you just need to pass "-version"
            ffmpeg.execute(cmd, object : ExecuteBinaryResponseHandler() {
                override fun onStart() {
                    pb.start()
                }
                override  fun onProgress(message: String?) {}
                override  fun onFailure(message: String?) {
                    pb.stop()
                }
                override  fun onSuccess(message: String?) {
                    pb.stop()
                    getMp3Files()
                }
                override  fun onFinish() {}
            })
        } catch (e: FFmpegCommandAlreadyRunningException) {
            // Handle if FFmpeg is already running
        }
    }

    //Pick Vidoe From sdcard
    fun pickVideos() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "video/*"
        startActivityForResult(intent, REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            videoUri = data.data
            val path = RealPathUtil().getRealPath(activity!!, videoUri!!)
            var split = path!!.split("/")

            video_name.setText(getNameFromURI(videoUri))

        }
        if (requestCode == REQUEST_VIDEO_CODE && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            videoUri = data.data
            val path = RealPathUtil().getRealPath(activity!!, videoUri!!)
            var split = path!!.split("/")

            video_name.setText(getNameFromURI(videoUri))

        }

        if (requestCode === 5000 && data != null) {
            if (resultCode === RESULT_OK) {
                val selectedFiles: ArrayList<Uri> =
                    data.getParcelableArrayListExtra(Constants.SELECTED_ITEMS)
            }
        }
    }

    //Get File Name
    fun getNameFromURI(uri: Uri?): String? {
        val c: Cursor? = activity!!.contentResolver.query(uri!!, null, null, null, null)
        c!!.moveToFirst()
        return c.getString(c.getColumnIndex(OpenableColumns.DISPLAY_NAME))
    }




    //Check The App Version
    fun checkNewVersion(version : String)
    {
        val myref1 = FirebaseDatabase.getInstance().getReference("updates").child("child")
        val myref2 = FirebaseDatabase.getInstance().getReference("updates").child("whatsnew")
        myref1.addValueEventListener(object : ValueEventListener
        {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {

                for(c in p0.children)
                {
                    val obj = c.getValue(appMetaData::class.java)
                    println(obj!!.app)
                    checkAppDetails(version,obj!!.app,myref2)


                }

            }

        })
    }



    //Create Directory If Not Exist
    fun createDirIfNotExists(path: String?): Boolean {
        var ret = true
        val file =
            File(Environment.getExternalStorageDirectory(), path)
        if (!file.exists()) {
            if (!file.mkdirs()) {
                Log.e("TravellerLog :: ", "Problem creating Image folder")
                ret = false
            }
        }
        return ret
    }

}
