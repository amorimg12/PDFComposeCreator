package com.example.pdfcreator

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Picture
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.AttributeSet
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.pdfcreator.ui.theme.PDFCreatorTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume


class MainActivity : ComponentActivity() {
    private var jetCaptureView: MutableState<ComposeListView>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val scope = rememberCoroutineScope()
            val context = LocalContext.current

            PDFCreatorTheme {
                Surface {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        Box(modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()) {
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())){
                                CreateViewUI()
                            }
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Button(onClick = {
                                    val view = jetCaptureView?.value!!
                                    scope.launch {
                                        captureBitmap(view)
                                            ?.saveToDisk(context)
                                    }
                                    Toast.makeText(
                                        context,
                                        "Image saved to Pictures",
                                        Toast.LENGTH_SHORT
                                    )
                                        .show()
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Add"
                                    )
                                }
                            }
                        }
                    }

                }
            }
        }
    }

    @Composable
    private fun CreateViewUI() {
        jetCaptureView = remember { mutableStateOf(ComposeListView(this@MainActivity)) }
        AndroidView(modifier = Modifier.wrapContentSize(),
            factory = {
                ComposeListView(it).apply {
                    post {
                        jetCaptureView?.value = this
                    }
                }
            }
        )
    }
}

class ComposeListView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AbstractComposeView(context, attrs, defStyleAttr) {


    @Composable
    override fun Content() {
        val picture = remember { Picture() }
        val scope = rememberCoroutineScope()
        val context = LocalContext.current

        Column() {
            for(it in 0..25) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Image(
                        imageVector = Icons.Default.AccountBox,
                        contentDescription = "Done"
                    )
                    Text(modifier = Modifier.height(50.dp), text = "Item $it")
                    Image(
                        imageVector = Icons.Default.Done,
                        contentDescription = "Done"
                    )
                }
            }
        }
    }
}

class ImageUtils {

    companion object{
        fun generateBitmapFromView(view: View): Bitmap {
            val specWidth = View.MeasureSpec.makeMeasureSpec(1324, View.MeasureSpec.AT_MOST)
            val specHeight = View.MeasureSpec.makeMeasureSpec(521, View.MeasureSpec.AT_MOST)
            view.measure(specWidth, specHeight)
            val width = view.measuredWidth
            val height = view.measuredHeight
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            view.layout(view.left, view.top, view.right, view.bottom)
            view.draw(canvas)
            return bitmap
        }
    }
}

suspend fun Bitmap.saveToDisk(context: Context): Uri {
    val file = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
        "screenshot-${System.currentTimeMillis()}.png"
    )

    file.writeBitmap(this, Bitmap.CompressFormat.PNG, 100)

    return scanFilePath(context, file.path) ?: throw Exception("File could not be saved")
}


suspend fun scanFilePath(context: Context, filePath: String): Uri? {
    return suspendCancellableCoroutine { continuation ->
        MediaScannerConnection.scanFile(
            context,
            arrayOf(filePath),
            arrayOf("image/png")
        ) { _, scannedUri ->
            if (scannedUri == null) {
                continuation.cancel(Exception("File $filePath could not be scanned"))
            } else {
                continuation.resume(scannedUri)
            }
        }
    }
}

fun File.writeBitmap(bitmap: Bitmap, format: Bitmap.CompressFormat, quality: Int) {
    outputStream().use { out ->
        bitmap.compress(format, quality, out)
        out.flush()
    }
}


fun getBitmapFromView(view: View, totalHeight: Int, totalWidth: Int): Bitmap {
    val returnedBitmap = Bitmap.createBitmap(totalWidth, totalHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(returnedBitmap)
    val bgDrawable = view.background
    if (bgDrawable != null) bgDrawable.draw(canvas)
    else canvas.drawColor(Color.WHITE)
    view.draw(canvas)
    return returnedBitmap
}


fun captureBitmap(view: View): Bitmap? {
    var mlp = MarginLayoutParams(0, 0)

    if (view.layoutParams is MarginLayoutParams) {
        mlp = view.layoutParams as MarginLayoutParams
    }

    val parentWms = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
    val parentHms = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
    val wms = ViewGroup.getChildMeasureSpec(
        parentWms,
        view.paddingLeft + view.paddingRight + mlp.leftMargin + mlp.rightMargin,
        view.layoutParams.width
    )
    val hms = ViewGroup.getChildMeasureSpec(
        parentHms,
        view.paddingTop + view.paddingBottom + mlp.topMargin + mlp.bottomMargin,
        view.layoutParams.height
    )
    view.measure(wms, hms)

    view.layout(0, 0, view.measuredWidth, view.measuredHeight)
    if (view.measuredWidth <= 0 || view.measuredHeight <= 0) {
        return null
    }
    val bitmap =
        Bitmap.createBitmap(view.measuredWidth, view.measuredHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val bgDrawable = view.background
    if (bgDrawable != null) bgDrawable.draw(canvas)
    else canvas.drawColor(Color.WHITE)

    view.draw(canvas)

    return bitmap
}