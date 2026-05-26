package com.example.stamped.util

import android.content.Context

object CountryLocale {

    private val mkNames: Map<String, String> = mapOf(
        // Africa
        "DZ" to "Алжир", "AO" to "Ангола", "BJ" to "Бенин", "BW" to "Боцвана",
        "BF" to "Буркина Фасо", "BI" to "Бурунди", "CM" to "Камерун",
        "CV" to "Кабо Верде", "CF" to "Централноафриканска Република", "TD" to "Чад",
        "KM" to "Комори", "CG" to "Република Конго", "CD" to "ДР Конго",
        "CI" to "Брег на Слоновача", "DJ" to "Џибути", "EG" to "Египет",
        "GQ" to "Екваторијална Гвинеја", "ER" to "Еритреа", "SZ" to "Есватини",
        "ET" to "Етиопија", "GA" to "Габон", "GM" to "Гамбија", "GH" to "Гана",
        "GN" to "Гвинеја", "GW" to "Гвинеја-Бисао", "KE" to "Кенија",
        "LS" to "Лесото", "LR" to "Либерија", "LY" to "Либија",
        "MG" to "Мадагаскар", "MW" to "Малави", "ML" to "Мали",
        "MR" to "Мавританија", "MU" to "Маврициус", "MA" to "Мароко",
        "MZ" to "Мозамбик", "NA" to "Намибија", "NE" to "Нигер",
        "NG" to "Нигерија", "RW" to "Руанда", "ST" to "Сао Томе и Принципе",
        "SN" to "Сенегал", "SL" to "Сиера Леоне", "SO" to "Сомалија",
        "ZA" to "Јужна Африка", "SS" to "Јужен Судан", "SD" to "Судан",
        "TZ" to "Танзанија", "TG" to "Того", "TN" to "Тунис",
        "UG" to "Уганда", "ZM" to "Замбија", "ZW" to "Зимбабве",
        // Asia
        "AF" to "Авганистан", "AM" to "Ерменија", "AZ" to "Азербејџан",
        "BH" to "Бахреин", "BD" to "Бангладеш", "BT" to "Бутан",
        "BN" to "Брунеј", "KH" to "Камбоџа", "CN" to "Кина", "CY" to "Кипар",
        "GE" to "Грузија", "IN" to "Индија", "ID" to "Индонезија",
        "IR" to "Иран", "IQ" to "Ирак", "IL" to "Израел", "JP" to "Јапонија",
        "JO" to "Јордан", "KZ" to "Казахстан", "KW" to "Кувајт",
        "KG" to "Киргистан", "LA" to "Лаос", "LB" to "Либан",
        "MY" to "Малезија", "MV" to "Малдиви", "MN" to "Монголија",
        "MM" to "Мјанмар", "NP" to "Непал", "KP" to "Северна Кореа",
        "OM" to "Оман", "PK" to "Пакистан", "PH" to "Филипини",
        "QA" to "Катар", "SA" to "Саудиска Арабија", "SG" to "Сингапур",
        "KR" to "Јужна Кореа", "LK" to "Шри Ланка", "SY" to "Сирија",
        "TW" to "Тајван", "TJ" to "Таџикистан", "TH" to "Тајланд",
        "TL" to "Тимор-Лесте", "TR" to "Турција", "TM" to "Туркменистан",
        "AE" to "ОАЕ", "UZ" to "Узбекистан", "VN" to "Виетнам", "YE" to "Јемен",
        // Europe
        "AL" to "Албанија", "AD" to "Андора", "AT" to "Австрија",
        "BY" to "Белорусија", "BE" to "Белгија", "BA" to "Босна и Херцеговина",
        "BG" to "Бугарија", "HR" to "Хрватска", "CZ" to "Чешка",
        "DK" to "Данска", "EE" to "Естонија", "FI" to "Финска",
        "FR" to "Франција", "DE" to "Германија", "GR" to "Грција",
        "HU" to "Унгарија", "IS" to "Исланд", "IE" to "Ирска",
        "IT" to "Италија", "XK" to "Косово", "LV" to "Латвија",
        "LI" to "Лихтенштајн", "LT" to "Литванија", "LU" to "Луксембург",
        "MT" to "Малта", "MD" to "Молдавија", "MC" to "Монако",
        "ME" to "Црна Гора", "NL" to "Холандија", "MK" to "Северна Македонија",
        "NO" to "Норвешка", "PL" to "Полска", "PT" to "Португалија",
        "RO" to "Романија", "RU" to "Русија", "SM" to "Сан Марино",
        "RS" to "Србија", "SK" to "Словачка", "SI" to "Словенија",
        "ES" to "Шпанија", "SE" to "Шведска", "CH" to "Швајцарија",
        "UA" to "Украина", "GB" to "Обединето Кралство", "VA" to "Ватикан",
        // North America
        "AG" to "Антигва и Барбуда", "BS" to "Бахами", "BB" to "Барбадос",
        "BZ" to "Белизе", "CA" to "Канада", "CR" to "Костарика",
        "CU" to "Куба", "DM" to "Доминика", "DO" to "Доминиканска Република",
        "SV" to "Ел Салвадор", "GD" to "Гренада", "GT" to "Гватемала",
        "HT" to "Хаити", "HN" to "Хондурас", "JM" to "Јамајка",
        "MX" to "Мексико", "NI" to "Никарагва", "PA" to "Панама",
        "KN" to "Свети Китс и Невис", "LC" to "Света Луција",
        "VC" to "Свети Винсент и Гренадини", "TT" to "Тринидад и Тобаго",
        "US" to "САД",
        // South America
        "AR" to "Аргентина", "BO" to "Боливија", "BR" to "Бразил",
        "CL" to "Чиле", "CO" to "Колумбија", "EC" to "Еквадор",
        "GY" to "Гвајана", "PY" to "Парагвај", "PE" to "Перу",
        "SR" to "Суринам", "UY" to "Уругвај", "VE" to "Венецуела",
        // Oceania
        "AU" to "Австралија", "FJ" to "Фиџи", "KI" to "Кирибати",
        "MH" to "Маршалски Острови", "FM" to "Микронезија", "NR" to "Науру",
        "NZ" to "Нов Зеланд", "PW" to "Палау", "PG" to "Папуа Нова Гвинеја",
        "WS" to "Самоа", "SB" to "Соломонски Острови", "TO" to "Тонга",
        "TV" to "Тувалу", "VU" to "Вануату"
    )

    private val mkContinents: Map<String, String> = mapOf(
        "Africa" to "Африка",
        "Asia" to "Азија",
        "Europe" to "Европа",
        "North America" to "Северна Америка",
        "South America" to "Јужна Америка",
        "Oceania" to "Океанија"
    )

    fun getLocalizedName(code: String, defaultName: String, context: Context): String {
        val lang = context.resources.configuration.locales[0].language
        return if (lang == "mk") mkNames[code] ?: defaultName else defaultName
    }

    fun getLocalizedContinent(continent: String, context: Context): String {
        val lang = context.resources.configuration.locales[0].language
        return if (lang == "mk") mkContinents[continent] ?: continent else continent
    }
}
