package app.tinks.tink.random

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

interface RandomSettingsStore {
    val settings: StateFlow<RandomSettings>
    fun save(settings: RandomSettings)
}

@Singleton
class RandomRepository @Inject constructor(@ApplicationContext context: Context) : RandomSettingsStore {
    private val prefs = context.getSharedPreferences("random_settings", Context.MODE_PRIVATE)
    private val mutableSettings = MutableStateFlow(decode(prefs.getString("settings", null)))
    override val settings = mutableSettings.asStateFlow()

    override fun save(settings: RandomSettings) {
        require(settings.isValid())
        prefs.edit().putString("settings", encode(settings)).apply()
        mutableSettings.value = settings
    }

    internal companion object {
        fun encode(value: RandomSettings): String = JSONObject().apply {
            put("type", value.type.name); put("diceCount", value.diceCount)
            put("wheelName", value.wheelName); put("sound", value.sound); put("speech", value.speech)
            put("equalWheelSections", value.equalWheelSections)
            put("sections", JSONArray().apply {
                value.sections.forEach { put(JSONObject().put("content", it.content).put("weight", it.weight)) }
            })
        }.toString()

        fun decode(json: String?): RandomSettings = runCatching {
            val obj = JSONObject(requireNotNull(json))
            val sections = obj.getJSONArray("sections")
            RandomSettings(
                type = RandomType.valueOf(obj.getString("type")), diceCount = obj.getInt("diceCount"),
                wheelName = obj.getString("wheelName"), sound = obj.getBoolean("sound"), speech = obj.getBoolean("speech"),
                equalWheelSections = obj.optBoolean("equalWheelSections", false),
                sections = List(sections.length()) { i -> sections.getJSONObject(i).let { WheelSection(it.getString("content"), it.getInt("weight")) } },
            ).also { require(it.isValid()) }
        }.getOrDefault(RandomSettings())
    }
}
