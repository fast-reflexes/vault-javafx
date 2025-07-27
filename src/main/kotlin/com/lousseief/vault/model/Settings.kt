package com.lousseief.vault.model

data class Settings(
    var passwordLength: Int = 20,
    var categories: MutableList<String> = mutableListOf(),
    var savePasswordForMinutes: Int = 2
    //var blocksEncoded: Int = 0
)
