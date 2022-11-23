package com.horis.cloudstreamplugins

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@Suppress("unused")
@CloudstreamPlugin
class UAPIPlugin: Plugin() {
    override fun load(context: Context) {
        // All providers should be added in this manner. Please don't edit the providers list directly.
        registerMainAPI(BdzyProvider())
        registerMainAPI(TiankongProvider())
        registerMainAPI(KczyProvider())
        registerMainAPI(Zyk1080Provider())
        registerMainAPI(KudianProvider())
        registerMainAPI(Kudian2Provider())
        registerMainAPI(KuaijingjiProvider())
        registerMainAPI(FoxProvider())
        registerMainAPI(LiangziProvider())
        registerMainAPI(FeisuProvider())
        registerMainAPI(HongniuProvider())
        registerMainAPI(KuaiboProvider())
        registerMainAPI(BajieProvider())
        registerMainAPI(TaopianProvider())
        registerMainAPI(ShenmaProvider())
        registerMainAPI(XinghaiProvider())
        registerMainAPI(WujinProvider())
        registerMainAPI(UkuProvider())
        registerMainAPI(WolongProvider())
        registerMainAPI(SugengProvider())
        registerMainAPI(YangguangProvider())
        registerMainAPI(XinghaiProvider())
        registerMainAPI(HaiwaikanProvider())
    }
}
