package com.hedvig.gradle

open class TextkeyExtension {
    lateinit var graphqlEndpoint: String
    lateinit var defaultLanguageCode: String
    var excludedLanguages: List<String> = emptyList()
}
