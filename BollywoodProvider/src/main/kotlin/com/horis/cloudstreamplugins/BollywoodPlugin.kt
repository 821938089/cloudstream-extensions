package com.horis.cloudstreamplugins

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context
import com.horis.BuildConfig

@CloudstreamPlugin
class BollywoodPlugin : Plugin() {
    override fun load(context: Context) {
        // All providers should be added in this manner. Please don't edit the providers list directly.
        registerMainAPI(BollywoodProvider())
        registerMainAPI(HollywoodProvider())
        registerMainAPI(GDIndexProvider())
        registerMainAPI(ShinobiCloudProvider())
        registerMainAPI(UltimateCourseProvider())
//        registerMainAPI(NGIndexProvider())
//        registerMainAPI(LemonMoviesProvider())
        registerMainAPI(TGArchiveProvider())
//        loadCryptoJs()
    }

    private fun loadCryptoJs() {
        val id = resources!!.getIdentifier("cryptojs", "raw", BuildConfig.LIBRARY_PACKAGE_NAME)
        cryptoJsSource = resources!!.openRawResource(id).bufferedReader().use { it.readText() }
    }

}
