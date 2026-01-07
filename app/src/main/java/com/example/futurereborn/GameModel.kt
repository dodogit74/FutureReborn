package com.example.futurereborn

import kotlin.math.max
import kotlin.math.min
import kotlin.math.floor
import kotlin.math.pow

typealias SkillId = String
typealias ActivityId = String
typealias JobId = String
typealias UpgradeId = String
typealias HousingId = String
typealias FoodId = String
typealias OtherId = String

data class SkillState(val level: Int = 1, val xp: Double = 0.0)

data class GameState(
    val credits: Double = 0.0,
    // 16 ans au départ, exprimé en jours
    val ageDays: Double = 16.0 * 365.0,
    val lifeSeconds: Double = 0.0,

    val activeActivity: ActivityId = "explore",
    val activeJob: JobId? = null,

    // Temps d'attente avant qu'un job commence réellement (en secondes réelles)
    val jobWaitSecondsRemaining: Double = 0.0,

    // Boutique : un choix exclusif pour Logement et Nourriture, et une catégorie "Autre" multi-achats
    val selectedHousing: HousingId = "shelter",
    val selectedFood: FoodId? = null,
    val ownedOther: Set<OtherId> = emptySet(),
    val activeOther: Set<OtherId> = emptySet(),

    val skills: Map<SkillId, SkillState> = mapOf(
        "strength" to SkillState(),
        "mind" to SkillState(),
        "charisma" to SkillState(),
        "tech" to SkillState(),
        "linguistics" to SkillState(),
        "adaptation" to SkillState()
    ),

    // ★ Maîtrise (niveau/XP) par activité et par job
    val activityMastery: Map<ActivityId, SkillState> = emptyMap(),
    val jobMastery: Map<JobId, SkillState> = emptyMap(),

    // Monnaie de réincarnation (prestige)
    val echoes: Int = 0,

    // Upgrades permanents (niveau par id)
    val upgrades: Map<UpgradeId, Int> = mapOf(
        "xp_boost" to 0,
        "credit_boost" to 0,
        "start_bonus" to 0
    ),

    val totalLives: Int = 0,

    // Progression narrative
    val storyFlags: Set<String> = emptySet(),
    val log: List<String> = listOf(
        "Tu ouvres les yeux. Le ciel est strié de lumières artificielles. Une cité inconnue bourdonne autour de toi.",
        "Tu ne comprends ni la langue, ni les règles. Mais tu es vivant."
    )
)

data class ActivityDef(
    val id: ActivityId,
    val name: String,
    val description: String,
    // XP / seconde par skill
    val xp: Map<SkillId, Double>,
    val creditsPerSec: Double = 0.0,
    val required: (GameState) -> Boolean = { true }
)

data class JobDef(
    val id: JobId,
    val name: String,
    val description: String,
    val creditsPerSec: Double,
    // XP / seconde par skill
    val xp: Map<SkillId, Double>,
    val required: (GameState) -> Boolean
)

data class UpgradeDef(
    val id: UpgradeId,
    val name: String,
    val description: String,
    val baseCost: Int,
    val costGrowth: Double
)

enum class ShopCategory { HOUSING, FOOD, OTHER }

data class ShopItemDef(
    val id: String,
    val name: String,
    val description: String,
    val category: ShopCategory,
    // Coût journalier (crédits par jour in-game)
    val costPerDay: Double,
    // Joie : augmente les gains d'XP (voir Engine)
    val joy: Int
)

object Defs {

    val activities: List<ActivityDef> = listOf(
        ActivityDef(
            id = "explore",
            name = "Explorer",
            description = "Observer la cité et survivre sans se faire remarquer.",
            xp = mapOf("adaptation" to 0.9, "linguistics" to 0.4),
            creditsPerSec = 0.05
        ),
        ActivityDef(
            id = "learn_language",
            name = "Apprendre la langue",
            description = "Imiter, mémoriser, comprendre. Les mots reviennent peu à peu.",
            xp = mapOf("linguistics" to 1.4, "mind" to 0.3, "adaptation" to 0.4)
        ),
        ActivityDef(
            id = "train_body",
            name = "S'entraîner",
            description = "La ville est dure. Ton corps doit suivre.",
            xp = mapOf("strength" to 1.2, "adaptation" to 0.2)
        ),
        ActivityDef(
            id = "study_tech",
            name = "Étudier la technologie",
            description = "Panneaux, drones, interfaces… tout est incompréhensible, mais pas impossible.",
            xp = mapOf("tech" to 1.2, "mind" to 0.6, "adaptation" to 0.3),
            required = { s -> (s.skills["linguistics"]?.level ?: 1) >= 3 }
        ),
        ActivityDef(
            id = "socialize",
            name = "Se socialiser",
            description = "Gagner la confiance, éviter les ennuis, apprendre les codes.",
            xp = mapOf("charisma" to 1.0, "linguistics" to 0.4, "adaptation" to 0.4),
            required = { s -> (s.skills["linguistics"]?.level ?: 1) >= 2 }
        )
    )

    val jobs: List<JobDef> = listOf(
        JobDef(
            id = "scrap_runner",
            name = "Coursier de débris",
            description = "Ramasser et livrer des pièces. Simple, fatigant, payé au crédit.",
            creditsPerSec = 0.9,
            xp = mapOf("strength" to 0.25, "adaptation" to 0.15),
            required = { s -> (s.skills["adaptation"]?.level ?: 1) >= 2 }
        ),
        JobDef(
            id = "translator_helper",
            name = "Aide-traducteur",
            description = "Faire l'interface entre anciens dialectes et argot futuriste.",
            creditsPerSec = 1.6,
            xp = mapOf("linguistics" to 0.35, "charisma" to 0.2, "adaptation" to 0.2),
            required = { s -> (s.skills["linguistics"]?.level ?: 1) >= 6 }
        ),
        JobDef(
            id = "tech_apprentice",
            name = "Apprenti technicien",
            description = "Réparer des modules. Tu apprends en faisant… et ça paie.",
            creditsPerSec = 2.4,
            xp = mapOf("tech" to 0.45, "mind" to 0.25, "adaptation" to 0.2),
            required = { s ->
                (s.skills["tech"]?.level ?: 1) >= 6 &&
                    (s.skills["adaptation"]?.level ?: 1) >= 5
            }
        ),
        JobDef(
            id = "district_mediator",
            name = "Médiateur de quartier",
            description = "Négocier, calmer, organiser. Ici, le chaos est une monnaie.",
            creditsPerSec = 3.2,
            xp = mapOf("charisma" to 0.5, "adaptation" to 0.25),
            required = { s ->
                (s.skills["charisma"]?.level ?: 1) >= 8 &&
                    (s.skills["linguistics"]?.level ?: 1) >= 7 &&
                    (s.skills["adaptation"]?.level ?: 1) >= 7
            }
        )
    )

    val upgrades: List<UpgradeDef> = listOf(
        UpgradeDef(
            id = "xp_boost",
            name = "Mémoire résiduelle",
            description = "+5% XP par niveau (permanent).",
            baseCost = 10,
            costGrowth = 1.35
        ),
        UpgradeDef(
            id = "credit_boost",
            name = "Instinct du marché",
            description = "+5% crédits/sec par niveau (permanent).",
            baseCost = 10,
            costGrowth = 1.35
        ),
        UpgradeDef(
            id = "start_bonus",
            name = "Sac de départ",
            description = "Départ avec +50 crédits par niveau (permanent).",
            baseCost = 15,
            costGrowth = 1.45
        )
    )

    // Boutique (tu peux ajouter d'autres objets plus tard)
    val housingItems: List<ShopItemDef> = listOf(
        ShopItemDef(
            id = "shelter",
            name = "Abri",
            description = "Un coin à toi. Pas grand-chose, mais tu es à couvert.",
            category = ShopCategory.HOUSING,
            costPerDay = 0.5,
            joy = 1
        ),
        ShopItemDef(
            id = "small_house",
            name = "Petite maison",
            description = "Un vrai toit, une vraie porte. Tu respires mieux.",
            category = ShopCategory.HOUSING,
            costPerDay = 5.0,
            joy = 3
        )
    )

    val foodItems: List<ShopItemDef> = listOf(
        ShopItemDef(
            id = "cooked_potato",
            name = "Pomme de terre cuite",
            description = "Simple, efficace. Ça cale.",
            category = ShopCategory.FOOD,
            costPerDay = 2.0,
            joy = 1
        ),
        ShopItemDef(
            id = "soup",
            name = "Soupe",
            description = "Chaud, nourrissant. Tu te sens presque chez toi.",
            category = ShopCategory.FOOD,
            costPerDay = 12.0,
            joy = 4
        )
    )

    // "Autre" : multi-achat. Laisse vide si tu veux le remplir plus tard.
    val otherItems: List<ShopItemDef> = emptyList()

    fun activity(id: ActivityId): ActivityDef =
        activities.firstOrNull { it.id == id } ?: activities.first()

    fun job(id: JobId): JobDef? =
        jobs.firstOrNull { it.id == id }

    fun upgrade(id: UpgradeId): UpgradeDef =
        upgrades.first { it.id == id }

    // ★ Déblocage séquentiel : +5 niveaux à chaque étape (5, 10, 15…)
    fun requiredPrevMasteryLevel(index: Int): Int = index * 5

    fun lastUnlockedActivityIndex(s: GameState): Int {
        var last = -1
        for (i in activities.indices) {
            val a = activities[i]
            val reqOk = a.required(s)
            val prevOk = if (i == 0) true else {
                val prevId = activities[i - 1].id
                val prevLvl = s.activityMastery[prevId]?.level ?: 1
                prevLvl >= requiredPrevMasteryLevel(i)
            }
            if (reqOk && prevOk && (i == 0 || last == i - 1)) last = i else break
        }
        return last
    }

    fun lastUnlockedJobIndex(s: GameState): Int {
        var last = -1
        for (i in jobs.indices) {
            val j = jobs[i]
            val reqOk = j.required(s)
            val prevOk = if (i == 0) true else {
                val prevId = jobs[i - 1].id
                val prevLvl = s.jobMastery[prevId]?.level ?: 1
                prevLvl >= requiredPrevMasteryLevel(i)
            }
            if (reqOk && prevOk && (i == 0 || last == i - 1)) last = i else break
        }
        return last
    }

    fun housing(id: HousingId): ShopItemDef? = housingItems.firstOrNull { it.id == id }
    fun food(id: FoodId): ShopItemDef? = foodItems.firstOrNull { it.id == id }
    fun other(id: OtherId): ShopItemDef? = otherItems.firstOrNull { it.id == id }

    fun dailyCost(s: GameState): Double {
        val housingCost = housing(s.selectedHousing)?.costPerDay ?: 0.0
        val foodCost = s.selectedFood?.let { food(it)?.costPerDay ?: 0.0 } ?: 0.0
        val otherCost = s.activeOther.sumOf { id -> other(id)?.costPerDay ?: 0.0 }
        return housingCost + foodCost + otherCost
    }

    fun joy(s: GameState): Int {
        val housingJoy = housing(s.selectedHousing)?.joy ?: 0
        val foodJoy = s.selectedFood?.let { food(it)?.joy ?: 0 } ?: 0
        val otherJoy = s.activeOther.sumOf { id -> other(id)?.joy ?: 0 }
        return housingJoy + foodJoy + otherJoy
    }

    // Affichage des prérequis (au lieu de juste vrai/faux)
    fun missingActivityRequirements(s: GameState, activityId: ActivityId): List<String> {
        fun lvl(id: SkillId) = s.skills[id]?.level ?: 1
        val missing = mutableListOf<String>()
        when (activityId) {
            "study_tech" -> {
                val cur = lvl("linguistics")
                if (cur < 3) missing += "Linguistique ≥ 3 (actuel : $cur)"
            }
            "socialize" -> {
                val cur = lvl("linguistics")
                if (cur < 2) missing += "Linguistique ≥ 2 (actuel : $cur)"
            }
        }
        return missing
    }

    fun missingJobRequirements(s: GameState, jobId: JobId): List<String> {
        fun lvl(id: SkillId) = s.skills[id]?.level ?: 1
        val missing = mutableListOf<String>()
        when (jobId) {
            "scrap_runner" -> {
                val cur = lvl("adaptation")
                if (cur < 2) missing += "Adaptation ≥ 2 (actuel : $cur)"
            }
            "translator_helper" -> {
                val cur = lvl("linguistics")
                if (cur < 6) missing += "Linguistique ≥ 6 (actuel : $cur)"
            }
            "tech_apprentice" -> {
                val tech = lvl("tech")
                val ad = lvl("adaptation")
                if (tech < 6) missing += "Tech ≥ 6 (actuel : $tech)"
                if (ad < 5) missing += "Adaptation ≥ 5 (actuel : $ad)"
            }
            "district_mediator" -> {
                val ch = lvl("charisma")
                val li = lvl("linguistics")
                val ad = lvl("adaptation")
                if (ch < 8) missing += "Charisme ≥ 8 (actuel : $ch)"
                if (li < 7) missing += "Linguistique ≥ 7 (actuel : $li)"
                if (ad < 7) missing += "Adaptation ≥ 7 (actuel : $ad)"
            }
        }
        return missing
    }
}

object Engine {

    private const val DAYS_PER_REAL_SECOND = 1.0

    private fun xpNeeded(level: Int): Double {
        val lv = max(1, level).toDouble()
        return 20.0 * lv.pow(1.55)
    }

    fun masteryXpNeeded(level: Int): Double {
        val lv = max(1, level).toDouble()
        return 15.0 * lv.pow(1.35)
    }

    private fun xpMultiplier(state: GameState): Double {
        val lvl = state.upgrades["xp_boost"] ?: 0
        val base = 1.0 + 0.05 * lvl
        // Joie : +1% XP par point de joie (réglage simple, modifiable)
        val joyMul = 1.0 + 0.01 * Defs.joy(state).coerceAtLeast(0)
        return base * joyMul
    }

    private fun creditMultiplier(state: GameState): Double {
        val lvl = state.upgrades["credit_boost"] ?: 0
        return 1.0 + 0.05 * lvl
    }

    fun currentDay(state: GameState): Int {
        // Jour 1 au démarrage
        return floor(state.lifeSeconds * DAYS_PER_REAL_SECOND).toInt() + 1
    }

    private fun lvl(s: GameState, id: SkillId): Int = s.skills[id]?.level ?: 1

    // Adaptation + Esprit => activités plus rapides
    fun activitySpeedMultiplier(s: GameState): Double {
        val ad = lvl(s, "adaptation")
        val mind = lvl(s, "mind")
        val mul = 1.0 + 0.03 * (ad - 1) + 0.02 * (mind - 1)
        return mul.coerceIn(1.0, 3.0)
    }

    // Linguistique => jobs plus rapides
    fun jobSpeedMultiplier(s: GameState): Double {
        val li = lvl(s, "linguistics")
        val mul = 1.0 + 0.03 * (li - 1)
        return mul.coerceIn(1.0, 3.0)
    }

    // Charisme => réduit le temps d'attente au démarrage d'un job
    fun jobStartDelaySeconds(s: GameState): Double {
        val ch = lvl(s, "charisma")
        val base = 20.0
        val factor = (1.0 - 0.03 * (ch - 1)).coerceIn(0.25, 1.0)
        return base * factor
    }

    fun tick(state: GameState, dtSecRaw: Double): GameState {
        val dtSec = max(0.0, min(dtSecRaw, 0.5))
        val dtDays = dtSec * DAYS_PER_REAL_SECOND

        var s = state.copy(
            lifeSeconds = state.lifeSeconds + dtSec,
            ageDays = state.ageDays + dtDays
        )

        // Décompte d'attente job (on gère un démarrage progressif)
        val waitBefore = s.jobWaitSecondsRemaining.coerceAtLeast(0.0)
        val waited = min(dtSec, waitBefore)
        val waitAfter = max(0.0, waitBefore - dtSec)
        s = s.copy(jobWaitSecondsRemaining = waitAfter)

        if (s.ageDays >= 80.0 * 365.0) {
            return reincarnate(s, "Ton corps n’a pas tenu. Mais quelque chose… persiste.")
        }

        val xpMul = xpMultiplier(s)
        val crMul = creditMultiplier(s)

        val actSpeed = activitySpeedMultiplier(s)
        val jobSpeed = jobSpeedMultiplier(s)

        // Activité : seulement si débloquée séquentiellement + required OK
        val act = Defs.activity(s.activeActivity)
        val actIdx = Defs.activities.indexOfFirst { it.id == act.id }
        val canDoActivity = actIdx != -1 && actIdx <= Defs.lastUnlockedActivityIndex(s) && act.required(s)

        if (canDoActivity) {
            val dtAct = dtSec * actSpeed
            s = s.copy(credits = s.credits + act.creditsPerSec * dtAct * crMul)
            act.xp.forEach { (skill, rate) ->
                s = gainXp(s, skill, rate * dtAct * xpMul)
            }
            s = gainActivityMastery(s, act.id, dtAct * 1.0 * xpMul)
        }

        // Job : seulement si débloqué séquentiellement + required OK
        val jobId = s.activeJob
        if (jobId != null) {
            val job = Defs.job(jobId)
            val jobIdx = Defs.jobs.indexOfFirst { it.id == jobId }
            val canDoJob = job != null && jobIdx != -1 && jobIdx <= Defs.lastUnlockedJobIndex(s) && job.required(s)

            if (canDoJob) {
                // Si on attend encore, pas de gains; sinon, on ne comptabilise que le temps restant après l'attente
                val effectiveJobTime = (dtSec - waited).coerceAtLeast(0.0)
                if (effectiveJobTime > 0.0) {
                    val dtJob = effectiveJobTime * jobSpeed
                    s = s.copy(credits = s.credits + job!!.creditsPerSec * dtJob * crMul)
                    job.xp.forEach { (skill, rate) ->
                        s = gainXp(s, skill, rate * dtJob * xpMul)
                    }
                    s = gainJobMastery(s, jobId, dtJob * 1.0 * xpMul)
                }
            }
        }

        // Coût journalier des objets (logement/nourriture/autre)
        val upkeep = Defs.dailyCost(s)
        if (upkeep > 0.0) {
            val newCredits = (s.credits - upkeep * dtDays).coerceAtLeast(0.0)
            s = s.copy(credits = newCredits)
        }

        s = storyCheck(s)
        return s
    }

    data class ReincarnationPreview(val echoGain: Int, val startCredits: Int)

    fun previewReincarnation(old: GameState): ReincarnationPreview {
        val adaptation = old.skills["adaptation"]?.level ?: 1
        val linguistics = old.skills["linguistics"]?.level ?: 1
        val echoGain = (old.credits / 250.0).toInt() + (adaptation / 2) + (linguistics / 3)
        val startBonus = (old.upgrades["start_bonus"] ?: 0) * 50
        return ReincarnationPreview(echoGain = max(0, echoGain), startCredits = startBonus)
    }

    private fun gainXp(state: GameState, skillId: SkillId, amount: Double): GameState {
        val cur = state.skills[skillId] ?: SkillState()
        var xp = cur.xp + max(0.0, amount)
        var lvl = cur.level

        while (xp >= xpNeeded(lvl)) {
            xp -= xpNeeded(lvl)
            lvl += 1
        }

        val newSkills = state.skills.toMutableMap()
        newSkills[skillId] = SkillState(level = lvl, xp = xp)
        return state.copy(skills = newSkills)
    }

    private fun gainActivityMastery(state: GameState, activityId: ActivityId, amount: Double): GameState {
        val cur = state.activityMastery[activityId] ?: SkillState()
        var xp = cur.xp + max(0.0, amount)
        var lvl = cur.level

        while (xp >= masteryXpNeeded(lvl)) {
            xp -= masteryXpNeeded(lvl)
            lvl += 1
        }

        val newMap = state.activityMastery.toMutableMap()
        newMap[activityId] = SkillState(level = lvl, xp = xp)
        return state.copy(activityMastery = newMap)
    }

    private fun gainJobMastery(state: GameState, jobId: JobId, amount: Double): GameState {
        val cur = state.jobMastery[jobId] ?: SkillState()
        var xp = cur.xp + max(0.0, amount)
        var lvl = cur.level

        while (xp >= masteryXpNeeded(lvl)) {
            xp -= masteryXpNeeded(lvl)
            lvl += 1
        }

        val newMap = state.jobMastery.toMutableMap()
        newMap[jobId] = SkillState(level = lvl, xp = xp)
        return state.copy(jobMastery = newMap)
    }

    private fun storyCheck(state: GameState): GameState {
        val ad = state.skills["adaptation"]?.level ?: 1
        val li = state.skills["linguistics"]?.level ?: 1

        fun addFlag(s0: GameState, flag: String, line: String): GameState {
            if (s0.storyFlags.contains(flag)) return s0
            val newLog = (s0.log + line).takeLast(60)
            return s0.copy(storyFlags = s0.storyFlags + flag, log = newLog)
        }

        var s = state
        if (ad >= 3) s = addFlag(s, "ad3", "Tu commences à reconnaître les routes sûres. La ville a un rythme.")
        if (li >= 3) s = addFlag(s, "li3", "Quelques mots deviennent clairs. Pas assez… mais tu peux demander de l'eau.")
        if (ad >= 6) s = addFlag(s, "ad6", "Tu comprends enfin : cette civilisation n'est pas “avancée”. Elle est… différente.")
        if (li >= 6) s = addFlag(s, "li6", "Tu saisis des phrases entières. Les gens parlent vite, mais tu ne te noies plus.")
        if (ad >= 10) s = addFlag(s, "ad10", "Tu arrêtes d’être un intrus. Tu deviens une pièce du puzzle.")
        return s
    }

    fun upgradeCost(def: UpgradeDef, currentLevel: Int): Int {
        return (def.baseCost * def.costGrowth.pow(currentLevel.toDouble()))
            .toInt()
            .coerceAtLeast(def.baseCost)
    }

    fun buyUpgrade(state: GameState, id: UpgradeId): GameState {
        val def = Defs.upgrade(id)
        val cur = state.upgrades[id] ?: 0
        val cost = upgradeCost(def, cur)
        if (state.echoes < cost) return state

        val newUp = state.upgrades.toMutableMap()
        newUp[id] = cur + 1

        return state.copy(
            echoes = state.echoes - cost,
            upgrades = newUp,
            log = (state.log + "Upgrade acheté : ${def.name} (niveau ${cur + 1}).").takeLast(60)
        )
    }

    fun manualReincarnate(state: GameState): GameState {
        return reincarnate(state, "Tu fermes les yeux… et tu choisis de recommencer.")
    }

    private fun reincarnate(old: GameState, reason: String): GameState {
        val prev = previewReincarnation(old)

        return GameState(
            credits = prev.startCredits.toDouble(),
            echoes = old.echoes + prev.echoGain,
            upgrades = old.upgrades,
            totalLives = old.totalLives + 1,
            log = listOf(
                reason,
                "Une nouvelle vie commence. Tes souvenirs sont flous… mais pas tes instincts.",
                "Échos gagnés : +${prev.echoGain}"
            )
        )
    }
}
