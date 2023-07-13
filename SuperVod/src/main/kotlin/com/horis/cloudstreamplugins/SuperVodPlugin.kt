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
//        activityRef = WeakReference(context as Activity)
        // dbyun,dbm3u8
        addVodSource("百度影视", "https://api.apibdzy.com/api.php/provide/vod/from/dbm3u8/")
        // tkyun,tkm3u8
        addVodSource("天空影视", "https://api.tiankongapi.com/api.php/provide/vod/from/tkm3u8/")
        // kcyun,kcm3u8
        addVodSource("快车影视", "https://caiji.kczyapi.com/api.php/provide/vod/from/kcm3u8/")
        // 1080zyk
        addVodSource("优质影视", "https://api.1080zyku.com/inc/apijson.php/provide/vod/")
        // kdyun,kdm3u8
//        addVodSource("酷点影视", "https://kudian10.com/api.php/provide/vod/from/kdm3u8/")
//        addVodSource("酷点影视2", "https://api.kuapi.cc/api.php/provide/vod/from/kdm3u8/")
        // kjj,kuaiyun
//        addVodSource("快竞技", "https://api.kjjapi.com/api.php/provide/vod/from/kjj/")
        // foxyun,foxm3u8
//        addVodSource("Fox影视", "https://api.foxzyapi.com/api.php/provide/vod/from/foxm3u8/")
        // liangzi,lzm3u8
        addVodSource("量子影视", "https://cj.lziapi.com/api.php/provide/vod/from/lzm3u8/")
        // fsyun,fsm3u8
        addVodSource("飞速影视", "https://www.feisuzyapi.com/api.php/provide/vod/from/fsm3u8/")
        // hnm3u8,hnyun
        addVodSource("红牛影视", "https://www.hongniuzy2.com/api.php/provide/vod/from/hnm3u8/")
        // kbzy,kbm3u8
//        addVodSource("快播影视", "https://www.kuaibozy.com/api.php/provide/vod/from/kbm3u8/")
        // bjyun,bjm3u8
        addVodSource("八戒影视", "https://cj.bajiecaiji.com/inc/api.php/provide/vod/", 1)
        // tpiframe,tpm3u8
        addVodSource("淘片影视", "https://taopianapi.com/home/cjapi/as/mc10/vod/xml", 1)
        // xhzy,xhm3u8
//        addVodSource("星海影视", "https://www.xhzy01.com/api.php/provide/vod/from/xhm3u8/")
        // wjm3u8
        addVodSource("无尽影视", "https://api.wujinapi.me/api.php/provide/vod/from/wjm3u8/")
        // ukyun,ukm3u8
        addVodSource("U酷影视", "https://api.ukuapi.com/api.php/provide/vod/from/ukm3u8/")
        // wolong
        addVodSource("卧龙影视", "https://collect.wolongzyw.com/api.php/provide/vod/")
        // iva,kkyun
//        addVodSource("速更影视", "https://www.sugengzy.cn/api.php/provide/vod/")
        // ptm3u8,ptyun
        addVodSource("阳光影视", "https://www.xxzy.org/api.php/provide/vod/from/ptyun/")
        // ikm3u8
        addVodSource("iKun影视", "https://ikunzyapi.com/api.php/provide/vod")
        // yhm3u8
        addVodSource("樱花影视", "https://m3u8.apiyhzy.com/api.php/provide/vod/")
//        addVodSource("樱花影视2", "https://yhzy.cc/api.php/provide/vod/")
        // jinyingm3u8,jinyingyun
        // https://jyzyapi.com/provide/vod/from/jinyingm3u8/
        addVodSource("金鹰影视", "https://jinyingzy.com/provide/vod/from/jinyingm3u8/")
        // sdyun,sdm3u8
        addVodSource("闪电影视", "https://sdzyapi.com/api.php/provide/vod/from/sdm3u8/")
        // lezy,lem3u8
        addVodSource("鱼乐影视", "https://api.yulecj.com/api.php/provide/vod/from/lem3u8/")
        // tom,tomm3u8
        addVodSource("Tom影视", "https://www.tomziyuan.com/api.php/provide/vod/from/tomm3u8/")
        // wjm3u8
        addVodSource("无尽影视", "https://api.wujinapi.me/api.php/provide/vod/from/wjm3u8/")
        // gsm3u8
        addVodSource("光速影视", "https://api.guangsuapi.com/api.php/provide/vod/from/gsm3u8/")
        // ffm3u8
        addVodSource("非凡影视", "https://cj.ffzyapi.com/api.php/provide/vod/from/ffm3u8/")
        // zkzym3u8
//        addVodSource("极客影视", "https://jkzy1.com/api.php/provide/vod/from/zkzym3u8/")
        // haiwaikan
        registerMainAPI(HaiwaikanProvider())
//        addVodSource {
//            // ckm3u8
//            name = "CK影视"
//            apiUrl = "http://feifei67.com/api.php/provide/vod/"
//        }
        addVodSource {
            // mkyun,mkm3u8
            name = "手机韩剧影视"
            apiUrl = "https://77hanju.com/api.php/provide/vod/from/mkm3u8/"
        }
        addVodSource {
            // jsyun,jsm3u8
            name = "极速影视"
            apiUrl = "https://jszyapi.com/api.php/provide/vod/from/jsm3u8/"
        }
        // -----------------------------------
        // haiwaikan
        // https://olevod1.com/api.php/provide/vod/
        addVodSource("欧乐影视", "https://olevodtv.com/api.php/provide/vod/")
        // bjyun,bjm3u8,tkyun,tkm3u8,dbyun,dbm3u8
        // http://104.149.175.67/api.php/provide/vod/
        addVodSource("神马聚合影视", "https://img.smdyw.top/api.php/provide/vod/")
        // kbm3u8,bjm3u8,hnm3u8,haiwaikan
//        addVodSource("考拉TV", "https://ikaola.tv/api.php/provide/vod/")
        // wjm3u8,if101,dbm3u8,qiyi,youku,qq,hnm3u8
        addVodSource("影图影视", "https://cj.vodimg.top/api.php/provide/vod/")
        // wjm3u8,fsm3u8,bjm3u8
        addVodSource("乐活影视", "https://lehootv.com/api.php/provide/vod/")
        // zuidam3u8,zuidall,kbm3u8
        addVodSource("蓝光影视", "http://www.zzrhgg.com/api.php/provide/vod/")
        // haiwaikan
//        addVodSource("1080P影视", "https://1080p.tv/api.php/provide/vod/")
        addVodSource("789盘影视", "https://www.rrvipw.com/api.php/provide/vod/")
//        addVodSource("韦青影视", "https://lehootv.com/api.php/provide/vod/")
//        addVodSource("网民影视", "https://www.prinevillesda.org/api.php/provide/vod/")
        // mahua,123kum3u8,kbm3u8
//        addVodSource("飘花影视", "http://www.zzrhgg.com/api.php/provide/vod/")
        addVodSource {
            // wjm3u8,bjm3u8,hnm3u8
            name = "39影视"
            apiUrl = "https://www.394tv.com/api.php/provide/vod/"
        }
        addVodSource {
            // wjyun,wjm3u8
            name = "飘零影视"
            apiUrl = "https://p2100.net/api.php/provide/vod/"
        }
        addVodSource {
            // ffm3u8,haiwaikan,ZNJSON,znkan
            name = "映迷影视"
            apiUrl = "https://www.inmi.app/api.php/provide/vod/"
        }
    }

    private fun addVodSource(builder: VodSource.() -> Unit) {
        addVodSource(VodSource().apply(builder))
    }

    private fun addVodSource(
        name: String,
        apiUrl: String,
        responseType: Int = 0
    ) {
        addVodSource(name, apiUrl, 0, responseType)
    }

    private fun addVodSource(vodSource: VodSource) {
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


