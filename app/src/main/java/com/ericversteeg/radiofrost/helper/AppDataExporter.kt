package com.ericversteeg.radiofrost.helper

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.ericversteeg.radiofrost.model.SessionSettings
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter

object AppDataExporter {

    fun export(context: Context, email: String) {
        val exportFile = saveExportFile(context)

        val intent = Intent(Intent.ACTION_SEND)

        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
        intent.putExtra(Intent.EXTRA_SUBJECT, "Canvas Data")
        intent.putExtra(Intent.EXTRA_TEXT, "Attached is your canvas. To import this back into the app on any device:\n\n1.) Go to pastebin.com\n2.) " +
                "Paste the contents of your data file into Pastebin under \"New Paste\"\n3.) Scroll to the bottom of the webpage and click \"Create New Paste\"4.) " +
                "Copy the Pastebin url (with a now included code) from the url bar in your web browser to the app in Options -> Import Canvas")

        val uri = FileProvider.getUriForFile(
            context,
            "com.ericversteeg.liquidocean.fileprovider",
            exportFile
        )

        intent.putExtra(Intent.EXTRA_STREAM, uri)

        intent.type = "message/rfc822"

        context.startActivity(Intent.createChooser(intent, "Choose an email client:"))
    }

    private fun saveExportFile(context: Context): File {
        val cacheDir = context.cacheDir

        val outputFile = File(context.cacheDir.absolutePath + File.separator.toString() + "canvas_data.json")

        val fos = FileOutputStream(outputFile)
        val pw = PrintWriter(fos)

        val sp = SessionSettings.instance.getSharedPrefs(context)

        pw.println(sp.getString("arr_canvas", ""))

        pw.flush()
        pw.close()
        fos.close()

        return outputFile
    }
}