package com.horis.cloudstreamplugins

import android.app.Activity
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context
import java.lang.ref.WeakReference

@Suppress("unused")
@CloudstreamPlugin
class SuperVodPlugin : Plugin() {
    override fun load(context: Context) {
        // All providers should be added in this manner. Please don't edit the providers list directly.
        activityRef = WeakReference(context as Activity)
        addVodSource("百度影视", "https://api.apibdzy.com/api.php/provide/vod/")
        addVodSource("天空影视", "https://api.tiankongapi.com/api.php/provide/vod/")
        addVodSource("快车影视", "https://caiji.kczyapi.com/api.php/provide/vod/")
        addVodSource("优质影视", "https://api.1080zyku.com/inc/apijson.php/provide/vod/")
        addVodSource("酷点影视", "https://kudian10.com/api.php/provide/vod/")
        addVodSource("酷点影视2", "https://api.kuapi.cc/api.php/provide/vod/")
        addVodSource("快竞技", "https://api.kjjapi.com/api.php/provide/vod/")
        addVodSource("Fox影视", "https://api.foxzyapi.com/api.php/provide/vod/")
        addVodSource("量子影视", "https://cj.lziapi.com/api.php/provide/vod/")
        addVodSource("飞速影视", "https://www.feisuzyapi.com/api.php/provide/vod/")
        addVodSource("红牛影视", "https://www.hongniuzy2.com/api.php/provide/vod/")
        addVodSource("快播影视", "https://www.kuaibozy.com/api.php/provide/vod/")
        addVodSource("八戒影视", "https://cj.bajiecaiji.com/inc/api.php/provide/vod/")
        addVodSource("淘片影视", "https://taopianapi.com/home/cjapi/as/mc10/vod/xml", responseType = 1)
        addVodSource("神马聚合影视", "https://img.smdyw.top/api.php/provide/vod/")
        addVodSource("星海影视", "https://www.xhzy01.com/api.php/provide/vod/")
        addVodSource("无尽影视", "https://api.wujinapi.me/api.php/provide/vod/from/wjm3u8/")
        addVodSource("U酷影视", "https://api.ukuapi.com/api.php/provide/vod/")
        addVodSource("卧龙影视", "https://collect.wolongzyw.com/api.php/provide/vod/")
        addVodSource("速更影视", "https://www.sugengzy.cn/api.php/provide/vod/")
        addVodSource("阳光影视", "https://www.xxzy.org/api.php/provide/vod/")
        addVodSource("星海影视", "https://www.xhzy01.com/api.php/provide/vod/")
        addVodSource("iKun影视", "https://ikunzyapi.com/api.php/provide/vod")
        addVodSource("樱花影视", "https://m3u8.apiyhzy.com/api.php/provide/vod/")
        addVodSource("金鹰影视", "https://jinyingzy.com/provide/vod/")
        addVodSource("闪电影视", "https://sdzyapi.com/api.php/provide/vod/")
        addVodSource("鱼乐影视", "https://api.yulecj.com/api.php/provide/vod/")
        addVodSource("Tom影视", "https://www.tomziyuan.com/api.php/provide/vod/")
        addVodSource("欧乐影视", "https://www.oulevod.tv/api.php/provide/vod/")
        addVodSource("考拉TV", "https://ikaola.tv/api.php/provide/vod/")
        addVodSource("影图影视", "https://cj.vodimg.top/api.php/provide/vod/")
        addVodSource("无尽影视", "https://api.wujinapi.me/api.php/provide/vod/from/wjm3u8/")
        addVodSource("乐活影视", "https://lehootv.com/api.php/provide/vod/")
        addVodSource("蓝光影视", "http://www.zzrhgg.com/api.php/provide/vod/")
        addVodSource("无名影视", "http://vipmv.cc/api.php/provide/vod/")
        addVodSource("1080P影视", "https://1080p.tv/api.php/provide/vod/")
//        registerMainAPI(BdzyProvider())
//        registerMainAPI(TiankongProvider())
//        registerMainAPI(KczyProvider())
//        registerMainAPI(Zyk1080Provider())
//        registerMainAPI(KudianProvider())
//        registerMainAPI(Kudian2Provider())
//        registerMainAPI(KuaijingjiProvider())
//        registerMainAPI(FoxProvider())
//        registerMainAPI(LiangziProvider())
//        registerMainAPI(FeisuProvider())
//        registerMainAPI(HongniuProvider())
//        registerMainAPI(KuaiboProvider())
//        registerMainAPI(BajieProvider())
//        registerMainAPI(TaopianProvider())
//        registerMainAPI(ShenmaProvider())
//        registerMainAPI(XinghaiProvider())
//        registerMainAPI(WujinProvider())
//        registerMainAPI(UkuProvider())
//        registerMainAPI(WolongProvider())
//        registerMainAPI(SugengProvider())
//        registerMainAPI(YangguangProvider())
//        registerMainAPI(XinghaiProvider())
        registerMainAPI(HaiwaikanProvider())
//        registerMainAPI(IkunProvider())
//        registerMainAPI(YinghuaProvider())
//        registerMainAPI(JinyingProvider())
//        registerMainAPI(ShandianProvider())
//        registerMainAPI(YuleProvider())
//        registerMainAPI(TomProvider())
//        registerMainAPI(OulevodProvider())
//        registerMainAPI(IkaolaProvider())
//        registerMainAPI(YintuProvider())
//        registerMainAPI(WujinProvider())
//        registerMainAPI(LehuoProvider())
//        registerMainAPI(LanguangProvider())

//        registerMainAPI(WumingProvider())
//        registerMainAPI(P1080Provider())
//        registerMainAPI(WoniuProvider())
    }

    fun addVodSource(vodSource: VodSource) {
        with(vodSource) {
            addVodSource(name, apiUrl, apiType, responseType)
        }
    }

    private fun addVodSource(
        name: String,
        apiUrl: String,
        apiType: Int = 0,
        responseType: Int = 0
    ) {
        val apiExtractor = makeApiExtractor(apiUrl, apiType, responseType)
        val provider = object : BaseVodProvider() {
            override val apiExtractor = apiExtractor
            override var name = name
        }
        registerMainAPI(provider)
    }
}


