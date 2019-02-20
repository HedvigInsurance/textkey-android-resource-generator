#![deny(clippy::pedantic)]

extern crate serde;

#[macro_use]
extern crate serde_derive;

use graphql_client::{GraphQLQuery, Response};
use reqwest;

#[derive(GraphQLQuery)]
#[graphql(
    schema_path = "src/graphql/schema.json",
    query_path = "src/graphql/textkeys.graphql",
    response_derives = "Debug"
)]
struct TextkeysQuery;

fn main() {
    let query = TextkeysQuery::build_query(textkeys_query::Variables {});
    dbg!(&query);
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
        let filename = format!("res/values-{}/strings.xml", language.code);
        dbg!(filename);
        //dbg!(language);
        let mut buffer = String::from(r#"
<?xml version="1.0" encoding="utf-8"?>
<resources>"#);
        let translations = language.translations.unwrap();
        for translation in translations {
            buffer.push_str(&format!("<string name=\"{}\">{}</string>", translation.key.unwrap().value, translation.text));
        }
        buffer.push_str("</resources>");
        println!("{}", buffer);
    }
}
