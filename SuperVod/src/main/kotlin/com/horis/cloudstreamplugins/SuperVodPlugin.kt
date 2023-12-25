package com.horis.cloudstreamplugins

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context
import com.horis.cloudstreamplugins.providers.HaiwaikanProvider

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
//        addVodSource("淘片影视", "https://taopianapi.com/home/cjapi/as/mc10/vod/xml", 1)
        addVodSource("淘片影视", "https://taopianapi.com/cjapi/mc10/vod/json.html")
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
//        addVodSource("阳光影视", "https://www.xxzy.org/api.php/provide/vod/from/ptyun/")
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
//        addVodSource("鱼乐影视", "https://api.yulecj.com/api.php/provide/vod/from/lem3u8/")
        // tom,tomm3u8
//        addVodSource("Tom影视", "https://www.tomziyuan.com/api.php/provide/vod/from/tomm3u8/")
        // wjm3u8
//        addVodSource("无尽影视", "https://api.wujinapi.me/api.php/provide/vod/from/wjm3u8/")
        // gsm3u8
        addVodSource("光速影视", "https://api.guangsuapi.com/api.php/provide/vod/from/gsm3u8/")
        // ffm3u8
        addVodSource("非凡影视", "https://cj.ffzyapi.com/api.php/provide/vod/from/ffm3u8/")
        // zkzym3u8
//        addVodSource("极客影视", "https://jkzy1.com/api.php/provide/vod/from/zkzym3u8/")
        // haiwaikan
//        registerMainAPI(HaiwaikanProvider())
//        addVodSource {
//            // ckm3u8
//            name = "CK影视"
//            apiUrl = "http://feifei67.com/api.php/provide/vod/"
//        }
//        addVodSource {
//            // mkyun,mkm3u8
//            name = "手机韩剧影视"
//            apiUrl = "https://77hanju.com/api.php/provide/vod/from/mkm3u8/"
//        }
        addVodSource {
            // jsyun,jsm3u8
            name = "极速影视"
            apiUrl = "https://jszyapi.com/api.php/provide/vod/from/jsm3u8/"
        }
        addVodSource {
            // bfzym3u8
            name = "暴风影视"
            apiUrl = "https://bfzyapi.com/api.php/provide/vod/"
        }
        addVodSource {
            // subyun,subm3u8
            name = "速播影视"
            apiUrl = "https://subocaiji.com/api.php/provide/vod/from/subm3u8/"
        }
        addVodSource {
            // xlyun,xlm3u8
            name = "新浪影视"
            apiUrl = "https://api.xinlangapi.com/xinlangapi.php/provide/vod/from/xlm3u8/"
        }
//        addVodSource {
//            // kuaikan,kuaikanyun
//            name = "快看影视"
//            apiUrl = "https://kuaikan-api.com/api.php/provide/vod/from/kuaikan/"
//            skipCategory = 9
//        }
//        addVodSource {
//            // okm3u8
//            name = "OK影视"
//            apiUrl = "https://okzy1.tv/api.php/provide/vod/"
//        }
        addVodSource {
            // qhyun,qhm3u8
            name = "奇虎影视"
            apiUrl = "https://caiji.qhzyapi.com/api.php/provide/vod/from/qhm3u8/"
        }
//        addVodSource {
//            // damo,M3U8
//            name = "大漠影视"
//            apiUrl = "https://damozy.com/api.php/provide/vod/from/M3U8/"
//        }
        addVodSource {
            // snm3u8
            name = "索尼影视"
            apiUrl = "https://suoniapi.com/api.php/provide/vod/from/snm3u8/"
        }
        addVodSource {
            // zzdj
            name = "种子短剧"
            apiUrl = "https://zzdj.cc/api.php/provide/vod/from/zzdj/"
        }
        addVodSource {
            // leshi
            name = "乐视影视"
            apiUrl = "https://leshizyapi.com/api.php/provide/vod/from/leshi/"
        }
        addVodSource {
            // modum3u8
            name = "魔都动漫"
            apiUrl = "https://caiji.moduapi.cc/api.php/provide/vod/from/modum3u8/"
        }
        addVodSource {
            // yxys
            name = "耀协影视"
            apiUrl = "http://zyz.yxys.top/api.php/provide/vod/from/yxys/"
        }
        addVodSource {
            // hhm3u8
            name = "豪华影视"
            apiUrl = "https://hhzyapi.com/api.php/provide/vod/from/hhm3u8/"
        }
        addVodSource {
            // dplayer
            name = "加菲猫影视"
            apiUrl = "https://xzcjz.com/api.php/provide/vod/"
        }
        addVodSource {
            // 49zyw
            name = "49影视"
            apiUrl = "https://49zyw.com/api.php/provide/vod/from/49zyw/"
        }
        addVodSource {
            // jiguang
            name = "极光影视"
            apiUrl = "https://jiguang.la/api.php/provide/vod/from/jiguang/"
        }
        addVodSource {
            // kuaiyun
            name = "快云影视"
            apiUrl = "https://kuaiyun-api.com/api.php/provide/vod/from/kuaiyun/"
            skipCategory = 9
        }
        addVodSource {
            // kuaikan
            name = "快看影视"
            apiUrl = "https://kuaikan-api.com/api.php/provide/vod/from/kuaikan/"
            skipCategory = 9
        }
        addVodSource {
            // qhm3u8
            name = "奇虎影视"
            apiUrl = "https://caiji.qhzyapi.com/api.php/provide/vod/from/qhm3u8/"
            skipCategory = 5
        }
        addVodSource {
            // haiwaikan
            name = "海外看影视"
            apiUrl = "https://haiwaikan.com/api.php/provide/vod/from/haiwaikan/"
        }
//        addVodSource {
//            // 68zy_yun,68zy_m3u8
//            name = "68影视"
//            apiUrl = "https://caiji.68zyapi.com/api.php/provide/vod/from/68zy_m3u8/"
//        }
        //registerMainAPI(JiangNanProvider())

        // -----------------------------------
        // haiwaikan
        // https://olevod1.com/api.php/provide/vod/
        // https://olevod2.com/api.php/provide/vod/
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
        addVodSource(VodSource(name, apiUrl, 0, responseType))
    }

    private fun addVodSource(
        vodSource: VodSource
    ) {
        vodSource.run {
            val apiExtractor = makeApiExtractor(apiUrl, apiType, responseType)
            val provider = object : BaseVodProvider() {
                override val apiExtractor = apiExtractor
                override var name: String = this@run.name
                override val skipCategory = this@run.skipCategory
            }
            registerMainAPI(provider)
        }
    }
}


