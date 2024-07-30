package com.horis.cloudstreamplugins

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context
import java.lang.ref.WeakReference

@CloudstreamPlugin
class BdysPlugin : Plugin() {

    override fun load(context: Context) {
        // All providers should be added in this manner. Please don't edit the providers list directly.
        registerMainAPI(BdysProvider())
    }
}
