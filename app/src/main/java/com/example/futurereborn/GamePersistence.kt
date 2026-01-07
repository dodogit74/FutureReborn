package com.example.futurereborn

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "game_store")
private val KEY_STATE = stringPreferencesKey("state")

object Persistence {

    fun flow(context: Context): Flow<GameState?> {
        return context.dataStore.data.map { prefs ->
            val raw = prefs[KEY_STATE] ?: return@map null
            runCatching { Codec.decode(raw) }.getOrNull()
        }
    }

    suspend fun save(context: Context, state: GameState) {
        val raw = Codec.encode(state)
        context.dataStore.edit { prefs -> prefs[KEY_STATE] = raw }
    }
}

object Codec {
    // Format compact maison :
    // credits|ageDays|lifeSeconds|act|job|null|echoes|lives|flags;...|upgrades(id,lv;...)|skills(id,lv,xp;...)|log(line;;line...)
    // |activityMastery(id,lv,xp;...)|jobMastery(id,lv,xp;...)|housing|food|null|ownedOther(;...)|activeOther(;...)|jobWait
    fun encode(s: GameState): String {
        val job = s.activeJob ?: "null"
        val flags = s.storyFlags.joinToString(";")
        val upgrades = s.upgrades.entries.joinToString(";") { "${it.key},${it.value}" }
        val skills = s.skills.entries.joinToString(";") { (id, st) -> "${id},${st.level},${st.xp}" }
        val actMastery = s.activityMastery.entries.joinToString(";") { (id, st) -> "${id},${st.level},${st.xp}" }
        val jobMastery = s.jobMastery.entries.joinToString(";") { (id, st) -> "${id},${st.level},${st.xp}" }

        val housing = s.selectedHousing
        val food = s.selectedFood ?: "null"
        val ownedOther = s.ownedOther.joinToString(";")
        val activeOther = s.activeOther.joinToString(";")

        val log = s.log.joinToString(";;") { it.replace("|", "/") }
        return listOf(
            s.credits, s.ageDays, s.lifeSeconds, s.activeActivity, job,
            s.echoes, s.totalLives, flags, upgrades, skills, log,
            actMastery, jobMastery,
            housing, food, ownedOther, activeOther, s.jobWaitSecondsRemaining
        ).joinToString("|")
    }

    fun decode(raw: String): GameState {
        val p = raw.split("|")
        require(p.size >= 11)

        val credits = p[0].toDouble()
        val ageDays = p[1].toDouble()
        val lifeSeconds = p[2].toDouble()
        val act = p[3]
        val job = p[4].let { if (it == "null") null else it }
        val echoes = p[5].toInt()
        val lives = p[6].toInt()

        val flags = if (p[7].isBlank()) emptySet() else p[7].split(";").toSet()

        val upgrades = mutableMapOf<UpgradeId, Int>()
        if (p[8].isNotBlank()) {
            p[8].split(";").forEach {
                val a = it.split(",")
                if (a.size == 2) upgrades[a[0]] = a[1].toInt()
            }
        }

        val skills = mutableMapOf<SkillId, SkillState>()
        if (p[9].isNotBlank()) {
            p[9].split(";").forEach {
                val a = it.split(",")
                if (a.size == 3) skills[a[0]] = SkillState(a[1].toInt(), a[2].toDouble())
            }
        }

        val log = if (p[10].isBlank()) emptyList() else p[10].split(";;")

        fun parseMastery(idx: Int): Map<String, SkillState> {
            if (p.size <= idx) return emptyMap()
            val rawM = p[idx]
            if (rawM.isBlank()) return emptyMap()
            val map = mutableMapOf<String, SkillState>()
            rawM.split(";").forEach {
                val a = it.split(",")
                if (a.size == 3) {
                    val id = a[0]
                    val lv = a[1].toIntOrNull() ?: 1
                    val xp = a[2].toDoubleOrNull() ?: 0.0
                    map[id] = SkillState(lv, xp)
                }
            }
            return map
        }

        val activityMastery = parseMastery(11)
        val jobMastery = parseMastery(12)

        val housing = if (p.size > 13 && p[13].isNotBlank()) p[13] else GameState().selectedHousing
        val food = if (p.size > 14) p[14].let { if (it == "null" || it.isBlank()) null else it } else null
        val ownedOther = if (p.size > 15 && p[15].isNotBlank()) p[15].split(";").toSet() else emptySet()
        val activeOther = if (p.size > 16 && p[16].isNotBlank()) p[16].split(";").toSet() else emptySet()
        val jobWait = if (p.size > 17) p[17].toDoubleOrNull() ?: 0.0 else 0.0

        val base = GameState()
        val mergedUp = base.upgrades.toMutableMap().also { it.putAll(upgrades) }
        val mergedSkills = base.skills.toMutableMap().also { it.putAll(skills) }

        return GameState(
            credits = credits,
            ageDays = ageDays,
            lifeSeconds = lifeSeconds,
            activeActivity = act,
            activeJob = job,
            jobWaitSecondsRemaining = jobWait,
            selectedHousing = housing,
            selectedFood = food,
            ownedOther = ownedOther,
            activeOther = activeOther,
            echoes = echoes,
            totalLives = lives,
            storyFlags = flags,
            upgrades = mergedUp,
            skills = mergedSkills,
            activityMastery = activityMastery,
            jobMastery = jobMastery,
            log = if (log.isEmpty()) base.log else log
        )
    }
}
