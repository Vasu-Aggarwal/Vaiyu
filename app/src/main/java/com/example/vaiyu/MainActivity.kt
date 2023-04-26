package com.example.vaiyu

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import com.example.vaiyu.databinding.ActivityMainBinding
import com.example.vaiyu.ml.AqiQualityModel
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var img: Bitmap

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.selectBtn.setOnClickListener {
            val intent = (Intent(Intent.ACTION_GET_CONTENT)).setType("image/*")
            startActivityForResult(intent, 101)
        }

        binding.predictBtn.setOnClickListener {
            img = Bitmap.createScaledBitmap(img, 256, 256, true)

            // Creates inputs for reference.
            val model = AqiQualityModel.newInstance(this)
            val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 256, 256, 3), DataType.FLOAT32)
            val tensorImage = TensorImage(DataType.FLOAT32)
            tensorImage.load(img)
            val byteBuffer = tensorImage.buffer
            inputFeature0.loadBuffer(byteBuffer)

            // Runs model inference and gets result.
            val outputs = model.process(inputFeature0)
            val result = outputs.outputFeature0AsTensorBuffer

            // Releases model resources if no longer used.
            model.close()

            if(result.floatArray[0] > result.floatArray[1] && result.floatArray[0] > result.floatArray[2]){
                binding.tv.text = "POOR"
            }

            else if(result.floatArray[1] > result.floatArray[0] && result.floatArray[1] > result.floatArray[2]){
                binding.tv.text = "VERY POOR"
            }

            else{
                binding.tv.text = "SEVERE"
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 101){
            binding.img.setImageURI(data?.data)
            val uri = data?.data!!
            img = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
        }
    }
}
