package com.halalcheck.app.domain.engine

import com.halalcheck.app.domain.model.AnalyzedIngredient
import com.halalcheck.app.domain.model.Doubt
import com.halalcheck.app.domain.model.HalalStatus
import com.halalcheck.app.domain.model.Madhab
import com.halalcheck.app.domain.model.Product
import com.halalcheck.app.domain.model.Reason
import com.halalcheck.app.domain.model.SourceIngredient
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The HalalEngine is the core analysis component of HalalCheck.
 *
 * It analyzes a product twice, from its ingredient text ([IngredientTextParser]) and from the
 * ingredient list Open Food Facts parsed itself (taxonomy ids), keeps whichever recognized more,
 * then adds doubtful additives that only Open Food Facts listed. Each ingredient gets the ruling of
 * the chosen madhab, adjusted by source qualifiers and the product's vegan/halal labels.
 */
@Singleton
class HalalEngine @Inject constructor() {

    fun analyzeProduct(
        product: Product,
        index: IngredientIndex,
        madhab: Madhab
    ): Product {
        val analysis = Analysis(
            index = index,
            madhab = madhab,
            veganLabel = product.hasVeganOrVegetarianLabel,
            halalLabel = product.hasHalalLabel
        )
        val fromText = dedupe(analysis.analyzeText(product.ingredientsText))
        val fromOff = dedupe(analysis.analyzeNodes(product.sourceIngredients))
        val chosen = if (fromOff.isNotEmpty() && (fromText.isEmpty() || knownRatio(fromOff) > knownRatio(fromText))) {
            fromOff
        } else {
            fromText
        }
        val analyzed = chosen + analysis.additivesNotListed(product.additivesTags, chosen)
        val hasHaram = analyzed.any { it.status == HalalStatus.HARAM }

        return product.copy(
            halalStatus = determineOverallStatus(analyzed, product.hasHalalLabel),
            confidence = calculateConfidence(analyzed),
            analyzedIngredients = analyzed,
            unknownIngredients = analyzed.filter { it.status == HalalStatus.UNKNOWN }.map { it.name },
            labelConflict = product.hasHalalLabel && hasHaram
        )
    }

    /**
     * Rules:
     * - Any HARAM ingredient → HARAM, even with a halal label
     * - Otherwise a halal label → HALAL
     * - Any MASHBOOH → MASHBOOH
     * - Any ingredient not recognized → UNKNOWN ("nicht vollständig geprüft")
     * - Otherwise HALAL
     */
    fun determineOverallStatus(analyzedIngredients: List<AnalyzedIngredient>, halalLabel: Boolean = false): HalalStatus {
        val statuses = analyzedIngredients.map { it.status }
        return when {
            statuses.isEmpty() -> if (halalLabel) HalalStatus.HALAL else HalalStatus.UNKNOWN
            HalalStatus.HARAM in statuses -> HalalStatus.HARAM
            halalLabel -> HalalStatus.HALAL
            HalalStatus.MASHBOOH in statuses -> HalalStatus.MASHBOOH
            HalalStatus.UNKNOWN in statuses -> HalalStatus.UNKNOWN
            else -> HalalStatus.HALAL
        }
    }

    /** Share of recognized ingredients (0.0 - 1.0); compound-head matches count half. */
    fun calculateConfidence(analyzedIngredients: List<AnalyzedIngredient>): Float {
        if (analyzedIngredients.isEmpty()) return 0f
        val known = analyzedIngredients.sumOf { ingredient ->
            when {
                ingredient.status == HalalStatus.UNKNOWN -> 0.0
                ingredient.inferred -> 0.5
                else -> 1.0
            }
        }
        return (known / analyzedIngredients.size).toFloat()
    }

    private fun knownRatio(list: List<AnalyzedIngredient>): Float =
        list.count { it.status != HalalStatus.UNKNOWN }.toFloat() / list.size

    /** One entry per ingredient (or unknown name); the most severe occurrence wins. */
    private fun dedupe(list: List<AnalyzedIngredient>): List<AnalyzedIngredient> {
        val byKey = LinkedHashMap<String, AnalyzedIngredient>()
        for (entry in list) {
            val key = entry.ingredient?.id ?: IngredientIndex.normalize(entry.name)
            val existing = byKey[key]
            if (existing == null || severity(entry.status) > severity(existing.status)) byKey[key] = entry
        }
        return byKey.values.toList()
    }

    private class Analysis(
        val index: IngredientIndex,
        val madhab: Madhab,
        val veganLabel: Boolean,
        val halalLabel: Boolean
    ) {
        private val parser = IngredientTextParser(
            isClassName = index::isClassName,
            isKnown = { index.exact(it) != null }
        )

        fun analyzeText(text: String): List<AnalyzedIngredient> =
            parser.parse(text).flatMap { analyzeParsed(it) }

        private fun analyzeParsed(parsed: ParsedIngredient): List<AnalyzedIngredient> {
            if (parsed.qualifiers.isNotEmpty()) {
                // "Emulgator (Sojalecithin)": the recognized content is more precise than the class
                if (parsed.isClass) {
                    val children = parsed.children.flatMap { analyzeParsed(it) }
                    if (children.any { it.ingredient != null }) return children
                }
                val main = analyzeName(parsed.text, parsed.name, parsed.qualifiers)
                return if (main != null) listOf(main) else parsed.children.flatMap { analyzeParsed(it) }
            }
            if (parsed.isListHead) {
                // Its composition follows after the colon; only a HARAM head adds information.
                val head = analyzeName(parsed.text, parsed.name, emptyList())
                return if (head?.status == HalalStatus.HARAM) listOf(head) else emptyList()
            }
            if (parsed.children.isEmpty()) {
                return listOf(analyzeName(parsed.text, parsed.name, emptyList()) ?: unknown(parsed.text))
            }
            // A composition: "Schokolade (Zucker, Kakaobutter)"
            val children = parsed.children.flatMap { analyzeParsed(it) }
            if (parsed.isClass) return children
            val parent = analyzeName(parsed.name, parsed.name, emptyList())
            return withParent(parent, parsed.children.size, children)
        }

        /** Same rule as the text parser: a bare class followed by more entries is a typo for a colon. */
        fun analyzeNodes(nodes: List<SourceIngredient>): List<AnalyzedIngredient> =
            nodes.filterIndexed { i, node ->
                i == nodes.lastIndex || node.children.isNotEmpty() || node.id?.let { index.byOffId(it) }?.isClass != true
            }.flatMap { analyzeNode(it) }

        /** Open Food Facts' own parse: taxonomy id first, our text matching as fallback. */
        private fun analyzeNode(node: SourceIngredient): List<AnalyzedIngredient> {
            val ingredient = node.id?.let { index.byOffId(it) }
            if (node.children.isNotEmpty()) {
                val children = analyzeNodes(node.children)
                if (ingredient?.isClass == true) return children
                val parent = ingredient?.let { evaluate(node.text, IngredientMatch(it), emptyList()) }
                    ?: analyzeName(node.text, node.text, emptyList())
                return withParent(parent, node.children.size, children)
            }
            if (ingredient != null) return listOf(evaluate(node.text, IngredientMatch(ingredient), emptyList()))
            return analyzeText(node.text).ifEmpty { listOf(unknown(node.text)) }
        }

        /**
         * A recognized parent counts next to its composition if it is HARAM ("Speck (Schweinefleisch, Salz)")
         * or has a single child, which is usually an annotation ("Molke (Milch)"). Otherwise the
         * composition answers the parent's doubt ("Milchschokolade (Zucker, Kakaobutter, ...)").
         */
        private fun withParent(
            parent: AnalyzedIngredient?,
            childCount: Int,
            children: List<AnalyzedIngredient>
        ): List<AnalyzedIngredient> =
            if (parent != null && (parent.status == HalalStatus.HARAM || childCount == 1)) listOf(parent) + children
            else children

        private fun analyzeName(display: String, name: String, qualifiers: List<String>): AnalyzedIngredient? {
            // "Lecithine (Soja)" -> "Sojalecithine"
            val combined = qualifiers.firstNotNullOfOrNull { q -> index.exact(q + name) ?: index.exact("$q $name") }
            val matches = combined?.let { listOf(IngredientMatch(it)) } ?: index.findAll(name)
            if (matches.isEmpty()) return null
            return matches.map { evaluate(display, it, qualifiers) }.maxBy { severity(it.status) }
        }

        fun evaluate(display: String, match: IngredientMatch, qualifiers: List<String>): AnalyzedIngredient {
            val ingredient = match.ingredient
            var status = ingredient.getRulingForMadhab(madhab)
            var reason: Reason = when {
                match.inferred -> Reason.Inferred(ingredient)
                ingredient.isClass -> Reason.NotSpecified(ingredient)
                else -> Reason.Database(ingredient)
            }

            val sources = (listOfNotNull(match.modifier) + qualifiers).mapNotNull { word ->
                Qualifiers.kindOf(word)?.let { word to it }
            }
            val haram = sources.firstOrNull { it.second == QualifierKind.HARAM }?.first
            val animal = sources.firstOrNull { it.second == QualifierKind.ANIMAL }?.first
            val halalSource = sources.firstOrNull { it.second == QualifierKind.HALAL_SOURCE }?.first
            when {
                haram != null -> {
                    status = HalalStatus.HARAM
                    reason = Reason.HaramQualifier(haram)
                }
                // "Rinderspeck", "Schmalz (Gans)": pork only by default
                animal != null && status == HalalStatus.HARAM && isPorkByDefault(ingredient.id, ingredient.inferredFrom) -> {
                    status = HalalStatus.MASHBOOH
                    reason = Reason.AnimalQualifier(animal)
                }
                animal != null && status == HalalStatus.MASHBOOH -> reason = Reason.AnimalQualifier(animal)
                halalSource != null && status == HalalStatus.MASHBOOH -> {
                    status = HalalStatus.HALAL
                    reason = Reason.HalalSourceQualifier(halalSource)
                }
            }

            if (status == HalalStatus.MASHBOOH && veganLabel && animal == null && ingredient.doubt == Doubt.ANIMAL_SOURCE) {
                status = HalalStatus.HALAL
                reason = Reason.VeganLabel
            }
            if (status == HalalStatus.MASHBOOH && halalLabel) {
                status = HalalStatus.HALAL
                reason = Reason.HalalLabel
            }
            return AnalyzedIngredient(display, ingredient, status, reason, match.inferred)
        }

        /** Doubtful or haram additives from Open Food Facts' additive list that the ingredient list did not show. */
        fun additivesNotListed(additivesTags: List<String>, analyzed: List<AnalyzedIngredient>): List<AnalyzedIngredient> {
            val present = analyzed.mapNotNull { a -> a.ingredient?.let { eBase(it.eNumber ?: it.id) } }.toSet()
            return additivesTags
                .mapNotNull { index.byOffId(it) }
                .distinctBy { eBase(it.eNumber ?: it.id) }
                .filter { eBase(it.eNumber ?: it.id) !in present }
                .map { evaluate((it.eNumber ?: it.id).uppercase(), IngredientMatch(it), emptyList()) }
                .filter { it.status == HalalStatus.HARAM || it.status == HalalStatus.MASHBOOH }
                .map { if (it.reason is Reason.Database) it.copy(reason = Reason.OffAdditive(it.ingredient!!)) else it }
        }

        private fun unknown(name: String) = AnalyzedIngredient(name, null, HalalStatus.UNKNOWN)
    }

    private companion object {
        val E_BASE = Regex("^(e\\d{3,4})")

        /** Curated entries that are HARAM because pork is the default animal ("Speck", "Schinken"). */
        val PORK_BY_DEFAULT = setOf("pork", "pork_fat", "lard", "bacon", "ham")

        fun isPorkByDefault(id: String, inferredFrom: String?) = id in PORK_BY_DEFAULT || inferredFrom in PORK_BY_DEFAULT

        fun eBase(id: String): String = E_BASE.find(id)?.value ?: id

        fun severity(status: HalalStatus): Int = when (status) {
            HalalStatus.HALAL -> 0
            HalalStatus.UNKNOWN -> 1
            HalalStatus.MASHBOOH -> 2
            HalalStatus.HARAM -> 3
        }
    }
}
