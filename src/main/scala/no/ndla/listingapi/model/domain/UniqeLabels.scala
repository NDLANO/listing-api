package no.ndla.listingapi.model.domain

case class UniqeLabels (labelsByType: Map[LabelType, Set[String]])
