package com.zomdroid.ui.screens.wiki

import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.zomdroid.ui.theme.ZomdroidTheme

@Composable
fun WikiScreen() {
    val backgroundColor = MaterialTheme.colorScheme.background.toArgb()

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                setBackgroundColor(backgroundColor)

                // Loading local asset
                loadUrl("file:///android_asset/wiki/index.html")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun WikiScreenPreview() {
    ZomdroidTheme {
        WikiScreen()
    }
}
