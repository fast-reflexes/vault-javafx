package com.lousseief.vault.utils

import javafx.scene.text.Font

class Font {

    companion object {
        // must write EXACTLY like this (there are many ways to load font but this is the only one that worked)
        val BitCount = Font.loadFont(javaClass.getResourceAsStream("/fonts/BitcountPropSingle_Roman-Medium.ttf"), 20.0)
    }

}
