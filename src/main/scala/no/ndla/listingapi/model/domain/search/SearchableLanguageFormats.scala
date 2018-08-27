package no.ndla.listingapi.model.domain.search

import no.ndla.listingapi.model.domain.Label
import no.ndla.listingapi.model.domain.search.Language.UnknownLanguage
import org.json4s.{CustomSerializer, MappingException}
import org.json4s.JsonAST.{JArray, JField, JObject, JString}

class SearchableLanguageValueSerializer
    extends CustomSerializer[SearchableLanguageValues](_ =>
      ({
        case JObject(items) =>
          SearchableLanguageValues(items.map {
            case JField(name, JString(value)) => LanguageValue(name, value)
          })
      }, {
        case x: SearchableLanguageValues =>
          JObject(
            x.languageValues
              .map(languageValue =>
                JField(languageValue.lang, JString(languageValue.value)))
              .toList)
      }))

class SearchableLanguageListSerializer
    extends CustomSerializer[SearchableLanguageList](format =>
      ({
        case JObject(items) => {
          val res = items.map {
            case JField(name, JArray(fieldItems)) =>
              LanguageValue(
                name,
                fieldItems
                  .map {
                    case JObject(Seq(JField("type", JString(type_)),
                                     JField("labels", JArray(x)))) =>
                      val labels = x.map {
                        case JString(label) => label
                        case unknownType =>
                          throw new MappingException(
                            s"Cannot convert $unknownType to Label")
                      }

                      Label(Some(type_), labels)
                    case JObject(Seq(JField("labels", JArray(x)))) =>
                      val labels = x.map {
                        case JString(label) => label
                        case unknownType =>
                          throw new MappingException(
                            s"Cannot convert $unknownType to Label")
                      }

                      Label(None, labels)
                    case x =>
                      throw new MappingException(
                        s"Cannot convert $x to SearchableLanguageList")
                  }
                  .to[Seq]
              )
          }

          SearchableLanguageList(res)
        }
      }, {
        case x: SearchableLanguageList =>
          JObject(
            x.languageValues
              .map(languageValue =>
                JField(
                  languageValue.lang,
                  JArray(languageValue.value
                    .map(lv => {
                      lv.`type` match {
                        case Some(typ) =>
                          JObject(
                            JField("type", JString(typ)),
                            JField(
                              "labels",
                              JArray(lv.labels.map(l => JString(l)).toList)))
                        case None =>
                          JObject(
                            JField(
                              "labels",
                              JArray(lv.labels.map(l => JString(l)).toList)))
                      }
                    })
                    .toList)
              ))
              .toList)
      }))

object SearchableLanguageFormats {
  val JSonFormats = org.json4s.DefaultFormats + new SearchableLanguageValueSerializer + new SearchableLanguageListSerializer
}
