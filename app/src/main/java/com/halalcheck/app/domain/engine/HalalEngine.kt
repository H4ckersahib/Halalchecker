package com.halalcheck.app.domain.engine

import com.halalcheck.app.domain.model.AnalyzedIngredient
import com.halalcheck.app.domain.model.DietFlag
import com.halalcheck.app.domain.model.Doubt
import com.halalcheck.app.domain.model.HalalStatus
import com.halalcheck.app.domain.model.Ingredient
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
 * ingredient list Open Food Facts parsed itself (taxonomy ids). It keeps the result that recognized
 * more, adds doubtful or haram findings only the other one had, then adds doubtful additives that
 * only Open Food Facts listed. Each ingredient gets the ruling of the chosen madhab, adjusted by
 * source qualifiers and the labels on the package (vegan/vegetarian if consistent with the
 * ingredients, a halal label only when the user confirmed it).
 */
@Singleton
class HalalEngine @Inject constructor() {

    fun analyzeProduct(
        product: Product,
        index: IngredientIndex,
        madhab: Madhab
    ): Product {
        val halalConfirmed = product.halalConfirmed
        val withoutDietLabel = analyzeAll(product, Analysis(index, madhab, dietLabel = false, halalLabel = halalConfirmed))

        // Each vegan/vegetarian claim is checked on its own; one the ingredients contradict is not used.
        val ingredients = withoutDietLabel.ingredients
        val veganConflicts = if (product.claimsVegan) conflictsWith(ingredients) { it.vegan == DietFlag.NO } else emptyList()
        val vegetarianConflicts = if (product.claimsVegetarian) {
            conflictsWith(ingredients) { it.vegetarian == DietFlag.NO }
        } else {
            emptyList()
        }
        val dietLabelValid = (product.claimsVegan && veganConflicts.isEmpty()) ||
            (product.claimsVegetarian && vegetarianConflicts.isEmpty())
        val result = if (dietLabelValid) {
            analyzeAll(product, Analysis(index, madhab, dietLabel = true, halalLabel = halalConfirmed))
        } else {
            withoutDietLabel
        }

        val analyzed = result.ingredients
        val hasHaram = analyzed.any { it.status == HalalStatus.HARAM }
        return product.copy(
            halalStatus = determineOverallStatus(analyzed),
            confidence = calculateConfidence(analyzed),
            analyzedIngredients = analyzed,
            unknownIngredients = analyzed.filter { it.status == HalalStatus.UNKNOWN }.map { it.name },
            labelConflict = (product.offHalal || halalConfirmed) && hasHaram,
            dietLabelWouldResolve = analyzed.any {
                it.status == HalalStatus.MASHBOOH && it.ingredient?.doubt == Doubt.ANIMAL_SOURCE &&
                    it.reason !is Reason.AnimalQualifier
            },
            veganConflicts = veganConflicts,
            vegetarianConflicts = vegetarianConflicts,
            sourceConflict = result.sourceConflict,
            nameMentions = if (hasHaram) emptyList() else nameMentions(product.name, analyzed, index, madhab),
            ignoredText = result.ignoredText
        )
    }

    /**
     * "Effekt Vodka Energie" without an ingredient list: the name is a hint to check the package, not a
     * verdict. Whole words only, so "Glücksschwein" (marzipan) does not count.
     */
    private fun nameMentions(
        name: String,
        analyzed: List<AnalyzedIngredient>,
        index: IngredientIndex,
        madhab: Madhab
    ): List<Ingredient> {
        val listed = analyzed.mapNotNull { it.ingredient?.let(::family) }.toSet()
        return index.findAll(name)
            .filter { !it.inferred && it.ingredient.getRulingForMadhab(madhab) == HalalStatus.HARAM }
            .map { it.ingredient }
            .filter { family(it) !in listed }
            .distinctBy { family(it) }
    }

    private class AnalysisResult(
        val ingredients: List<AnalyzedIngredient>,
        val sourceConflict: Boolean,
        val ignoredText: List<String>
    )

    private fun analyzeAll(product: Product, analysis: Analysis): AnalysisResult {
        val (fromText, textIgnored) = withoutLabelText(dedupe(analysis.analyzeText(product.ingredientsText)))
        val (fromOff, offIgnored) = withoutLabelText(dedupe(analysis.analyzeNodes(product.sourceIngredients)))
        // OFF parses only the product's main language: with a German text and a French main language its
        // parse would list "Eau, protéines de soja". It then only adds what the text missed.
        val sameLanguage = product.sourceIngredientsLanguage == null || product.ingredientsLanguage == null ||
            product.sourceIngredientsLanguage == product.ingredientsLanguage
        val offWins = fromOff.isNotEmpty() &&
            (fromText.isEmpty() || (sameLanguage && knownRatio(fromOff) > knownRatio(fromText)))
        val (chosen, other) = if (offWins) fromOff to fromText else fromText to fromOff
        // Doubtful or haram findings of the other source must not disappear with it.
        val chosenFamilies = chosen.mapNotNull { it.ingredient?.let(::family) }.toSet()
        val missing = other.filter {
            (it.status == HalalStatus.HARAM || it.status == HalalStatus.MASHBOOH) &&
                it.ingredient?.let(::family) !in chosenFamilies
        }
        val merged = chosen + missing
        return AnalysisResult(
            merged + analysis.additivesNotListed(product.additivesTags, merged),
            missing.isNotEmpty(),
            if (offWins) offIgnored else textIgnored
        )
    }

    /**
     * An unrecognized phrase of five or more words with a German or English function word is label text
     * the parser's note rules missed ("Weitere Informationen unter ra.org"), not an ingredient. It is
     * shown separately and does not make the product "not fully checked".
     */
    private fun withoutLabelText(list: List<AnalyzedIngredient>): Pair<List<AnalyzedIngredient>, List<String>> {
        val (text, ingredients) = list.partition { it.ingredient == null && isLabelText(it.name) }
        return ingredients to text.map { it.name }
    }

    private fun isLabelText(phrase: String): Boolean {
        val words = WORD.findAll(phrase.lowercase()).map { it.value }.toList()
        return words.size >= LABEL_TEXT_WORDS && words.any { it in FUNCTION_WORDS }
    }

    /** Names of recognized ingredients that contradict a diet claim. Compound guesses don't count. */
    private fun conflictsWith(ingredients: List<AnalyzedIngredient>, contradicts: (Ingredient) -> Boolean): List<String> =
        ingredients.filter { entry ->
            val ingredient = entry.ingredient ?: return@filter false
            !entry.inferred && entry.reason !is Reason.HalalSourceQualifier && contradicts(ingredient)
        }.map { it.name }.distinct()

    /**
     * Rules:
     * - Any HARAM ingredient → HARAM
     * - Any MASHBOOH → MASHBOOH
     * - Any ingredient not recognized, or nothing to analyze → UNKNOWN ("nicht vollständig geprüft")
     * - Otherwise HALAL
     * A confirmed halal label only resolves doubtful ingredients (see [Analysis.evaluate]).
     */
    fun determineOverallStatus(analyzedIngredients: List<AnalyzedIngredient>): HalalStatus {
        val statuses = analyzedIngredients.map { it.status }
        return when {
            statuses.isEmpty() -> HalalStatus.UNKNOWN
            HalalStatus.HARAM in statuses -> HalalStatus.HARAM
            HalalStatus.MASHBOOH in statuses -> HalalStatus.MASHBOOH
            HalalStatus.UNKNOWN in statuses -> HalalStatus.UNKNOWN
            else -> HalalStatus.HALAL
        }
    }

    /** Share of recognized ingredients (0.0 - 1.0); compound-head matches count half. Not a religious certainty. */
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
        /** A vegan/vegetarian label that the ingredients don't contradict. */
        val dietLabel: Boolean,
        /** A halal label the user confirmed on the package. */
        val halalLabel: Boolean
    ) {
        private val parser = IngredientTextParser(
            isClassName = index::isClassName,
            isKnown = { index.exact(it) != null },
            isAdditive = { text -> index.findAll(text).any { it.ingredient.eNumber != null } }
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

        /** Same rule as the text parser: a bare class directly followed by an additive is a typo for a colon. */
        fun analyzeNodes(nodes: List<SourceIngredient>): List<AnalyzedIngredient> =
            nodes.filterIndexed { i, node ->
                val bareClass = node.children.isEmpty() && node.id?.let { index.byOffId(it) }?.isClass == true
                !bareClass || nodes.getOrNull(i + 1)?.let(::isAdditiveNode) != true
            }.flatMap { analyzeNode(it) }

        private fun isAdditiveNode(node: SourceIngredient): Boolean =
            node.id?.let { index.byOffId(it) }?.eNumber != null ||
                index.findAll(node.text).any { it.ingredient.eNumber != null }

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
         * A recognized parent counts next to its composition if
         * - it is HARAM ("Speck (Schweinefleisch, Salz)"),
         * - it has a single child, usually an annotation ("Süßmolkenpulver (aus Milch)"); a HALAL or
         *   unrecognized annotation (a Latin name: "Chiasamen (Salvia hispanica)") adds nothing and is not listed,
         * - or it is doubtful in a way its composition can't answer ("Wurst (Salz, Gewürze)": the
         *   slaughter; "Aroma (Wasser, Zucker)": the flavouring itself).
         * Doubts that only ask what is inside are answered by the composition: OFF-derived "may contain
         * animal products" ("Füllung (Zucker, Palmfett)") and curated entries marked compositionResolves
         * ("Milchschokolade (Zucker, Kakaobutter, ...)").
         */
        private fun withParent(
            parent: AnalyzedIngredient?,
            childCount: Int,
            children: List<AnalyzedIngredient>
        ): List<AnalyzedIngredient> = when {
            parent == null -> children
            parent.status == HalalStatus.HARAM -> listOf(parent) + children
            childCount == 1 -> listOf(parent) + children.filter { it.status != HalalStatus.HALAL && it.ingredient != null }
            parent.status == HalalStatus.MASHBOOH && compositionCannotAnswer(parent.ingredient) -> listOf(parent) + children
            else -> children
        }

        private fun compositionCannotAnswer(ingredient: Ingredient?): Boolean =
            ingredient != null && !ingredient.compositionResolves && ingredient.source?.startsWith("off_") != true

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
            val sourceDoubt = ingredient.doubt == Doubt.ANIMAL_SOURCE
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
                // A plant source answers "animal or plant?", not alcohol or slaughter ("Sojasoße (Soja)")
                halalSource != null && status == HalalStatus.MASHBOOH && sourceDoubt -> {
                    status = HalalStatus.HALAL
                    reason = Reason.HalalSourceQualifier(halalSource)
                }
            }

            if (status == HalalStatus.MASHBOOH && dietLabel && animal == null && sourceDoubt) {
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
        val WORD = Regex("\\p{L}{2,}")
        const val LABEL_TEXT_WORDS = 5
        val FUNCTION_WORDS = setOf(
            "der", "die", "das", "den", "dem", "des", "und", "oder", "unter", "nach", "von", "vor", "mit", "bei",
            "zu", "zum", "zur", "für", "ist", "sind", "wird", "werden", "kann", "sie", "eine", "einem", "einer",
            "the", "and", "for", "with", "under", "after", "is", "are", "can", "more"
        )

        /** Curated entries that are HARAM because pork is the default animal ("Speck", "Schinken"). */
        val PORK_BY_DEFAULT = setOf("pork", "pork_fat", "lard", "bacon", "ham")

        fun isPorkByDefault(id: String, inferredFrom: String?) = id in PORK_BY_DEFAULT || inferredFrom in PORK_BY_DEFAULT

        fun eBase(id: String): String = E_BASE.find(id)?.value ?: id

        /** The same ingredient under different ids: E-number family, or the curated entry it inherits from. */
        fun family(ingredient: Ingredient): String = ingredient.eNumber?.let(::eBase) ?: ingredient.inferredFrom ?: ingredient.id

        fun severity(status: HalalStatus): Int = when (status) {
            HalalStatus.HALAL -> 0
            HalalStatus.UNKNOWN -> 1
            HalalStatus.MASHBOOH -> 2
            HalalStatus.HARAM -> 3
        }
    }
}
