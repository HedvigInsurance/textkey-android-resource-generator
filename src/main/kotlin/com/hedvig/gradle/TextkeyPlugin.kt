package com.hedvig.gradle

import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.BaseVariant
import com.beust.klaxon.Klaxon
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.gradle.api.DomainObjectCollection
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File

open class TextkeyPlugin : Plugin<Project> {
    lateinit var project: Project
    lateinit var extension: TextkeyExtension
    var data: Data? = null

    override fun apply(project: Project) {
        this.project = project

        val configuration = project.configurations.create("textkeys")
        configuration.isVisible = false
        configuration.description = "Dependencies required by the Hedvig TextKey Plugin"

        configuration.defaultDependencies { dependencies ->
            dependencies.add(project.dependencies.create("com.android.tools.build:gradle:3.2.1"))
            dependencies.add(project.dependencies.create("com.squareup.okhttp3:okhttp:3.14.0"))
            dependencies.add(project.dependencies.create("com.beust:klaxon:5.0.5"))
        }

        extension = project.extensions.create("textkeys", TextkeyExtension::class.java)

        getVariants().all { variant ->
            val variantName = variant.name.capitalize()
            val outputDir = "${project.buildDir}/generated/textkeys/res"
            val stringsTask = project.task("generateTextKeyStringResourcesFor$variantName") {
                if (data == null) {
                    data = callHttp()
                }
                makeStrings(outputDir)
            }
            stringsTask.description = "Generate Text Key string resources for $variantName"
            variant.registerGeneratedResFolders(project.files(outputDir).builtBy(stringsTask))
        }
    }

    fun getVariants(): DomainObjectCollection<out BaseVariant> {
        val libraryExtension = project.extensions.findByType(LibraryExtension::class.java)
        if (libraryExtension != null) {
            return libraryExtension.libraryVariants
        }

        val applicationExtension = project.extensions.findByType(AppExtension::class.java)
        if (applicationExtension != null) {
            return applicationExtension.applicationVariants
        }
        throw Error("Project must either be an Android Library or Android Application")
    }

    fun callHttp(): Data? {
        val client = OkHttpClient()
        val request = Request.Builder()
                .url(extension.graphqlEndpoint)
                .header("Accept", "application/json")
                .post(RequestBody.create(MediaType.get("application/json"), """{"query": "{\nlanguages {\ntranslations(where: { project: Android }) {\ntext\nkey {\nvalue\n}\n}\ncode\n}\n}","variables": null}"""))
                .build()
        val result = client.newCall(request).execute().body()?.string()
                ?: throw Error("Got no data from graphql endpoint")

        val response = Response.fromJson(result) ?: throw Error("Failed to parse body: $result")

/*        if (json.errors != null) {
            throw Error("Got errors when fetching text keys: ${json.errors}")
        }*/

        return response.data
    }

    fun makeStrings(outputDir: String) {
        data?.let { data ->
            data.languages.forEach { language ->
                if (extension.excludedLanguages.contains(language.code)) {
                    return@forEach
                }

                val buffer = StringBuilder()
                buffer.append("""<?xml version="1.0" encoding="utf-8"?>
<resources>
""")
                language.translations.forEach { translation ->
                    translation.key?.let { key ->
                        buffer.append("    <string name=\"${key.value}\"><![CDATA[${translation.text}]]></string>\n")
                    }
                }
                buffer.append("</resources>")
                val fileContents = buffer.toString()

                val code = language.code.replace("_", "-r")
                val file = File("$outputDir/values-$code/strings.xml")
                file.parentFile.mkdirs()
                file.createNewFile()
                file.writeText(fileContents)

                if (extension.defaultLanguageCode == language.code) {
                    val defaultFile = File("$outputDir/values/strings.xml")
                    defaultFile.parentFile.mkdirs()
                    defaultFile.createNewFile()
                    defaultFile.writeText(fileContents)
                }

            }
        }
    }
}

data class Response(
    val data: Data
) {
    companion object {
        fun fromJson(json: String) = Klaxon().parse<Response>(json)
    }
}

data class Data(
    val languages: List<Language>
)

data class Language(
    val translations: List<Translation>,
    val code: String
)

data class Translation(
    val text: String,
    val key: Key?
)

data class Key(
    val value: String
)
