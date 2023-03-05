package com.example.shandilya.encrypto

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import javax.crypto.SecretKey
import java.security.Key
import android.util.Base64
import android.util.Log
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class EncryptionActivity : AppCompatActivity() {

    private var key: EditText?=null
    private var encryptData: EditText?=null
    private  var image: TextView?=null
    private  var encryptBtn: Button?=null
    private var string: String?=""
    //for storing graphics
    private var bitMap: Bitmap?=null
    private  var imageView: ImageView?=null
    private var builder: AlertDialog.Builder?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_encryption)

        key = findViewById<View>(R.id.key) as EditText
        encryptData = findViewById<View>(R.id.encryptData) as EditText
        image = findViewById<View>(R.id.image) as TextView
        encryptBtn = findViewById<View>(R.id.encryptBtn) as Button
        imageView = findViewById<View>(R.id.imageView) as ImageView
        builder = AlertDialog.Builder(this)

        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val dialogView: View = inflater.inflate(R.layout.progressbar, null)
        builder!!.setView(dialogView)
        builder!!.setTitle("Encrytping...")
        builder!!.setCancelable(false)

        val dialog: Dialog = builder!!.create()

        encryptData!!.setOnClickListener{
            inputDialog()
        }

        //Adding image in data
        image!!.setOnClickListener{
            //Gallery Opening
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.type=("image/*")
            val mimeType = arrayOf("image/jpeg","image/jpg","image/png")
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeType)
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivityForResult(intent, 1);
        }


        //Encrypting data into image
        encryptBtn!!.setOnClickListener {
            if(key!!.text.toString().length == 16){
                if(encryptData!!.text.toString().length > 0){
                    if(bitMap != null){
                        Thread{
                            runOnUiThread{dialog.show()}
                            //Encrypt data using AES
                            val data: String = encryptData()

                            val map: Bitmap = hideData(data, bitMap!!)
                            //reset encrypted data
                            string=""
                            //save encrypted image to device
                            saveMediaFile(map)

                        }
                    }
                }
            }
        }



    }

    private  fun saveMediaFile(bitMap : Bitmap){
        
    }

    private fun hideData(data: String, bitMap: Bitmap):Bitmap{
        val string = "0001011100011110"
        val startingString = "011010010110111001100110011010010110111001101001"

        val encodeString = data + string + startingString

        val width = bitMap.getWidth()
        val height = bitMap.getHeight()


        val array = IntArray(width * height)
        bitMap.getPixels(array, 0, width, 0, 0, width, height)
        Log.e("width",width.toString())
        Log.e("height", height.toString())

        var count = 0

        //Modifying pixel data by encoded string
        for(x in 0 until height){
            if(count > encodeString!!.length-1){
                break
            }
            else{
                for(y in 0 until width){
                    if(count > encodeString!!.length-1){
                        break
                    }
                    else{
                        val index: Int = x * width + y

                        //bitwise shifting
                        var R: Int = array.get(index) shr 16 and 0xff
                        var G: Int = array.get(index) shr 8 and 0xff
                        var B: Int = array.get(index) and 0xff

                        R = encode(R,count,encodeString)
                        count++

                        if(count < encodeString!!.length){
                            G = encode(G,count,encodeString)
                            count++
                        }
                        if(count < encodeString!!.length){
                            B = encode(B,count,encodeString)
                            count++
                        }

                        //storing modified rgb value
                        array[index] = -0x1000000 or (R shl 16) or (G shl 8) or B
                    }
                }
            }
        }

        val newBitmap = Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888)
        //creating bitmap of modified pixel
        newBitmap.setPixels(array,0, width, 0, 0, width, height)
        return newBitmap
    }


    //encoding into RGB
    private fun encode(color: Int, count: Int, encodeString: String): Int{
        var binary = Integer.toBinaryString(color)
        if(binary.length < 8){
            for(x in 1 .. (8-binary.length)){
                binary = "0" + binary
            }
        }

        binary = binary.slice(0 .. (binary.length - 2)) + encodeString!![count]

        return Integer.parseInt(binary, 2)
    }

    private fun encryptData(): String{
        val key = key!!.text.toString()
        val data = encryptData!!.text.toString()

        val seckey: Key = SecretKeySpec(key.toByteArray(), "AES")

        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.ENCRYPT_MODE, seckey)

        val encrypt = cipher.doFinal(data.toByteArray())


        //Converting Encrypted data to BASE_64
        val encrypt_64 = android.util.Base64.encodeToString(encrypt, Base64.NO_WRAP or Base64.NO_PADDING)


        for(index in encrypt_64){
            var binaryString = Integer.toBinaryString((index.toInt()))
            if(binaryString.length < 8){
                for(index2 in 1..(8-binaryString.length)){
                    binaryString = "0" + binaryString
                }
            }

            string = binaryString + string
        }
        return string.toString()
    }

    private fun inputDialog(){
        val dialog: AlertDialog.Builder = AlertDialog.Builder(this)
        dialog.setTitle("Encrypt Data")
        dialog.setMessage("Enter data you want to encrypt")
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val viewInflated: View = inflater.inflate(R.layout.inputdialog, null)
        val input: EditText? = viewInflated.findViewById<View>(R.id.input) as? EditText

        input!!.setText(encryptData!!.text.toString())
        dialog.setView(viewInflated)

        dialog.setPositiveButton("Done", object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
                dialog!!.dismiss()
                encryptData!!.setText(input!!.text.toString())
            }
        })

        dialog.setNegativeButton("Cancel", object : DialogInterface.OnClickListener{
            override fun onClick(dialog: DialogInterface?, which: Int) {
                dialog!!.cancel()
            }
        })
            .create()
        dialog.show()
    }
}