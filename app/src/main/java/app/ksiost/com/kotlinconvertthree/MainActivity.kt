package app.ksiost.com.kotlinconvertthree

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.MifareUltralight
import android.nfc.tech.Ndef
import android.nfc.tech.NfcA
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    private var isNfcSupported = true
    private var nfcAdapter: NfcAdapter? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        nfcAdapter = NfcAdapter.getDefaultAdapter(applicationContext)
        textView.text = "当前NFC没有开启"
        nfcOnBtn.setOnClickListener {
            initNFC()
        }
        val x = A("", "", arrayOf(""), byteArrayOf())

    }

    fun initNFC() {
        isNfcSupported = true
        if (nfcAdapter == null) {
            textView.text = "当前设备不支持NFC"
            isNfcSupported = false
        } else {
            if (!nfcAdapter!!.isEnabled) {
                textView.text = "请先在系统设置中启用NFC功能"
                isNfcSupported = false
            }
        }
        if (isNfcSupported) {
            setNfcTAG()
        }

    }

    private lateinit var pi: PendingIntent
    private lateinit var tagDetected: IntentFilter
    private fun setNfcTAG() {
        // 初始化PendingIntent，当有NFC设备连接上的时候，就交给当前Activity处理
        pi = PendingIntent.getActivity(this, 0, Intent(this, javaClass)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0)
        // 新建IntentFilter，使用的是第二种的过滤机制
        tagDetected = IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)
        tagDetected.addCategory(Intent.CATEGORY_DEFAULT)
    }

    override fun onResume() {
        super.onResume()
        if (isNfcSupported) {
            startNFCListener()
            if (NfcAdapter.ACTION_TECH_DISCOVERED == this.intent.action) {
                processIntent(intent)
            }
        } else {
            return
        }
    }

    override fun onPause() {
        super.onPause()
        if (isNfcSupported) {
            stopNFCListener()
        }
    }

    private fun startNFCListener() {
        nfcAdapter?.enableForegroundDispatch(this, pi, arrayOf(tagDetected), null)
    }

    private fun stopNFCListener() {
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (NfcAdapter.ACTION_TECH_DISCOVERED == intent?.action) {
            processIntent(intent)
        }
    }

    lateinit var tagFromIntent: Tag

    private fun processIntent(intent: Intent) {
        if (!isNfcSupported) {
            return
        } else {
            tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            textView.setTextColor(Color.CYAN)
            textView.text = textView.text.toString() + "\n找到NFC卡片,ID是 " + tagFromIntent.id + "\n"
            var prefix = "android.nfc.tech"
            val techList = tagFromIntent.techList
            var cardType = ""
            for (techOne in techList) {
                if (techOne == NfcA::class.java.name) {
                    var nfcA = NfcA.get(tagFromIntent)
                    if (cardType == "") {
                        cardType = "卡片不支持NDEF"
                    }
                } else if (techOne == MifareUltralight::class.java.name) {
                    val m = MifareUltralight.get(tagFromIntent)
                    var lightType = ""
                    when (m.type) {
                        MifareUltralight.TYPE_ULTRALIGHT -> lightType = "MifareUltralight"
                        MifareUltralight.TYPE_ULTRALIGHT_C -> lightType = "MifareUltralight_C"
                    }
                    cardType = lightType + "卡片类型"

                    val ndef = Ndef.get(tagFromIntent)
                    cardType += "最大数据尺寸：" + ndef.maxSize + "\n"
                }
            }
            textView.text = textView.text.toString() + cardType
        }
    }

    private fun readNfc(tag: Tag?): String? {
        if (tag == null) {
            textView.text = "设备断开了连接，请重新连接！"
            return null
        } else {
            val ndefRead = Ndef.get(tag)
            ndefRead.connect()
            val ndefMessage = ndefRead.ndefMessage
            val byteArray = ndefMessage.toByteArray()
            val str = byteArray.toString()
            ndefRead.close()
            return str
        }
    }

    private fun writeNfc(tag: Tag?) {
        if (tag == null) {
            textView.text = "设备断开了连接，请重新连接！"
        } else {
            val records = arrayOf(creatRecords())
            val ndefWrite = Ndef.get(tag)
            ndefWrite.connect()
            val ndefMessage = NdefMessage(records)
            if(ndefWrite.isWritable){
                ndefWrite.writeNdefMessage(ndefMessage)
                textView.text = "写入数据成功！"
            }else{
                textView.text = "无法写入数据！"
            }
            ndefWrite.close()
        }
    }

    private fun creatRecords(): NdefRecord {

        val msg = "BEGIN:VCARD\n" + "VERSION:2.1\n" + "test message!" + "END:VCARD"
        val bytes: ByteArray = msg.toByteArray()
        return NdefRecord(NdefRecord.TNF_MIME_MEDIA, "text/X-vCard".toByteArray(), byteArrayOf(), bytes)
    }


    inner class A(a: String?, b: String, c: Array<String>, d: ByteArray)
}
