package com.example.stamped.util

object TravelTips {

    private val en = listOf(
        "The world is a book, and those who do not travel read only one page. — Saint Augustine",
        "Travel makes one modest. You see what a tiny place you occupy in the world. — Gustave Flaubert",
        "Not all those who wander are lost. — J.R.R. Tolkien",
        "Adventure is worthwhile. — Aesop",
        "We travel not to escape life, but for life not to escape us. — Anonymous",
        "Life is short, and the world is wide.",
        "Travel far enough, you meet yourself. — David Mitchell",
        "Once a year, go someplace you've never been before. — Dalai Lama",
        "To travel is to live. — Hans Christian Andersen",
        "Jobs fill your pocket, but adventures fill your soul. — Jaime Lyn",
        "Take only memories, leave only footprints. — Chief Seattle",
        "The journey of a thousand miles begins with one step. — Lao Tzu",
        "Wherever you go becomes a part of you somehow. — Anita Desai",
        "Travel is the only thing you buy that makes you richer.",
        "Collect moments, not things."
    )

    private val mk = listOf(
        "Светот е книга, а оние што не патуваат читаат само една страница. — Свети Августин",
        "Патувањето учи на скромност. Гледаш колку мало место заземаш во светот. — Густав Флобер",
        "Не сите што талкаат се изгубени. — Џ.Р.Р. Толкин",
        "Авантурата вреди. — Езоп",
        "Не патуваме за да побегнеме од животот, туку животот да не побегне од нас.",
        "Животот е краток, а светот е широк.",
        "Патувај доволно далеку и ќе се сретнеш со себе. — Дејвид Мичел",
        "Еднаш годишно, оди некаде каде никогаш не си бил. — Далај Лама",
        "Да патуваш значи да живееш. — Ханс Кристијан Андерсен",
        "Работите ти полнат џеб, авантурите ти полнат душа.",
        "Земи само спомени, остави само траги. — Поглавар Сиетл",
        "Патот од илјада милји почнува со еден чекор. — Лао Це",
        "Каде и да одиш, тоа место станува дел од тебе.",
        "Патувањето е единственото нешто кое го купуваш и те прави побогат.",
        "Собирај моменти, не работи."
    )

    fun random(language: String): String {
        val list = if (language == "mk") mk else en
        return list.random()
    }
}
