package com.hedvig.gradle

import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.BaseVariant
import com.beust.klaxon.Klaxon
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.gradle.api.Action
import org.gradle.api.DomainObjectCollection
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
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

        extension = project.extensions.create("textkeys", TextkeyExtension::class.java)

        getVariants().all { variant ->
            val variantName = variant.name.capitalize()
            val outputDir = "${project.buildDir}/generated/textkeys/res"
            val taskName = "generateTextKeyStringResourcesFor$variantName"
            val stringsTask = project.task(hashMapOf("action" to Action<Task> {
                if (data == null) {
                    data = callHttp()
                }
                makeStrings(outputDir)
            }), taskName)
            stringsTask.description = "Generate Text Key string resources for $variantName"
            variant.registerGeneratedResFolders(project.files(outputDir).builtBy(stringsTask))
            variant.mergeResources.dependsOn(stringsTask)
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
                        buffer.append("    <string name=\"${key.value}\">\"<![CDATA[${translation.text}]]>\"</string>\n")
                    }
                }
                buffer.append("</resources>")
                val fileContents = buffer.toString()

                val code = language.code.replace("_", "-r")
                val pathname = "$outputDir/values-$code/strings.xml"
                val file = File(pathname)
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
        } ?: throw Error("Did not get any data")
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
