package app.tinks.tink.geography

import android.content.Context
import org.json.JSONArray
import kotlin.random.Random

data class GeographyCountry(
    val id: String,
    val cca2: String,
    val chineseShortName: String,
    val chineseFullName: String,
    val englishName: String,
    val localName: String,
    val population: String,
    val area: String,
    val currency: String,
    val capital: String,
    val region: String,
    val latitude: Float,
    val longitude: Float,
    val flagEmoji: String,
)

enum class GeographyQuizType(val label: String) {
    CountryToFlag("国家猜国旗"),
    CountryToMap("国家猜地图"),
    MapToCountry("地图猜国家"),
    FlagToCountry("国旗猜国家"),
}

data class GeographyQuestion(
    val type: GeographyQuizType,
    val answer: GeographyCountry,
    val options: List<GeographyCountry>,
)

object GeographyData {
    fun loadCountries(context: Context): List<GeographyCountry> =
        context.assets.open("geography/countries.json")
            .bufferedReader()
            .use { loadCountriesFromJson(it.readText()) }

    fun loadCountriesFromJson(json: String): List<GeographyCountry> {
        val array = JSONArray(json)
        return List(array.length()) { index ->
            val item = array.getJSONObject(index)
            GeographyCountry(
                id = item.getString("id"),
                cca2 = item.getString("cca2"),
                chineseShortName = item.getString("chineseShortName"),
                chineseFullName = item.getString("chineseFullName"),
                englishName = item.getString("englishName"),
                localName = item.getString("localName"),
                population = item.getString("population"),
                area = item.getString("area"),
                currency = item.getString("currency"),
                capital = item.getString("capital"),
                region = item.getString("region"),
                latitude = item.getDouble("latitude").toFloat(),
                longitude = item.getDouble("longitude").toFloat(),
                flagEmoji = item.optString("flagEmoji"),
            )
        }
    }

    val previewCountries: List<GeographyCountry> = listOf(
        GeographyCountry(
            id = "china",
            cca2 = "CN",
            chineseShortName = "中国",
            chineseFullName = "中华人民共和国",
            englishName = "China",
            localName = "中国",
            population = "约 14.1 亿",
            area = "约 960 万平方公里",
            currency = "人民币 CNY",
            capital = "北京",
            region = "东亚",
            latitude = 35.0f,
            longitude = 103.0f,
            flagEmoji = "🇨🇳",
        ),
        GeographyCountry(
            id = "japan",
            cca2 = "JP",
            chineseShortName = "日本",
            chineseFullName = "日本国",
            englishName = "Japan",
            localName = "日本",
            population = "约 1.24 亿",
            area = "约 37.8 万平方公里",
            currency = "日元 JPY",
            capital = "东京",
            region = "东亚",
            latitude = 36.0f,
            longitude = 138.0f,
            flagEmoji = "🇯🇵",
        ),
        GeographyCountry(
            id = "united_states",
            cca2 = "US",
            chineseShortName = "美国",
            chineseFullName = "美利坚合众国",
            englishName = "United States",
            localName = "United States",
            population = "约 3.35 亿",
            area = "约 983 万平方公里",
            currency = "美元 USD",
            capital = "华盛顿哥伦比亚特区",
            region = "北美洲",
            latitude = 39.0f,
            longitude = -98.0f,
            flagEmoji = "🇺🇸",
        ),
        GeographyCountry(
            id = "canada",
            cca2 = "CA",
            chineseShortName = "加拿大",
            chineseFullName = "加拿大",
            englishName = "Canada",
            localName = "Canada",
            population = "约 4000 万",
            area = "约 998 万平方公里",
            currency = "加拿大元 CAD",
            capital = "渥太华",
            region = "北美洲",
            latitude = 57.0f,
            longitude = -106.0f,
            flagEmoji = "🇨🇦",
        ),
        GeographyCountry(
            id = "brazil",
            cca2 = "BR",
            chineseShortName = "巴西",
            chineseFullName = "巴西联邦共和国",
            englishName = "Brazil",
            localName = "Brasil",
            population = "约 2.03 亿",
            area = "约 851 万平方公里",
            currency = "巴西雷亚尔 BRL",
            capital = "巴西利亚",
            region = "南美洲",
            latitude = -10.0f,
            longitude = -55.0f,
            flagEmoji = "🇧🇷",
        ),
        GeographyCountry(
            id = "australia",
            cca2 = "AU",
            chineseShortName = "澳大利亚",
            chineseFullName = "澳大利亚联邦",
            englishName = "Australia",
            localName = "Australia",
            population = "约 2660 万",
            area = "约 769 万平方公里",
            currency = "澳大利亚元 AUD",
            capital = "堪培拉",
            region = "大洋洲",
            latitude = -25.0f,
            longitude = 133.0f,
            flagEmoji = "🇦🇺",
        ),
        GeographyCountry(
            id = "france",
            cca2 = "FR",
            chineseShortName = "法国",
            chineseFullName = "法兰西共和国",
            englishName = "France",
            localName = "France",
            population = "约 6800 万",
            area = "约 64.4 万平方公里",
            currency = "欧元 EUR",
            capital = "巴黎",
            region = "欧洲",
            latitude = 46.0f,
            longitude = 2.0f,
            flagEmoji = "🇫🇷",
        ),
        GeographyCountry(
            id = "germany",
            cca2 = "DE",
            chineseShortName = "德国",
            chineseFullName = "德意志联邦共和国",
            englishName = "Germany",
            localName = "Deutschland",
            population = "约 8400 万",
            area = "约 35.7 万平方公里",
            currency = "欧元 EUR",
            capital = "柏林",
            region = "欧洲",
            latitude = 51.0f,
            longitude = 10.0f,
            flagEmoji = "🇩🇪",
        ),
        GeographyCountry(
            id = "india",
            cca2 = "IN",
            chineseShortName = "印度",
            chineseFullName = "印度共和国",
            englishName = "India",
            localName = "भारत",
            population = "约 14.3 亿",
            area = "约 329 万平方公里",
            currency = "印度卢比 INR",
            capital = "新德里",
            region = "南亚",
            latitude = 22.0f,
            longitude = 79.0f,
            flagEmoji = "🇮🇳",
        ),
        GeographyCountry(
            id = "egypt",
            cca2 = "EG",
            chineseShortName = "埃及",
            chineseFullName = "阿拉伯埃及共和国",
            englishName = "Egypt",
            localName = "مصر",
            population = "约 1.13 亿",
            area = "约 100 万平方公里",
            currency = "埃及镑 EGP",
            capital = "开罗",
            region = "非洲东北部",
            latitude = 26.0f,
            longitude = 30.0f,
            flagEmoji = "🇪🇬",
        ),
        GeographyCountry(
            id = "south_africa",
            cca2 = "ZA",
            chineseShortName = "南非",
            chineseFullName = "南非共和国",
            englishName = "South Africa",
            localName = "South Africa",
            population = "约 6200 万",
            area = "约 122 万平方公里",
            currency = "南非兰特 ZAR",
            capital = "比勒陀利亚",
            region = "非洲南部",
            latitude = -30.0f,
            longitude = 25.0f,
            flagEmoji = "🇿🇦",
        ),
        GeographyCountry(
            id = "united_kingdom",
            cca2 = "GB",
            chineseShortName = "英国",
            chineseFullName = "大不列颠及北爱尔兰联合王国",
            englishName = "United Kingdom",
            localName = "United Kingdom",
            population = "约 6800 万",
            area = "约 24.4 万平方公里",
            currency = "英镑 GBP",
            capital = "伦敦",
            region = "欧洲西北部",
            latitude = 54.0f,
            longitude = -2.0f,
            flagEmoji = "🇬🇧",
        ),
    )
}

fun List<GeographyCountry>.filterByCountryName(query: String): List<GeographyCountry> {
    val normalized = query.trim()
    if (normalized.isBlank()) return this
    return filter { country ->
        country.chineseShortName.contains(normalized, ignoreCase = true) ||
            country.chineseFullName.contains(normalized, ignoreCase = true) ||
            country.englishName.contains(normalized, ignoreCase = true) ||
            country.localName.contains(normalized, ignoreCase = true)
    }
}

fun buildGeographyQuestion(
    countries: List<GeographyCountry>,
    type: GeographyQuizType,
    random: Random = Random.Default,
): GeographyQuestion {
    require(countries.size >= 4) { "At least four countries are required to build a geography quiz." }

    val answer = countries.random(random)
    val distractors = countries
        .filterNot { it.id == answer.id }
        .shuffled(random)
        .take(3)

    return GeographyQuestion(
        type = type,
        answer = answer,
        options = (distractors + answer).shuffled(random),
    )
}
