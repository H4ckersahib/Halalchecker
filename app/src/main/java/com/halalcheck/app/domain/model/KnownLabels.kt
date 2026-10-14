package com.halalcheck.app.domain.model

/**
 * Open Food Facts label tags that mean vegan / vegetarian / halal: the label itself and its
 * descendants in the OFF labels taxonomy (labels.full.json, 2026-10-14). Matched exactly, never by
 * substring, because OFF also has "en:non-vegan" and "en:non-vegetarian".
 */
object KnownLabels {
    val VEGAN: Set<String> = setOf(
        "en:vegan", "en:100-vegetable", "en:certified-ivegan", "en:certified-plant-based", "en:ecoveg",
        "en:european-vegetarian-union-vegan", "en:eve-vegan", "en:i-m-vegan", "en:plamil-foods-vegan",
        "en:plant-based", "en:the-vegan-society", "en:v-label-international-vegan", "en:vegan-action",
        "en:vegan-australia", "en:veganok", "en:vege-project-vegan", "en:vegecert-certified-vegan",
        "en:vegetarian-society-approved-vegan", "da:dansk-vegetarisk-forening-vegansk", "es:uve-vegano",
        "it:icea-bio-vegan", "it:icea-vegan", "it:qualita-vegetariana-vegan", "it:vegan-cert",
        "pl:viva-dla-wegetarian-i-wegan"
    )

    val VEGETARIAN: Set<String> = VEGAN + setOf(
        "en:vegetarian", "da:dansk-vegetarisk-forening", "da:dansk-vegetarisk-forening-vegetarisk",
        "en:european-vegetarian-union", "en:european-vegetarian-union-lacto-vegetarian",
        "en:european-vegetarian-union-ovo-lacto-vegetarian", "en:european-vegetarian-union-ovo-vegetarian",
        "en:european-vegetarian-union-vegetarian", "en:green-dot-india", "en:lacto-vegetarian",
        "en:ovo-lacto-vegetarian", "en:ovo-vegetarian", "en:v-label-international",
        "en:v-label-international-vegetarian", "en:vege-project", "en:vege-project-vegetarian",
        "en:vegetarian-society", "it:icea-bio-vegetariano", "it:icea-vegetariano"
    )

    val HALAL: Set<String> = setOf(
        "en:halal", "en:eurohalal", "en:halal-food-authority", "en:halal-food-council-of-europe",
        "en:halal-monitoring-committee", "en:islamic-food-and-nutrition-council-of-america",
        "en:islamic-food-and-nutrition-council-of-canada", "en:junta-islamica", "en:muslim-consumer-group",
        "en:muslim-council-international", "en:the-central-islamic-committee-of-thailand",
        "fr:a-votre-service", "fr:association-culturelle-musulmane-d-ile-de-france",
        "fr:association-rituelle-de-la-grande-mosquee-de-lyon", "fr:centre-islamique-aachen",
        "fr:controle-de-la-mosquee-d-evry-courcouronnes", "fr:halal-reunion", "fr:muslim-conseil-international",
        "fr:societe-francaise-de-controle-de-viande-halal"
    )
}
