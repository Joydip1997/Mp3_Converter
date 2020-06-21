package com.androdude.mp3converter.Fragments

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.androdude.mp3converter.ModelClass.appMetaData
import com.androdude.mp3converter.ModelClass.appUpdatedData

import com.androdude.mp3converter.R
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_about.*
import kotlinx.android.synthetic.main.fragment_about.view.*
import kotlinx.android.synthetic.main.update_dialog.view.*

/**
 * A simple [Fragment] subclass.
 */
class AboutFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val layoutView = inflater.inflate(R.layout.fragment_about, container, false)
        openLinks(layoutView)
        layoutView.about_Toolbar.setNavigationIcon(R.drawable.ic_back_icon)
        layoutView.about_Toolbar.setNavigationOnClickListener {
            activity!!.viewPager.currentItem=0
        }



        return layoutView

    }

    private fun openLinks(layoutView: View?) {
        layoutView!!.google_play.setOnClickListener{
            redirectUrl(resources.getString(R.string.googlePlayStore))
        }
        layoutView.github.setOnClickListener{
            redirectUrl(resources.getString(R.string.githubUrl))
        }
        layoutView.youtube.setOnClickListener{
            redirectUrl(resources.getString(R.string.youtube))
        }
        layoutView.website.setOnClickListener{
            redirectUrl(resources.getString(R.string.website))
        }

        //Checking For Updates
        layoutView.check_updates_bttn.setOnClickListener()
        {
            checkIfUpdate()
        }

    }


    //Redirect To Google PlatStore
    private fun redirectUrl(updateUrl: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
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
                    checkAppDetails(version,obj.app,myref2)


                }

            }

        })
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
        else
        {
            Toast.makeText(activity!!,"App Is Updated",Toast.LENGTH_SHORT).show()
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


}
