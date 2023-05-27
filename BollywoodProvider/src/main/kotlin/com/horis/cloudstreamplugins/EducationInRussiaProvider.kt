package com.horis.cloudstreamplugins

import com.lagradost.cloudstream3.ErrorLoadingException
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.SeasonData
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.amapIndexed
import com.lagradost.cloudstream3.mainPageOf
import com.lagradost.cloudstream3.newEpisode
import com.lagradost.cloudstream3.newTvSeriesLoadResponse
import com.lagradost.cloudstream3.utils.AppUtils

class EducationInRussiaProvider : BollywoodProvider() {
    override var lang = "en"

    override var mainUrl = "https://educationinrussia.pages.dev"
    override var name = "EducationInRussia"

    override val api = "https://courses.zindex.eu.org"

    override val mainPage = mainPageOf(
        "$api/0:/" to " AN.EDU.01",
        "$api/1:/" to " BC.Course.TARS",
        "$api/2:/" to " BC.Courses.01",
        "$api/3:/" to " BC.Courses.02",
        "$api/4:/" to " BC.Courses.ALL",
        "$api/5:/" to " BC.Courses.SPECIAL",
        "$api/6:/" to " BC.Courses.Special.Folders",
        "$api/7:/" to " BC.Courses.Special.Folders.02",
        "$api/8:/" to " BC.Courses.Udemy.01",
        "$api/9:/" to " BC.EDU.01",
        "$api/10:/" to " BNC.Courses",
        "$api/11:/" to " BNC.Lets.Educate",
        "$api/12:/" to " Bot.Lets.Educate.Drive.1.1",
        "$api/13:/" to " Bot.Lets.Educate.Drive.2.1",
        "$api/14:/" to " Course.A",
        "$api/15:/" to " Course.B",
        "$api/16:/" to " Course.C",
        "$api/17:/" to " Course.D",
        "$api/18:/" to " Course.E",
        "$api/19:/" to " Course.F",
        "$api/20:/" to " Course.G",
        "$api/21:/" to " Course.H",
        "$api/22:/" to " Course.I",
        "$api/23:/" to " Course.J",
        "$api/24:/" to " Course.K",
        "$api/25:/" to " Course.L",
        "$api/26:/" to " Course.M",
        "$api/27:/" to " Course.N",
        "$api/28:/" to " Course.O",
        "$api/29:/" to " Course.P",
        "$api/30:/" to " Course.Q",
        "$api/31:/" to " Course.Q",
        "$api/32:/" to " Course.R",
        "$api/33:/" to " Course.S",
        "$api/34:/" to " Course.T",
        "$api/35:/" to " Course.THE",
        "$api/36:/" to " Course.U",
        "$api/37:/" to " Course.V",
        "$api/38:/" to " Course.W",
        "$api/39:/" to " Course.X",
        "$api/40:/" to " Course.Y",
        "$api/41:/" to " Course.Z",
        "$api/42:/" to " COURSES DRIVE 10",
        "$api/43:/" to " COURSES DRIVE 2",
        "$api/44:/" to " COURSES DRIVE 3",
        "$api/45:/" to " COURSES DRIVE 4",
        "$api/46:/" to " COURSES DRIVE 5",
        "$api/47:/" to " COURSES DRIVE 6",
        "$api/48:/" to " COURSES DRIVE 7",
        "$api/49:/" to " COURSES DRIVE 8",
        "$api/50:/" to " COURSES DRIVE 9",
        "$api/51:/" to " Courses.01",
        "$api/52:/" to " Courses.02",
        "$api/53:/" to " Courses.03",
        "$api/54:/" to " Courses.04",
        "$api/55:/" to " Courses.05",
        "$api/56:/" to " Courses.A",
        "$api/57:/" to " Courses.B",
        "$api/58:/" to " Courses.C",
        "$api/59:/" to " Courses.D",
        "$api/60:/" to " Courses.E",
        "$api/61:/" to " Courses.F",
        "$api/62:/" to " Courses.G",
        "$api/63:/" to " Courses.H",
        "$api/64:/" to " Courses.I",
        "$api/65:/" to " Courses.J",
        "$api/66:/" to " Courses.K",
        "$api/67:/" to " Courses.L",
        "$api/68:/" to " Courses.M",
        "$api/69:/" to " Courses.N",
        "$api/70:/" to " Courses.O",
        "$api/71:/" to " Courses.P",
        "$api/72:/" to " Courses.Q",
        "$api/73:/" to " Courses.R",
        "$api/74:/" to " Courses.S",
        "$api/75:/" to " Courses.T",
        "$api/76:/" to " Courses.THE",
        "$api/77:/" to " Courses.U",
        "$api/78:/" to " Courses.V",
        "$api/79:/" to " Courses.W",
        "$api/80:/" to " Courses.X",
        "$api/81:/" to " Courses.Y",
        "$api/82:/" to " Courses.Z",
        "$api/83:/" to " Courses.Z.Udemy.01",
        "$api/84:/" to " Courses.Z.Udemy.02",
        "$api/85:/" to " HH_Course.Categories",
        "$api/86:/" to " HH_Courses",
        "$api/87:/" to " HH_Courses.ArchiveSites",
        "$api/88:/" to " HH_Courses.DUMP",
        "$api/89:/" to " HH_Courses.SourceSites",
        "$api/90:/" to " HH_Courses.Udemy.01",
        "$api/91:/" to " HH_Courses_A-H",
        "$api/92:/" to " HH_Courses_I-Q",
        "$api/93:/" to " HH_Courses_R-Z",
        "$api/94:/" to " HH_EDU.01",
        "$api/95:/" to " HH_EDU.02"
    )

    private val driverIds = arrayOf(
        "0AHobTDR_LC-pUk9PVA",
        "0AI68fjvq9apAUk9PVA",
        "0ADkPi2gZDDurUk9PVA",
        "0AEtumiV4dym0Uk9PVA",
        "0ABBBI2WzUrw_Uk9PVA",
        "0ALGlnhwYKrpkUk9PVA",
        "0AA3vgmnp-KFrUk9PVA",
        "0AJ6y9-3BS03cUk9PVA",
        "0AGlMsaTal8AwUk9PVA",
        "0AMxP23uxRhiRUk9PVA",
        "0AMlrvRe6RQ8BUk9PVA",
        "0ABDznKg7gB4uUk9PVA",
        "0APjfv3esEo54Uk9PVA",
        "0AP51flz9kGV8Uk9PVA",
        "0AD48BIECvSnYUk9PVA",
        "0AEXBlqBt4UDtUk9PVA",
        "0AIt9tYhnvkTAUk9PVA",
        "0ADagxAdiQu5JUk9PVA",
        "0AKwPQqH9ZTfWUk9PVA",
        "0AG9lHVRLZMT9Uk9PVA",
        "0ACBNA0LbRtrvUk9PVA",
        "0AKnNaT6Um6jIUk9PVA",
        "0AM4fLEjYnWaPUk9PVA",
        "0AAyG9_hYGrEJUk9PVA",
        "0AH_gXQNxziqYUk9PVA",
        "0AMQ9n3FByMdqUk9PVA",
        "0APB7B18rPEUAUk9PVA",
        "0AFZGtDU8mgEzUk9PVA",
        "0AN4RuENRqABRUk9PVA",
        "0AMrK1xRugyjXUk9PVA",
        "0AKREMgry-oMUUk9PVA",
        "0AKIEOcQbaFqeUk9PVA",
        "0AHCBwK8xGYC0Uk9PVA",
        "0ABtqsA79ApCqUk9PVA",
        "0ANf6y0936tjUUk9PVA",
        "0AGYpZHaW2qBCUk9PVA",
        "0ANTyohjJP1kDUk9PVA",
        "0ALdaMs1BZihzUk9PVA",
        "0ALfPBVITXDkfUk9PVA",
        "0AO8z24kA2MZiUk9PVA",
        "0AOpvfdSDQDIoUk9PVA",
        "0AK0igkqocx1YUk9PVA",
        "0AGOk6bcqBrOXUk9PVA",
        "0AD37K5c5KlLhUk9PVA",
        "0AGHYMYDc6BJZUk9PVA",
        "0AOS1Rp_13YdOUk9PVA",
        "0AE7XeKVLsrtiUk9PVA",
        "0ANWPmqH3_i7VUk9PVA",
        "0AHn5pq6i07wVUk9PVA",
        "0AJcGXUkEfnYNUk9PVA",
        "0AAyQTnofQ6ArUk9PVA",
        "0AFM5qYOdq4pbUk9PVA",
        "0ACZsrjjflFcPUk9PVA",
        "0ALxJFYOFUsmsUk9PVA",
        "0AB1rfAAQ1qlAUk9PVA",
        "0AK3BjxUOH2tgUk9PVA",
        "0AD8NJ6ZSlm3YUk9PVA",
        "0AAB-I8gjBFa3Uk9PVA",
        "0AF9W3iiSmq8vUk9PVA",
        "0AAO67bhOY9bFUk9PVA",
        "0APwhtGzKuWzeUk9PVA",
        "0ANxwtX-a0wBmUk9PVA",
        "0ABE8QiPVHGDKUk9PVA",
        "0AKqyLuC0hjYWUk9PVA",
        "0ACEuKWYJzf5oUk9PVA",
        "0AK4pBEl63_K6Uk9PVA",
        "0AF-Ta2RXLyIcUk9PVA",
        "0AKBXaTyI0TDAUk9PVA",
        "0ADOTivmR_BhcUk9PVA",
        "0AJ4ZMBXgokBzUk9PVA",
        "0ACQvUroLKXHPUk9PVA",
        "0ANkwCtWS2EPhUk9PVA",
        "0ACz61EhW1STqUk9PVA",
        "0ACDKDMO0bQkHUk9PVA",
        "0APGXzkx2iiyxUk9PVA",
        "0AHPFwN31rualUk9PVA",
        "0AHWKju14IBweUk9PVA",
        "0AMraAOSOj74gUk9PVA",
        "0AAf6aMZOaNhCUk9PVA",
        "0ADAf3dMc6buwUk9PVA",
        "0ACp7ukVV6olnUk9PVA",
        "0AI3fWxNp41u-Uk9PVA",
        "0AJU9-p6Z2ecDUk9PVA",
        "0AHtoyy1KF6HyUk9PVA",
        "0AB_aOAmlTN4YUk9PVA",
        "0AP_TLvLf_EwxUk9PVA",
        "0ABoH-yqXmPj6Uk9PVA",
        "0AFtPDhfC-9w9Uk9PVA",
        "0AAL--eOesF7DUk9PVA",
        "0AE4oZyHJsmk2Uk9PVA",
        "0AIOsg0vg_2OHUk9PVA",
        "0AE8sUcDopSzxUk9PVA",
        "0AHYjKN9ojO68Uk9PVA",
        "0APNRURXunUqzUk9PVA",
        "0ANQj0UU6W784Uk9PVA",
        "0APb2aqeTmB1FUk9PVA"
    )

    override suspend fun load(url: String): LoadResponse? {
        val file = AppUtils.tryParseJson<GDFile>(url) ?: return null
        val title = file.name

        var seasons: List<SeasonData>? = null

        val episodes = if (file.isFolder) {
            if (file.parentFolder == null) {
                val path = id2Path(file).let {
                    it.substring(0, it.lastIndexOf("/", it.lastIndex - 1))
                }
                val driveNum = driverIds.indexOf(file.driveId)
                file.parentFolder = "$api/$driveNum:$path/"
            }
            val items = listDir(file)
            val folders = items.filter { it.isFolder }
            val files = items.filter { !it.isFolder && it.name.contains(videoFileRegex) }
            seasons = folders.mapIndexed { i, f ->
                SeasonData(i + 1, "S\\d+".toRegex().find(f.name)?.value ?: f.name)
            }
            folders.amapIndexed { index, gdFile ->
                listDir(gdFile).mapNotNull {
                    if (!it.name.contains(videoFileRegex)) return@mapNotNull null
                    newEpisode(it) {
                        name = "E\\d+".toRegex().find(it.name)?.value ?: it.name
                        season = index + 1
                    }
                }
            }.flatten().toMutableList().also {
                files.mapTo(it) { gdFile ->
                    newEpisode(gdFile) {
                        name = gdFile.name
                        season = seasons.size
                    }
                }
            }
        } else {
            arrayListOf(newEpisode(file) {
                name = title
            })
        }

        return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
            seasonNames = seasons
        }
    }

    override suspend fun id2Path(file: GDFile): String {
        val driveNum = driverIds.indexOf(file.driveId)
        val id = file.id
        val text = app.get("$api/$driveNum:id2path?id=$id", headers = headers).text
        return AppUtils.tryParseJson<Path>(text)?.path
            ?: throw ErrorLoadingException("parse path data fail (id2path)")
    }
}
