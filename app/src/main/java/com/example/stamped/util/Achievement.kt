package com.example.stamped.util

import com.example.stamped.data.model.City
import com.example.stamped.data.model.Country

data class Achievement(
    val id: String,
    val titleEn: String,
    val titleMk: String,
    val descEn: String,
    val descMk: String,
    val emoji: String,
    val unlocked: Boolean,
    val colorHex: String = "#FFFFFF"
) {
    fun title(lang: String): String = if (lang == "mk") titleMk else titleEn
    fun description(lang: String): String = if (lang == "mk") descMk else descEn
}

object Achievements {

    fun evaluate(
        visited: List<Country>,
        cities: List<City> = emptyList()
    ): List<Achievement> {
        val count = visited.size
        val continents = visited.map { it.continent }.toSet()
        val europe = visited.count { it.continent == "Europe" }
        val asia = visited.count { it.continent == "Asia" }
        val africa = visited.count { it.continent == "Africa" }
        val northAmerica = visited.count { it.continent == "North America" }
        val southAmerica = visited.count { it.continent == "South America" }
        val oceania = visited.count { it.continent == "Oceania" }

        val cityCount = cities.size
        val capitalCount = cities.count { it.isCapital }

        return listOf(
            Achievement(
                "first_step", "First Step", "Прв чекор",
                "Visit your first country", "Посети ја првата земја",
                "👣", count >= 1,
                colorHex = "#FDE68A"  // soft yellow
            ),
            Achievement(
                "explorer_5", "Explorer", "Истражувач",
                "Visit 5 countries", "Посети 5 земји",
                "🗺️", count >= 5,
                colorHex = "#67E8F9"  // cyan
            ),
            Achievement(
                "traveler_10", "Traveler", "Патник",
                "Visit 10 countries", "Посети 10 земји",
                "✈️", count >= 10,
                colorHex = "#7DD3FC"  // sky blue
            ),
            Achievement(
                "globetrotter_25", "Globetrotter", "Светски патник",
                "Visit 25 countries", "Посети 25 земји",
                "🌍", count >= 25,
                colorHex = "#86EFAC"  // emerald
            ),
            Achievement(
                "wanderer_50", "Wanderer", "Скитник",
                "Visit 50 countries", "Посети 50 земји",
                "🧭", count >= 50,
                colorHex = "#BEF264"  // lime
            ),
            Achievement(
                "centurion_100", "Centurion", "Стогодишник",
                "Visit 100 countries", "Посети 100 земји",
                "💯", count >= 100,
                colorHex = "#FACC15"  // gold
            ),
            Achievement(
                "europe_master", "Europe Master", "Мајстор на Европа",
                "Visit 20 European countries", "Посети 20 европски земји",
                "🏰", europe >= 20,
                colorHex = "#C4B5FD"  // lavender
            ),
            Achievement(
                "asia_master", "Asia Master", "Мајстор на Азија",
                "Visit 15 Asian countries", "Посети 15 азиски земји",
                "🏯", asia >= 15,
                colorHex = "#F9A8D4"  // soft pink
            ),
            Achievement(
                "africa_master", "Africa Master", "Мајстор на Африка",
                "Visit 15 African countries", "Посети 15 африкански земји",
                "🦁", africa >= 15,
                colorHex = "#FCD34D"  // amber
            ),
            Achievement(
                "americas_master", "Americas Master", "Мајстор на Америките",
                "Visit 10 countries in the Americas",
                "Посети 10 земји во Америките",
                "🗽", (northAmerica + southAmerica) >= 10,
                colorHex = "#FDA4AF"  // rose
            ),
            Achievement(
                "oceania_explorer", "Oceania Explorer", "Истражувач на Океанија",
                "Visit 5 countries in Oceania", "Посети 5 земји во Океанија",
                "🏝️", oceania >= 5,
                colorHex = "#5EEAD4"  // aqua
            ),
            Achievement(
                "all_continents", "World Citizen", "Граѓанин на светот",
                "Visit at least one country on every continent",
                "Посети барем една земја од секој континент",
                "🌐", continents.size >= 6,
                colorHex = "#DDD6FE"  // soft violet
            ),
            // ===== City achievements =====
            Achievement(
                "city_explorer", "Urban Explorer", "Урбан истражувач",
                "Visit 5 cities", "Посети 5 градови",
                "🏙️", cityCount >= 5,
                colorHex = "#FED7AA"  // peach
            ),
            Achievement(
                "metropolitan", "Metropolitan", "Метрополит",
                "Visit 25 cities", "Посети 25 градови",
                "🌆", cityCount >= 25,
                colorHex = "#FCA5A5"  // coral
            ),
            Achievement(
                "city_globetrotter", "City Globetrotter", "Градски номад",
                "Visit 50 cities", "Посети 50 градови",
                "🌃", cityCount >= 50,
                colorHex = "#A5B4FC"  // indigo
            ),
            Achievement(
                "capital_collector", "Capital Collector", "Колекционер на главни градови",
                "Visit 10 capital cities", "Посети 10 главни градови",
                "👑", capitalCount >= 10,
                colorHex = "#FFD700"  // bright gold
            )
        )
    }
}
