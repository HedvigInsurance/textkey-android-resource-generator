package com.hedvig.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.DomainObjectCollection
import com.android.build.gradle.api.BaseVariant
import org.gradle.api.plugins.ExtensionContainer

import static groovyx.net.http.HttpBuilder.configure

class TextkeyPlugin implements Plugin<Project> {
    private Project project
    private Map data
    private TextkeyExtension extension

    void apply(Project project) {
        this.project = project

        extension = project.extensions.create('textkeys', TextkeyExtension)

        getVariants().all { v -> 
            def variantName = v.name.capitalize()
            String outputDir = "$project.buildDir/generated/textkeys/res"
            Task stringsTask = project.task("generateTextKeyStringResourcesFor$variantName") {
                if (data == null) {
                    data = callHttp()
                }
                makeStrings(outputDir)
            }
            stringsTask.description = "Generate Text Key string resources for $variantName"
            v.registerGeneratedResFolders(project.files(outputDir).builtBy(stringsTask))
        }
    }

    private DomainObjectCollection<BaseVariant> getVariants() {
        return project.android.hasProperty('libraryVariants') ? project.android.libraryVariants : project.android.applicationVariants
    }

    private Map callHttp() {
        Map result = configure {
            request.raw = extension.graphqlEndpoint
            request.contentType = 'application/json'
            request.accept = 'application/json'
            request.body = '''
{
    "variables": null,
    "query": "\
        query TextKeys {\
            languages {\
                translations(where: {project: Android}) {\
                    text\
                    key {\
                        value\
                    }\
                }\
                code\
            }\
        }",
    "operationName": "TextKeys"
}'''
        }.post(Map) {}

        if (result['errors'] != null) {
            throw RuntimeException("Got errors when fetching text keys")
        }
        
        return result['data']
    }

    private void makeStrings(String outputDir) {
        for (language in data['languages']) {
            String code = language['code']
            def formattedCode = code.replace('_', '-r')
            def buffer = new StringBuilder()
            buffer.append('''<?xml version="1.0" encoding="utf-8"?>
<resources>
''')
            def translations = language['translations']
            for (translation in translations) {
                def key = translation['key']['value']
                buffer.append("    <string name=\"$key\"><![CDATA[$translation.text]]></string>\n")
            }
            buffer.append("</resources>")
            def fileContents = buffer.toString()

            def file = new File("$outputDir/values-$formattedCode/strings.xml")
            file.getParentFile().mkdirs()
            file.createNewFile()
            file.text = fileContents

            if (extension.defaultLanguageCode == code) {
                def defaultFile = new File("$outputDir/values/strings.xml")
                defaultFile.getParentFile().mkdirs()
                defaultFile.createNewFile()
                defaultFile.text = fileContents
            }
        }
    }
}
