package com.mctw.sc

import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.mctw.sc.MainService.MainBinder

class QRShowActivity : BaseActivity(), ServiceConnection {
    private var extraContact: Contact? = null
    private var binder: MainBinder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qrshow)

        if (intent.hasExtra("EXTRA_CONTACT")) {
            extraContact = intent.extras!!["EXTRA_CONTACT"] as Contact?
        }

        setTitle(getString(R.string.scan_invitation))

        bindService(Intent(this, MainService::class.java), this, 0)

        findViewById<View>(R.id.fabPresenter).setOnClickListener {
            startActivity(Intent(this, QRScanActivity::class.java))
            finish()
        }

        findViewById<View>(R.id.fabShare).setOnClickListener {
            try {
                val contact = extraContact ?: binder!!.getSettings().getOwnContact()
                val data = Contact.toJSON(contact, false).toString()
                val i = Intent(Intent.ACTION_SEND)
                i.putExtra(Intent.EXTRA_TEXT, data)
                i.type = "text/plain"
                startActivity(i)
                finish()
            } catch (e: Exception) {
                // ignore
            }
        }

        findViewById<View>(R.id.fabCopy).setOnClickListener {
            try {
                val contact = extraContact ?: binder!!.getSettings().getOwnContact()
                val data = Contact.toJSON(contact, false).toString()
                val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clipData = ClipData.newPlainText("text", data)
                clipboardManager.setPrimaryClip(clipData)
                Toast.makeText(this, "Text copied to clipboard", Toast.LENGTH_LONG).show()
                finish()
            } catch (e: Exception) {
                // ignore
            }
        }


    }

    override fun onDestroy() {
        super.onDestroy()
        if (binder != null) {
            unbindService(this)
        }
    }

    private fun generateQR(contact: Contact) {
        findViewById<TextView>(R.id.contact_name_tv)
            .text = contact.name

        val data = Contact.toJSON(contact, false).toString()
        val multiFormatWriter = MultiFormatWriter()
        val bitMatrix = multiFormatWriter.encode(data, BarcodeFormat.QR_CODE, 1080, 1080)
        val barcodeEncoder = BarcodeEncoder()
        val bitmap = barcodeEncoder.createBitmap(bitMatrix)
        findViewById<ImageView>(R.id.QRView).setImageBitmap(bitmap)

        if (contact.addresses.isEmpty()) {
            Toast.makeText(this, R.string.contact_has_no_address_warning, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
        binder = iBinder as MainBinder

        try {
            val contact = extraContact ?: binder!!.getSettings().getOwnContact()
            generateQR(contact)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onServiceDisconnected(componentName: ComponentName) {
        binder = null
    }

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            finish()
        }
    }
}