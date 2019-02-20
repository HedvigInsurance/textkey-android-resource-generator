#![deny(clippy::pedantic)]

extern crate serde;

#[macro_use]
extern crate serde_derive;

use graphql_client::{GraphQLQuery, Response};
use reqwest;

use std::fs;
use std::io::prelude::*;

#[derive(GraphQLQuery)]
#[graphql(
    schema_path = "src/graphql/schema.json",
    query_path = "src/graphql/textkeys.graphql",
    response_derives = "Debug"
)]
struct TextkeysQuery;

fn main() {
    let query = TextkeysQuery::build_query(textkeys_query::Variables {});
    let res: Response<textkeys_query::ResponseData> = reqwest::Client::new()
        .post("https://graphql.dev.hedvigit.com/graphql")
        .json(&query)
        .send()
        .unwrap()
        .json()
        .unwrap();

    if res.errors.is_some() {
        panic!("Request errored: {:?}", res.errors);
    }

    let data = res.data.unwrap();
    for language in data.languages {
        let language = language.unwrap();
        let directory = format!("output/res/values-{}", language.code);
        fs::create_dir_all(&directory).unwrap();
        let mut buffer = String::from(
            r#"<?xml version="1.0" encoding="utf-8"?>
<resources>
"#,
        );
        let translations = language.translations.unwrap();
        for translation in translations {
            buffer.push_str(&format!(
                r#"    <string name="{}">{}</string>
"#,
                translation.key.unwrap().value,
                translation.text
            ));
        }
        buffer.push_str("</resources>");
        let mut file = fs::File::create(format!("{}/strings.xml", directory)).unwrap();
        file.write_all(buffer.as_bytes()).unwrap();
    }
}
