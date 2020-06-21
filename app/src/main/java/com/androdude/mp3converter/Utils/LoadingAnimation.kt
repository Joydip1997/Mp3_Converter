package com.androdude.mp3converter.Utils

import android.app.ProgressDialog
import android.content.Context
import com.androdude.mp3converter.R


class LoadingAnimation {

    var  progresDialog: ProgressDialog? = null

    var context : Context?= null

    fun get(context: Context)
    {
        this.context=context
    }

    fun start()
    {
        progresDialog= ProgressDialog(context)
        progresDialog!!.show()


        progresDialog!!.setContentView(R.layout.loading_layout)




        progresDialog!!.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    fun stop()
    {

        progresDialog!!.hide()
    }
}