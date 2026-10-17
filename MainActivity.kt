package com.example.apkmerger

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import java.io.File
import kotlin.concurrent.thread

class MainActivity : ComponentActivity() {
    private lateinit var status: TextView
    private lateinit var pick: Button
    private var result: File? = null
    private var baseName = "merged"

    private val openDoc = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) convert(uri)
    }
    private val saveDoc = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.android.package-archive")
    ) { uri -> if (uri != null) save(uri) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (24 * resources.displayMetrics.density).toInt()
        status = TextView(this).apply { text = "选择 .apks / .apkm / .xapk 文件"; textSize = 16f }
        pick = Button(this).apply {
            text = "选择文件并转换"
            setOnClickListener { openDoc.launch(arrayOf("*/*")) }
        }
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
            addView(pick)
            addView(status)
        })
    }

    private fun convert(uri: Uri) {
        val name = contentResolver.query(uri, null, null, null, null)?.use {
            if (it.moveToFirst()) it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)) else null
        } ?: "input.apks"
        val ext = name.substringAfterLast('.', "apks")
        baseName = name.substringBeforeLast('.')
        pick.isEnabled = false
        status.text = "转换中，大文件需要一些时间…"
        thread {
            try {
                val input = File(cacheDir, "in.$ext").also { it.delete() }
                contentResolver.openInputStream(uri)!!.use { i -> input.outputStream().use { o -> i.copyTo(o) } }
                val out = Converter.convert(this, input)
                input.delete()
                result = out
                runOnUiThread {
                    status.text = "转换完成，请选择保存位置"
                    pick.isEnabled = true
                    saveDoc.launch("$baseName.apk")
                }
            } catch (e: Throwable) {
                runOnUiThread { status.text = "失败：${e.message}"; pick.isEnabled = true }
            }
        }
    }

    private fun save(uri: Uri) {
        val f = result ?: return
        contentResolver.openOutputStream(uri)!!.use { o -> f.inputStream().use { it.copyTo(o) } }
        status.text = "已保存。到文件管理器点开 $baseName.apk 即可安装"
    }
}
