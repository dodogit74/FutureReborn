package com.example.futurereborn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.floor
import kotlin.math.pow

class MainActivity : ComponentActivity() {

    private val vm: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {

                var tab by remember { mutableIntStateOf(0) }
                val s = vm.state

                // Popup récit
                var lastLogSize by remember { mutableIntStateOf(s.log.size) }
                var showStoryPopup by remember { mutableStateOf(false) }
                var latestStoryLine by remember { mutableStateOf("") }

                LaunchedEffect(s.log.size) {
                    if (s.log.size > lastLogSize) {
                        latestStoryLine = s.log.last()
                        showStoryPopup = true
                        lastLogSize = s.log.size
                    }
                }

                if (showStoryPopup) {
                    AlertDialog(
                        onDismissRequest = { showStoryPopup = false },
                        confirmButton = {
                            TextButton(onClick = { showStoryPopup = false }) {
                                Text("Continuer")
                            }
                        },
                        title = { Text("Nouvel événement") },
                        text = { Text(latestStoryLine) }
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Future Reborn", style = MaterialTheme.typography.titleLarge)
                            Text("Vies : ${s.totalLives}  |  Échos : ${s.echoes}")
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Card {
                        Column(Modifier.padding(12.dp)) {
                            Text("Âge : ${formatAge(s.ageDays)}")
                            Text("Crédits : ${s.credits.toInt()}")
                            Text(
                                "Activité : ${Defs.activity(s.activeActivity).name}" +
                                    (if (s.activeJob != null) " | Job : ${Defs.job(s.activeJob!!)?.name}" else ""),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    TabRow(selectedTabIndex = tab) {
                        Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Action") })
                        Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Upgrades") })
                        Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Journal") })
                    }

                    Spacer(Modifier.height(12.dp))

                    when (tab) {
                        0 -> ActionTab(
                            s = s,
                            onActivity = vm::setActivity,
                            onJob = vm::setJob,
                            onReincarnate = vm::reincarnateNow
                        )
                        1 -> UpgradeTab(s = s, onBuy = vm::buyUpgrade)
                        2 -> LogTab(s = s)
                    }
                }
            }
        }
    }
}

@Composable
fun ActionTab(
    s: GameState,
    onActivity: (ActivityId) -> Unit,
    onJob: (JobId?) -> Unit,
    onReincarnate: () -> Unit
) {
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier.verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Card {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Compétences", style = MaterialTheme.typography.titleSmall)
                SkillLine("Adaptation", s.skills["adaptation"])
                SkillLine("Linguistique", s.skills["linguistics"])
                SkillLine("Tech", s.skills["tech"])
                SkillLine("Esprit", s.skills["mind"])
                SkillLine("Force", s.skills["strength"])
                SkillLine("Charisme", s.skills["charisma"])
            }
        }

        // Activités (séquentiel +5 niveaux de maîtrise)
        Card {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Activités", style = MaterialTheme.typography.titleSmall)

                val acts = Defs.activities
                val lastUnlocked = Defs.lastUnlockedActivityIndex(s)
                val visibleCount = (lastUnlocked + 2).coerceIn(1, acts.size)
                val visible = acts.take(visibleCount)

                visible.forEachIndexed { index, a ->
                    val unlocked = index <= lastUnlocked

                    ElevatedButton(
                        onClick = { onActivity(a.id) },
                        enabled = unlocked,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            when {
                                s.activeActivity == a.id -> "✓ ${a.name}"
                                unlocked -> a.name
                                else -> "🔒 ${a.name}"
                            }
                        )
                    }

                    MasteryLine("Maîtrise activité", s.activityMastery[a.id] ?: SkillState())
                    Text(a.description, style = MaterialTheme.typography.bodySmall)

                    if (!unlocked) {
                        val prev = acts.getOrNull(index - 1)
                        if (prev != null) {
                            val need = Defs.requiredPrevMasteryLevel(index)
                            val prevLvl = s.activityMastery[prev.id]?.level ?: 1
                            Text(
                                "Condition : ${prev.name} maîtrise niveau $need (actuel : $prevLvl)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        if (!a.required(s)) {
                            Text(
                                "Prérequis : compétences insuffisantes.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }

        // Jobs (séquentiel +5 niveaux de maîtrise)
        Card {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Jobs", style = MaterialTheme.typography.titleSmall)

                OutlinedButton(onClick = { onJob(null) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Quitter le job")
                }

                val jobs = Defs.jobs
                val lastUnlocked = Defs.lastUnlockedJobIndex(s)
                val visibleCount = (lastUnlocked + 2).coerceIn(1, jobs.size)
                val visible = jobs.take(visibleCount)

                visible.forEachIndexed { index, j ->
                    val unlocked = index <= lastUnlocked

                    ElevatedButton(
                        onClick = { onJob(j.id) },
                        enabled = unlocked,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            when {
                                s.activeJob == j.id -> "✓ ${j.name}"
                                unlocked -> j.name
                                else -> "🔒 ${j.name}"
                            }
                        )
                    }

                    MasteryLine("Maîtrise job", s.jobMastery[j.id] ?: SkillState())
                    Text("${j.description}\n+${j.creditsPerSec}/s", style = MaterialTheme.typography.bodySmall)

                    if (!unlocked) {
                        val prev = jobs.getOrNull(index - 1)
                        if (prev != null) {
                            val need = Defs.requiredPrevMasteryLevel(index)
                            val prevLvl = s.jobMastery[prev.id]?.level ?: 1
                            Text(
                                "Condition : ${prev.name} maîtrise niveau $need (actuel : $prevLvl)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        if (!j.required(s)) {
                            Text(
                                "Prérequis : compétences insuffisantes.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }

        Button(onClick = onReincarnate, modifier = Modifier.fillMaxWidth()) {
            Text("Réincarner maintenant")
        }
    }
}

@Composable
fun UpgradeTab(s: GameState, onBuy: (UpgradeId) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Upgrades permanents (Échos)", style = MaterialTheme.typography.titleSmall)
                Text("Échos disponibles : ${s.echoes}")

                Defs.upgrades.forEach { u ->
                    val lvl = s.upgrades[u.id] ?: 0
                    val cost = Engine.upgradeCost(u, lvl)

                    Card {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("${u.name} (niveau $lvl)", style = MaterialTheme.typography.titleMedium)
                            Text(u.description, style = MaterialTheme.typography.bodySmall)
                            Button(onClick = { onBuy(u.id) }, enabled = s.echoes >= cost) {
                                Text("Acheter : $cost")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LogTab(s: GameState) {
    val scroll = rememberScrollState()
    Card {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .verticalScroll(scroll),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Journal", style = MaterialTheme.typography.titleSmall)
            s.log.forEach { line -> Text("• $line") }
        }
    }
}

@Composable
fun SkillLine(label: String, st: SkillState?) {
    val level = st?.level ?: 1
    val xp = st?.xp ?: 0.0
    val xpNeeded = 20.0 * level.toDouble().pow(1.55)
    val progress = (xp / xpNeeded).toFloat().coerceIn(0f, 1f)

    Column {
        Text("$label : niveau $level → ${level + 1}")
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxWidth().height(8.dp)
        )
    }
}

@Composable
fun MasteryLine(label: String, st: SkillState) {
    val level = st.level
    val xp = st.xp
    val xpNeeded = Engine.masteryXpNeeded(level)
    val progress = (xp / xpNeeded).toFloat().coerceIn(0f, 1f)

    Column {
        Text("$label : niveau $level → ${level + 1}", style = MaterialTheme.typography.bodySmall)
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxWidth().height(6.dp)
        )
    }
}

fun formatAge(ageDays: Double): String {
    val years = floor(ageDays / 365.0).toInt()
    val months = floor(((ageDays % 365.0) / 30.0)).toInt()
    return "${years}a ${months}m"
}
