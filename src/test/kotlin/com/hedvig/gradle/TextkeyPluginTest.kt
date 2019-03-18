package com.hedvig.gradle

import com.android.build.gradle.LibraryExtension
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class Tests {
    @Test
    fun `it registers tasks properly`() {
        val project = ProjectBuilder
                .builder()
                .build()
        project.pluginManager.apply(TextkeyPlugin::class.java)
        project.extensions.add(LibraryExtension::class.java, "libraryExtension", LibraryExtension())

        assertTrue(project.pluginManager.hasPlugin("com.hedvig.textkeys"))

        //assertNotNull(project.tasks.getByName)
    }

    @Test
    fun `it can fetch data from graphql`() {
        val sut = TextkeyPlugin().apply {
            extension = TextkeyExtension().apply {
                graphqlEndpoint = "https://api-euwest.graphcms.com/v1/cjmawd9hw036a01cuzmjhplka/master"
                defaultLanguageCode = "sv_SE"
            }
        }
        print(sut.callHttp())
    }
}
