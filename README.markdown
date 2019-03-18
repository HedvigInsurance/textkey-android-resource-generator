# textkey-android-resource-generator

Download text keys from GraphQL, reformat them as string XML resources and make them available during build in Android.

## Usage

```groovy

apply plugin: 'com.hedvig.textkeys'

textkeys {
    graphqlEndpoint = 'https://your.graphql.endpoint/in/full'
    defaultLanguageCode = 'sv_SE' // For example
    excludedLangauges = ['en_SE'] // If you'd like only some languages to go live. This property is optional
}

```
