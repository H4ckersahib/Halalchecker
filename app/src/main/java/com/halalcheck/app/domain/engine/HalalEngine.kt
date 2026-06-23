package com.halalcheck.app.domain.engine

import com.halalcheck.app.domain.model.AnalyzedIngredient
import com.halalcheck.app.domain.model.HalalStatus
import com.halalcheck.app.domain.model.Ingredient
import com.halalcheck.app.domain.model.Madhab
import com.halalcheck.app.domain.model.Product
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The HalalEngine is the core analysis component of HalalCheck.
 * It parses ingredient lists, matches them against the ingredient database,
 * and produces a halal status assessment based on the user's chosen madhab.
 *
 * v2: Enhanced with qualifier-aware parsing, context resolution,
 * and vegan/vegetarian label support.
 */
@Singleton
class HalalEngine @Inject constructor() {

    // ─────────────────────────────────────────────────
    // Qualifier definitions for context-aware analysis
    // ─────────────────────────────────────────────────

    /** Qualifiers that indicate a plant/synthetic source → resolves MASHBOOH to HALAL */
    private val halalQualifiers = setOf(
        // Soja / Soy variants
        "soja", "soy", "sojabohne",
        // Sonnenblume / Sunflower variants
        "sonnenblume", "sunflower",
        // Pflanzlich / Vegetable variants
        "pflanzlich", "pflanzliche", "pflanzlicher", "pflanzliches", "pflanzlichem", "pflanzlichen",
        "vegetable", "plant", "vegan", "végétal",
        // Raps / Rapeseed
        "raps", "rapeseed", "canola",
        // Mais / Corn
        "mais", "corn",
        // Kokos / Coconut
        "kokos", "coconut", "kokosnuss",
        // Andere pflanzliche Quellen
        "palm", "olive", "weizen", "wheat", "reis", "rice", "kartoffel", "potato",
        "erbse", "pea", "lein", "flax", "hanf", "hemp", "mandel", "almond",
        // Synthetisch / Microbial
        "synthetisch", "synthetic", "mikrobiell", "microbial", "bakteriell", "bacterial",
        // Früchte
        "citrus", "zitrus", "apfel", "apple", "trauben", "grape",
        // Tier-basiert aber halal
        "biene", "bee", "fisch", "fish",
        // Weitere
        "tapioka", "tapioca", "maniok", "cassava", "shea", "karite",
        "alge", "algae", "seaweed", "seetang",
        // Nüsse
        "erdnuss", "peanut", "haselnuss", "hazelnut", "walnuss", "walnut",
        "cashew", "pistazie", "macadamia"
    )

    /** Qualifiers that indicate a haram source → resolves MASHBOOH to HARAM */
    private val haramQualifiers = setOf(
        "schwein", "schweine", "pork", "porc", "pig",
        "schweinefleisch", "schweinefett", "schweineschmalz",
        "swine", "suino"
    )

    /** Qualifiers that indicate a possibly-halal animal source → stays MASHBOOH */
    private val animalQualifiers = setOf(
        "rind", "rinder", "beef", "bovine", "cattle",
        "kalb", "veal", "calf",
        "huhn", "hähnchen", "chicken", "poultry", "geflügel",
        "lamm", "lamb", "schaf", "sheep",
        "tier", "tierisch", "tierische", "tierischer", "tierisches", "tierischem", "tierischen",
        "animal"
    )

    /**
     * Analyze a product's ingredients and determine its halal status.
     * Now enhanced with:
     * - Qualifier-aware parsing (e.g., "Lecithin (Soja)" → HALAL)
     * - Compound ingredient matching before splitting
     * - Vegan/Vegetarian label-based resolution
     */
    fun analyzeProduct(
        product: Product,
        knownIngredients: List<Ingredient>,
        madhab: Madhab
    ): Product {
        val parsedIngredients = parseIngredientsListV2(product.ingredientsText)

        if (parsedIngredients.isEmpty()) {
            return product.copy(
                halalStatus = HalalStatus.UNKNOWN,
                confidence = 0f,
                analyzedIngredients = emptyList(),
                unknownIngredients = emptyList()
            )
        }

        val isVeganProduct = product.isLabeledVeganOrVegetarian()
        val analyzedList = mutableListOf<AnalyzedIngredient>()
        val unknownList = mutableListOf<String>()

        for (parsed in parsedIngredients) {
            analyzeIngredientEntry(
                parsed = parsed,
                knownIngredients = knownIngredients,
                madhab = madhab,
                isVeganProduct = isVeganProduct,
                analyzedList = analyzedList,
                unknownList = unknownList
            )
        }

        val overallStatus = determineOverallStatus(analyzedList)
        val confidence = calculateConfidence(analyzedList, unknownList)

        return product.copy(
            halalStatus = overallStatus,
            confidence = confidence,
            analyzedIngredients = analyzedList,
            unknownIngredients = unknownList
        )
    }

    /**
     * Analyze a single parsed ingredient entry, applying qualifier logic.
     */
    private fun analyzeIngredientEntry(
        parsed: ParsedIngredient,
        knownIngredients: List<Ingredient>,
        madhab: Madhab,
        isVeganProduct: Boolean,
        analyzedList: MutableList<AnalyzedIngredient>,
        unknownList: MutableList<String>
    ) {
        // Step 1: Try matching the FULL text (e.g. "Sojalecithin" or "Lecithin (Soja)")
        val fullTextMatch = findIngredientMatch(parsed.fullText, knownIngredients)
        if (fullTextMatch != null) {
            val ruling = fullTextMatch.getRulingForMadhab(madhab)
            analyzedList.add(
                AnalyzedIngredient(
                    name = parsed.displayName,
                    ingredient = fullTextMatch,
                    status = ruling,
                    reason = fullTextMatch.reasonDe ?: fullTextMatch.reason
                )
            )
            return
        }

        // Step 2: Match the main name
        val mainMatch = findIngredientMatch(parsed.mainName, knownIngredients)
        if (mainMatch != null) {
            var ruling = mainMatch.getRulingForMadhab(madhab)
            var reason = mainMatch.reasonDe ?: mainMatch.reason

            // Step 3: Apply qualifier logic if the ingredient is MASHBOOH
            if (ruling == HalalStatus.MASHBOOH && parsed.qualifiers.isNotEmpty()) {
                val qualifierResult = resolveWithQualifiers(parsed.qualifiers)
                if (qualifierResult != null) {
                    ruling = qualifierResult.first
                    reason = qualifierResult.second
                }
            }

            // Step 4: Apply vegan/vegetarian label logic for MASHBOOH emulsifiers/additives
            if (ruling == HalalStatus.MASHBOOH && isVeganProduct) {
                if (isSourceAmbiguousIngredient(mainMatch)) {
                    ruling = HalalStatus.HALAL
                    reason = "Produkt ist als vegan/vegetarisch gekennzeichnet → pflanzliche Quelle"
                }
            }

            analyzedList.add(
                AnalyzedIngredient(
                    name = parsed.displayName,
                    ingredient = mainMatch,
                    status = ruling,
                    reason = reason
                )
            )
        } else {
            unknownList.add(parsed.displayName)
            analyzedList.add(
                AnalyzedIngredient(
                    name = parsed.displayName,
                    ingredient = null,
                    status = HalalStatus.UNKNOWN,
                    reason = null
                )
            )
        }
    }

    /**
     * Check qualifiers to resolve a MASHBOOH ingredient.
     * Returns (HalalStatus, reason) or null if qualifiers don't resolve it.
     */
    private fun resolveWithQualifiers(qualifiers: List<String>): Pair<HalalStatus, String>? {
        val qualifiersLower = qualifiers.map {
            it.lowercase().trim()
                .replace(Regex("[()\\[\\].:;]"), "") // strip stray parens/punctuation
                .trim()
        }.filter { it.isNotBlank() }

        // Check for haram qualifiers first (more specific = higher priority)
        for (q in qualifiersLower) {
            if (haramQualifiers.any { q.contains(it) }) {
                return HalalStatus.HARAM to "Qualifier '$q' deutet auf verbotene Quelle (Schwein) hin"
            }
        }

        // Check for halal qualifiers (plant/synthetic source)
        for (q in qualifiersLower) {
            if (halalQualifiers.any { q.contains(it) }) {
                return HalalStatus.HALAL to "Qualifier '$q' deutet auf pflanzliche/synthetische Quelle hin → Halal"
            }
        }

        // Check for animal qualifiers (keeps MASHBOOH but adds context)
        for (q in qualifiersLower) {
            if (animalQualifiers.any { q.contains(it) }) {
                return HalalStatus.MASHBOOH to "Tierische Quelle ($q) – Halal-Schlachtung nicht bestätigt"
            }
        }

        return null
    }

    /**
     * Check if an ingredient is the kind whose source (animal/plant) is typically ambiguous.
     * These are the ones where a vegan/vegetarian label can resolve them to HALAL.
     */
    private fun isSourceAmbiguousIngredient(ingredient: Ingredient): Boolean {
        val ambiguousIds = setOf(
            "e322", "e422", "e441", "e470a", "e471", "e472", "e473", "e475", "e476",
            "e481", "e491", "e570", "e627", "e631", "e635", "e904", "e920",
            "gelatin", "glycerin", "lecithin", "whey", "whey_powder", "casein",
            "natural_flavors", "aroma", "lipase", "collagen", "rennet",
            "stearic_acid", "animal_fat", "beef_fat", "vitamin_d"
        )
        return ingredient.id.lowercase() in ambiguousIds ||
               ingredient.defaultCategory == HalalStatus.MASHBOOH
    }

    // ─────────────────────────────────────────────────
    // v2 Parser: Context-aware ingredient parsing
    // ─────────────────────────────────────────────────

    /**
     * Represents a parsed ingredient with context.
     */
    data class ParsedIngredient(
        /** The full original text, e.g. "Emulgator (Lecithine (Soja))" */
        val fullText: String,
        /** The main ingredient name, e.g. "Lecithine" */
        val mainName: String,
        /** The display name shown to the user */
        val displayName: String,
        /** Qualifier/source hints from parentheses, e.g. ["Soja"] */
        val qualifiers: List<String> = emptyList()
    )

    /**
     * Parse ingredients list into structured ParsedIngredient objects
     * that preserve qualifier context.
     *
     * Examples:
     * - "Zucker, Weizenmehl, Wasser" → 3 simple entries
     * - "Emulgator (Lecithine (Soja)), Salz" → Lecithine with qualifier [Soja]
     * - "Mono- und Diglyceride (pflanzlich)" → E471 with qualifier [pflanzlich] → HALAL
     */
    fun parseIngredientsListV2(ingredientsText: String): List<ParsedIngredient> {
        if (ingredientsText.isBlank()) return emptyList()

        // Remove percentage indicators like "10%", "5,3%"
        var cleaned = ingredientsText.replace(Regex("\\d+[,.]?\\d*\\s*%"), "")

        // Replace non-standard separators
        cleaned = cleaned.replace(";", ",")
            .replace("·", ",")
            .replace("•", ",")
            .replace(" - ", ",")

        val result = mutableListOf<ParsedIngredient>()
        val topLevelParts = splitTopLevel(cleaned)

        for (part in topLevelParts) {
            val trimmed = part.trim()
            if (trimmed.isBlank() || trimmed.length <= 1) continue

            val parsed = parseIngredientWithContext(trimmed)
            result.addAll(parsed)
        }

        return result.distinctBy { normalizeIngredientName(it.mainName).lowercase() }
    }

    /**
     * Split text at top-level commas (not inside parentheses).
     */
    private fun splitTopLevel(text: String): List<String> {
        val parts = mutableListOf<String>()
        var current = StringBuilder()
        var depth = 0

        for (char in text) {
            when {
                char == '(' -> { depth++; current.append(char) }
                char == ')' -> { depth--; current.append(char) }
                char == ',' && depth == 0 -> {
                    parts.add(current.toString())
                    current = StringBuilder()
                }
                else -> current.append(char)
            }
        }
        parts.add(current.toString())
        return parts
    }

    /**
     * Parse a single ingredient entry that may have parenthetical context.
     *
     * Handles patterns like:
     * - "Lecithine (Soja)" → main=Lecithine, qualifiers=[Soja]
     * - "Emulgator (E471)" → returns both "Emulgator" and "E471"
     * - "Pflanzliche Öle (Raps, Sonnenblume)" → sub-ingredients
     * - "Gelatine (Schwein)" → main=Gelatine, qualifiers=[Schwein]
     */
    private fun parseIngredientWithContext(text: String): List<ParsedIngredient> {
        val trimmed = text.trim()
        val parenMatch = Regex("^(.+?)\\((.+)\\)\\s*$").find(trimmed)

        if (parenMatch == null) {
            // Simple ingredient without parentheses
            val normalized = normalizeIngredientName(trimmed)
            if (normalized.isBlank()) return emptyList()
            return listOf(
                ParsedIngredient(
                    fullText = normalized,
                    mainName = normalized,
                    displayName = normalized
                )
            )
        }

        val mainPart = parenMatch.groupValues[1].trim()
        val parenContent = parenMatch.groupValues[2].trim()

        // Determine if parentheses contain:
        // (a) A qualifier/source (e.g., "Soja", "pflanzlich", "Schwein")
        // (b) E-numbers (e.g., "E471")
        // (c) Sub-ingredients (e.g., "Raps, Sonnenblume, Palm")

        val subParts = splitTopLevel(parenContent).map { it.trim() }.filter { it.isNotBlank() }

        // Check if this looks like a qualifier (single word or known qualifier)
        val isQualifier = subParts.size == 1 && isKnownQualifier(subParts[0])
        val isENumber = subParts.size == 1 && subParts[0].matches(Regex("E\\s*\\d+[a-z]?", RegexOption.IGNORE_CASE))
        val isSingleSource = subParts.size == 1 && subParts[0].split(" ").size <= 3

        return when {
            isENumber -> {
                // "Emulgator (E471)" → return both as separate items, E-number is the real ingredient
                val eNumber = normalizeIngredientName(subParts[0])
                listOf(
                    ParsedIngredient(
                        fullText = normalizeIngredientName(trimmed),
                        mainName = eNumber,
                        displayName = "${normalizeIngredientName(mainPart)} ($eNumber)",
                        qualifiers = emptyList()
                    )
                )
            }
            isQualifier || isSingleSource -> {
                // "Lecithine (Soja)" or "Gelatine (Schwein)" → qualifier context
                val mainNorm = normalizeIngredientName(mainPart)
                // Also try the combined form: "Sojalecithin" for the full text match
                val combinedName = normalizeIngredientName("${subParts[0]}${mainPart}")
                listOf(
                    ParsedIngredient(
                        fullText = combinedName,
                        mainName = mainNorm,
                        displayName = "${mainNorm} (${subParts[0]})",
                        qualifiers = subParts
                    )
                )
            }
            else -> {
                // Multiple sub-ingredients: "Pflanzliche Öle (Raps, Sonnenblume, Palm)"
                val results = mutableListOf<ParsedIngredient>()
                val mainNorm = normalizeIngredientName(mainPart)

                // Add the main ingredient
                if (mainNorm.isNotBlank()) {
                    results.add(
                        ParsedIngredient(
                            fullText = mainNorm,
                            mainName = mainNorm,
                            displayName = mainNorm,
                            qualifiers = subParts
                        )
                    )
                }

                // Add sub-ingredients (they may themselves have parentheses)
                for (sub in subParts) {
                    results.addAll(parseIngredientWithContext(sub))
                }

                results
            }
        }
    }

    /**
     * Check if a word is a known qualifier (source/origin hint).
     * Case-insensitive, also strips stray parentheses and punctuation.
     */
    private fun isKnownQualifier(text: String): Boolean {
        val lower = text.lowercase().trim()
            .replace(Regex("[()\\[\\].:;]"), "") // strip stray parens/punctuation
            .trim()
        if (lower.isBlank()) return false
        return halalQualifiers.any { lower.contains(it) } ||
               haramQualifiers.any { lower.contains(it) } ||
               animalQualifiers.any { lower.contains(it) }
    }

    /**
     * Normalize an ingredient name for matching purposes.
     */
    fun normalizeIngredientName(name: String): String {
        return name.trim()
            .replace(Regex("\\s+"), " ")
            .replace(Regex("[_.\\-]"), " ")
            .replace(Regex("^(enthält|contains|aus|from|davon|thereof)\\s+", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\.$"), "") // Remove trailing period
            .trim()
    }

    /**
     * Find a matching ingredient in the database using normalized matching.
     * Tries exact match first, then E-number, then partial matching.
     */
    fun findIngredientMatch(
        ingredientName: String,
        knownIngredients: List<Ingredient>
    ): Ingredient? {
        val normalized = normalizeIngredientName(ingredientName).lowercase()

        if (normalized.isBlank()) return null

        // 1. Exact match on ID or any language name
        val exactMatch = knownIngredients.find { ingredient ->
            ingredient.id.lowercase() == normalized ||
            ingredient.nameDe.lowercase() == normalized ||
            ingredient.nameEn.lowercase() == normalized ||
            (ingredient.nameTr?.lowercase() == normalized) ||
            (ingredient.nameAr?.lowercase() == normalized)
        }
        if (exactMatch != null) return exactMatch

        // 2. E-number match (e.g., "E471", "e 471", "E-471")
        val eNumberMatch = Regex("e\\s*-?\\s*(\\d+[a-z]?)", RegexOption.IGNORE_CASE)
            .find(normalized)
        if (eNumberMatch != null) {
            val eNumber = "e${eNumberMatch.groupValues[1]}".lowercase()
            val match = knownIngredients.find { it.id.lowercase() == eNumber }
            if (match != null) return match
        }

        // 3. Compound form match (e.g., "sojalecithin" → find "soy_lecithin")
        val withoutSpaces = normalized.replace(" ", "")
        val compoundMatch = knownIngredients.find { ingredient ->
            val names = listOfNotNull(
                ingredient.nameDe.lowercase().replace(" ", ""),
                ingredient.nameEn.lowercase().replace(" ", ""),
                ingredient.id.lowercase().replace("_", "")
            )
            names.any { it == withoutSpaces }
        }
        if (compoundMatch != null) return compoundMatch

        // 4. Contains match (e.g., "Mono- und Diglyceride von Speisefettsäuren" → E471)
        // Only match if one name clearly contains the other (minimum 4 chars to avoid false positives)
        if (normalized.length >= 4) {
            val containsMatch = knownIngredients.find { ingredient ->
                val names = listOfNotNull(
                    ingredient.nameDe.lowercase(),
                    ingredient.nameEn.lowercase(),
                    ingredient.nameTr?.lowercase(),
                    ingredient.nameAr?.lowercase()
                )
                names.any { name ->
                    (name.length >= 4 && normalized.contains(name)) ||
                    (normalized.length >= 4 && name.contains(normalized))
                }
            }
            if (containsMatch != null) return containsMatch
        }

        return null
    }

    /**
     * Determine the overall halal status based on analyzed ingredients.
     *
     * Rules:
     * - Any HARAM ingredient → Product is HARAM
     * - No HARAM but any MASHBOOH → Product is MASHBOOH
     * - All HALAL → Product is HALAL
     * - Only UNKNOWN → Product is UNKNOWN
     */
    fun determineOverallStatus(analyzedIngredients: List<AnalyzedIngredient>): HalalStatus {
        if (analyzedIngredients.isEmpty()) return HalalStatus.UNKNOWN

        val statuses = analyzedIngredients.map { it.status }

        return when {
            statuses.any { it == HalalStatus.HARAM } -> HalalStatus.HARAM
            statuses.any { it == HalalStatus.MASHBOOH } -> HalalStatus.MASHBOOH
            statuses.all { it == HalalStatus.UNKNOWN } -> HalalStatus.UNKNOWN
            statuses.any { it == HalalStatus.UNKNOWN } -> {
                // Some known (HALAL) + some unknown → MASHBOOH (caution)
                if (statuses.any { it == HalalStatus.HALAL }) HalalStatus.MASHBOOH
                else HalalStatus.UNKNOWN
            }
            else -> HalalStatus.HALAL
        }
    }

    /**
     * Calculate confidence score (0.0 - 1.0) based on how many ingredients were identified.
     */
    fun calculateConfidence(
        analyzedIngredients: List<AnalyzedIngredient>,
        unknownIngredients: List<String>
    ): Float {
        val total = analyzedIngredients.size
        if (total == 0) return 0f

        val known = analyzedIngredients.count { it.status != HalalStatus.UNKNOWN }
        return known.toFloat() / total.toFloat()
    }

    // ─────────────────────────────────────────────────
    // Legacy v1 parser (kept for reference)
    // ─────────────────────────────────────────────────

    /**
     * v1 parser - kept for backwards compatibility.
     * Use parseIngredientsListV2 for new code.
     */
    fun parseIngredientsList(ingredientsText: String): List<String> {
        return parseIngredientsListV2(ingredientsText).map { it.displayName }
    }
}
