package online.malilab.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.SvgDecoder
import online.malilab.app.data.Auth
import online.malilab.app.data.Repo

class MaliApp : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        Repo.init(this)
        Auth.init(this)
    }

    // The site draws playbook covers as SVG pictures, so images are loaded with an SVG decoder as well.
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components { add(SvgDecoder.Factory()) }
            .crossfade(true)
            .build()
}
