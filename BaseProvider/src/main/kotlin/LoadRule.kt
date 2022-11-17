import com.lagradost.cloudstream3.SearchQuality
import com.lagradost.cloudstream3.TvType

open class LoadRule(
    val list: String,
    val title: String,
    val url: String,
    val posterUrl: String? = null,
    val posterHeaders: Map<String, String?> = mapOf(),
    val year: String? = null,
    val type: String? = null,
    val quality: String? = null,
) {
    open fun getType(type: String): TvType? = null
    open fun getQuality(quality: String): SearchQuality? = null
    open fun getYear(year: String): String? = null
}