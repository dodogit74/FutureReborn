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
    fun encode(s: GameState): String {
        val job = s.activeJob ?: "null"
        val flags = s.storyFlags.joinToString(";")
        val upgrades = s.upgrades.entries.joinToString(";") { "${it.key},${it.value}" }
        val skills = s.skills.entries.joinToString(";") { (id, st) -> "${id},${st.level},${st.xp}" }
        val log = s.log.joinToString(";;") { it.replace("|", "/") }
        return listOf(
            s.credits, s.ageDays, s.lifeSeconds, s.activeActivity, job,
            s.echoes, s.totalLives, flags, upgrades, skills, log
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

        val base = GameState()
        val mergedUp = base.upgrades.toMutableMap().also { it.putAll(upgrades) }
        val mergedSkills = base.skills.toMutableMap().also { it.putAll(skills) }

        return GameState(
            credits = credits,
            ageDays = ageDays,
            lifeSeconds = lifeSeconds,
            activeActivity = act,
            activeJob = job,
            echoes = echoes,
            totalLives = lives,
            storyFlags = flags,
            upgrades = mergedUp,
            skills = mergedSkills,
            log = if (log.isEmpty()) base.log else log
        )
    }
}
