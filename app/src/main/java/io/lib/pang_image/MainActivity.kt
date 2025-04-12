package io.lib.pang_image

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.imageloader.pang.decoder.PangDecoder
import com.example.imageloader.pang.util.domain.PangRequest
import io.lib.pang_image.downloader.PangDownloader
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val imgView = findViewById<ImageView>(R.id.img)

        lifecycleScope.launch {
            val result = PangDownloader.saveImage(
                request = PangRequest(
                    url = "https://images.unsplash.com/photo-1741851374674-e4b7e573a9e7?q=80&w=2940&auto=format&fit=crop&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D",
                    cachePath = cacheDir.path,
                ),
                diskCacheKey = "image_${"https://images.unsplash.com/photo-1741851374674-e4b7e573a9e7?q=80&w=2940&auto=format&fit=crop&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D".hashCode()}"
            )

            result.onSuccess { file ->
                if (file != null) {
                    Log.d("PangDecoder", "ImageView Size: ${imgView.width} ${imgView.height}")
                    PangDecoder.decodeFromFile(file.path, imgView.width, imgView.height).onSuccess {
                        it?.let {
                            Log.d("PangDecoder", "Bitmap Size: ${it.width} ${it.height}")

                            imgView.setImageBitmap(it)
                        }
                    }
                } else {
                    Log.e("Pang", "파일이 null이에요")
                }
            }.onFailure { e ->
                Log.e("Pang", "이미지 저장 실패: ${e.message}", e)
            }
        }

    }
}
