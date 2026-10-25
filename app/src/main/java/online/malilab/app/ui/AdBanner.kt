package online.malilab.app.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import online.malilab.app.BuildConfig

/**
 * One Google banner ad, as wide as the screen. [onFailed] is called if no ad can be shown (no connection, no ad to
 * show), so the screen can fall back to its own message instead of an empty gap.
 */
@Composable
fun AdBanner(onFailed: () -> Unit) {
    val context = LocalContext.current
    val widthDp = LocalConfiguration.current.screenWidthDp
    val view = remember(widthDp) {
        AdView(context).apply {
            setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp))
            adUnitId = BuildConfig.BANNER_UNIT
            adListener = object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    onFailed()
                }
            }
            loadAd(AdRequest.Builder().build())
        }
    }
    DisposableEffect(view) { onDispose { view.destroy() } }
    AndroidView(factory = { view }, modifier = Modifier.fillMaxWidth())
}
