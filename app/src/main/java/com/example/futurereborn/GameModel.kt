package com.example.futurereborn

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

typealias SkillId = String
typealias ActivityId = String
typealias JobId = String
typealias UpgradeId = String

data class SkillState(val level: Int = 1, val xp: Double = 0.0)

data class GameState(
    val credits: Double = 0.0,
    // 16 ans au départ, exprimé en jours
    val ageDays: Double = 16.0 * 365.0,
    val lifeSeconds: Double = 0.0,

    val activeActivity: ActivityId = "explore",
    val activeJob: JobId? = null,

    val skills: Map<SkillId, SkillState> = mapOf(
        "strength" to SkillState(),
        "mind" to SkillState(),
        "charisma" to SkillState(),
        "tech" to SkillState(),
        "linguistics" to SkillState(),
        "adaptation" to SkillState()
    ),

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

    fun activity(id: ActivityId): ActivityDef =
        activities.firstOrNull { it.id == id } ?: activities.first()

    fun job(id: JobId): JobDef? =
        jobs.firstOrNull { it.id == id }

    fun upgrade(id: UpgradeId): UpgradeDef =
        upgrades.first { it.id == id }
}

object Engine {

    // 1 seconde réelle = 1 jour in-game (rapide, satisfaisant)
    private const val DAYS_PER_REAL_SECOND = 1.0

    private fun xpNeeded(level: Int): Double {
        val lv = max(1, level).toDouble()
        return 20.0 * lv.pow(1.55)
    }

    private fun xpMultiplier(state: GameState): Double {
        val lvl = state.upgrades["xp_boost"] ?: 0
        return 1.0 + 0.05 * lvl
    }

    private fun creditMultiplier(state: GameState): Double {
        val lvl = state.upgrades["credit_boost"] ?: 0
        return 1.0 + 0.05 * lvl
    }

    fun tick(state: GameState, dtSecRaw: Double): GameState {
        val dtSec = max(0.0, min(dtSecRaw, 0.5))
        val dtDays = dtSec * DAYS_PER_REAL_SECOND

        var s = state.copy(
            lifeSeconds = state.lifeSeconds + dtSec,
            ageDays = state.ageDays + dtDays
        )

        // Mort à 80 ans
        if (s.ageDays >= 80.0 * 365.0) {
            return reincarnate(s, "Ton corps n’a pas tenu. Mais quelque chose… persiste.")
        }

        val xpMul = xpMultiplier(s)
        val crMul = creditMultiplier(s)

        // Activité
        val act = Defs.activity(s.activeActivity)
        if (act.required(s)) {
            s = s.copy(credits = s.credits + act.creditsPerSec * dtSec * crMul)
            act.xp.forEach { (skill, rate) ->
                s = gainXp(s, skill, rate * dtSec * xpMul)
            }
        }

        // Job
        val jobId = s.activeJob
        if (jobId != null) {
            val job = Defs.job(jobId)
            if (job != null && job.required(s)) {
                s = s.copy(credits = s.credits + job.creditsPerSec * dtSec * crMul)
                job.xp.forEach { (skill, rate) ->
                    s = gainXp(s, skill, rate * dtSec * xpMul)
                }
            }
        }

        // Story milestones
        s = storyCheck(s)

        return s
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

    private fun storyCheck(state: GameState): GameState {
        val ad = state.skills["adaptation"]?.level ?: 1
        val li = state.skills["linguistics"]?.level ?: 1

        fun addFlag(flag: String, line: String): GameState {
            if (state.storyFlags.contains(flag)) return state
            val newLog = (state.log + line).takeLast(60)
            return state.copy(storyFlags = state.storyFlags + flag, log = newLog)
        }

        var s = state
        if (ad >= 3) s = addFlag("ad3", "Tu commences à reconnaître les routes sûres. La ville a un rythme.")
        if (li >= 3) s = addFlag("li3", "Quelques mots deviennent clairs. Pas assez… mais tu peux demander de l'eau.")
        if (ad >= 6) s = addFlag("ad6", "Tu comprends enfin : cette civilisation n'est pas “avancée”. Elle est… différente.")
        if (li >= 6) s = addFlag("li6", "Tu saisis des phrases entières. Les gens parlent vite, mais tu ne te noies plus.")
        if (ad >= 10) s = addFlag("ad10", "Tu arrêtes d’être un intrus. Tu deviens une pièce du puzzle.")
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

package com.example.futurereborn

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

typealias SkillId = String
typealias ActivityId = String
typealias JobId = String
typealias UpgradeId = String

data class SkillState(val level: Int = 1, val xp: Double = 0.0)

data class GameState(
    val credits: Double = 0.0,
    // 16 ans au départ, exprimé en jours
    val ageDays: Double = 16.0 * 365.0,
    val lifeSeconds: Double = 0.0,

    val activeActivity: ActivityId = "explore",
    val activeJob: JobId? = null,

    val skills: Map<SkillId, SkillState> = mapOf(
        "strength" to SkillState(),
        "mind" to SkillState(),
        "charisma" to SkillState(),
        "tech" to SkillState(),
        "linguistics" to SkillState(),
        "adaptation" to SkillState()
    ),

    // ★ NOUVEAU : niveau/XP de maîtrise par activité et par job
    // (sert au déblocage séquentiel + affiche des barres en UI)
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

    fun activity(id: ActivityId): ActivityDef =
        activities.firstOrNull { it.id == id } ?: activities.first()

    fun job(id: JobId): JobDef? =
        jobs.firstOrNull { it.id == id }

    fun upgrade(id: UpgradeId): UpgradeDef =
        upgrades.first { it.id == id }

    // ★ NOUVEAU : règle “+5 niveaux avant le suivant”
    // index=1 => 5, index=2 => 10, index=3 => 15, ...
    fun requiredPrevMasteryLevel(index: Int): Int = index * 5

    // ★ NOUVEAU : dernier index d'activité débloqué séquentiellement
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
            if (reqOk && prevOk && (i == 0 || last == i - 1)) {
                last = i
            } else {
                break
            }
        }
        return last
    }

    // ★ NOUVEAU : dernier index de job débloqué séquentiellement
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
            if (reqOk && prevOk && (i == 0 || last == i - 1)) {
                last = i
            } else {
                break
            }
        }
        return last
    }

    fun isActivityUnlockedSequential(s: GameState, id: ActivityId): Boolean {
        val idx = activities.indexOfFirst { it.id == id }
        if (idx == -1) return false
        return idx <= lastUnlockedActivityIndex(s)
    }

    fun isJobUnlockedSequential(s: GameState, id: JobId): Boolean {
        val idx = jobs.indexOfFirst { it.id == id }
        if (idx == -1) return false
        return idx <= lastUnlockedJobIndex(s)
    }
}

object Engine {

    // 1 seconde réelle = 1 jour in-game (rapide, satisfaisant)
    private const val DAYS_PER_REAL_SECOND = 1.0

    private fun xpNeeded(level: Int): Double {
        val lv = max(1, level).toDouble()
        return 20.0 * lv.pow(1.55)
    }

    // ★ NOUVEAU : XP nécessaire pour monter la “maîtrise” d’un job/activité
    fun masteryXpNeeded(level: Int): Double {
        val lv = max(1, level).toDouble()
        return 15.0 * lv.pow(1.35)
    }

    private fun xpMultiplier(state: GameState): Double {
        val lvl = state.upgrades["xp_boost"] ?: 0
        return 1.0 + 0.05 * lvl
    }

    private fun creditMultiplier(state: GameState): Double {
        val lvl = state.upgrades["credit_boost"] ?: 0
        return 1.0 + 0.05 * lvl
    }

    fun tick(state: GameState, dtSecRaw: Double): GameState {
        val dtSec = max(0.0, min(dtSecRaw, 0.5))
        val dtDays = dtSec * DAYS_PER_REAL_SECOND

        var s = state.copy(
            lifeSeconds = state.lifeSeconds + dtSec,
            ageDays = state.ageDays + dtDays
        )

        // Mort à 80 ans
        if (s.ageDays >= 80.0 * 365.0) {
            return reincarnate(s, "Ton corps n’a pas tenu. Mais quelque chose… persiste.")
        }

        val xpMul = xpMultiplier(s)
        val crMul = creditMultiplier(s)

        // Activité (bloquée aussi par le déblocage séquentiel)
        val act = Defs.activity(s.activeActivity)
        val canDoActivity = Defs.isActivityUnlockedSequential(s, act.id) && act.required(s)
        if (canDoActivity) {
            s = s.copy(credits = s.credits + act.creditsPerSec * dtSec * crMul)
            act.xp.forEach { (skill, rate) ->
                s = gainXp(s, skill, rate * dtSec * xpMul)
            }
            // ★ gain maîtrise activité
            s = gainActivityMastery(s, act.id, dtSec * 1.0 * xpMul)
        }

        // Job (bloqué aussi par le déblocage séquentiel)
        val jobId = s.activeJob
        if (jobId != null) {
            val job = Defs.job(jobId)
            val canDoJob = job != null && Defs.isJobUnlockedSequential(s, jobId) && job.required(s)
            if (canDoJob) {
                s = s.copy(credits = s.credits + job!!.creditsPerSec * dtSec * crMul)
                job.xp.forEach { (skill, rate) ->
                    s = gainXp(s, skill, rate * dtSec * xpMul)
                }
                // ★ gain maîtrise job
                s = gainJobMastery(s, jobId, dtSec * 1.0 * xpMul)
            }
        }

        // Story milestones
        s = storyCheck(s)

        return s
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

    // ★ NOUVEAU : gain de maîtrise (activité)
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

    // ★ NOUVEAU : gain de maîtrise (job)
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

        fun addFlag(flag: String, line: String): GameState {
            if (state.storyFlags.contains(flag)) return state
            val newLog = (state.log + line).takeLast(60)
            return state.copy(storyFlags = state.storyFlags + flag, log = newLog)
        }

        var s = state
        if (ad >= 3) s = addFlag("ad3", "Tu commences à reconnaître les routes sûres. La ville a un rythme.")
        if (li >= 3) s = addFlag("li3", "Quelques mots deviennent clairs. Pas assez… mais tu peux demander de l'eau.")
        if (ad >= 6) s = addFlag("ad6", "Tu comprends enfin : cette civilisation n'est pas “avancée”. Elle est… différente.")
        if (li >= 6) s = addFlag("li6", "Tu saisis des phrases entières. Les gens parlent vite, mais tu ne te noies plus.")
        if (ad >= 10) s = addFlag("ad10", "Tu arrêtes d’être un intrus. Tu deviens une pièce du puzzle.")
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
        val adaptation = old.skills["adaptation"]?.level ?: 1
        val linguistics = old.skills["linguistics"]?.level ?: 1

        val echoGain = (old.credits / 250.0).toInt() + (adaptation / 2) + (linguistics / 3)
        val startBonus = (old.upgrades["start_bonus"] ?: 0) * 50

        return GameState(
            credits = startBonus.toDouble(),
            echoes = old.echoes + max(0, echoGain),
            upgrades = old.upgrades,
            totalLives = old.totalLives + 1,
            log = listOf(
                reason,
                "Une nouvelle vie commence. Tes souvenirs sont flous… mais pas tes instincts.",
                "Échos gagnés : +${max(0, echoGain)}"
            )
        )
    }
}
      totalLives = old.totalLives + 1,
            log = listOf(
                reason,
                "Une nouvelle vie commence. Tes souvenirs sont flous… mais pas tes instincts.",
                "Échos gagnés : +${max(0, echoGain)}"
            )
        )
    }
}
