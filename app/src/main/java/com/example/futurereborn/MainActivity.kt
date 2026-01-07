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
import java.util.Locale

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

                // Réincarnation : aperçu + confirmation
                val reincPreview = Engine.previewReincarnation(s)
                var showReincarnateDialog by remember { mutableStateOf(false) }

                if (showReincarnateDialog) {
                    AlertDialog(
                        onDismissRequest = { showReincarnateDialog = false },
                        confirmButton = {
                            TextButton(onClick = {
                                showReincarnateDialog = false
                                vm.reincarnateNow()
                            }) { Text("Confirmer") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showReincarnateDialog = false }) { Text("Annuler") }
                        },
                        title = { Text("Réincarner ?") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Échos à gagner : +${reincPreview.echoGain}")
                                Text("Crédits de départ (sac de départ) : ${reincPreview.startCredits}")
                                Text("Tu recommenceras à 16 ans.", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {

                    val day = Engine.currentDay(s)
                    val joy = Defs.joy(s)
                    val dailyCost = Defs.dailyCost(s)

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Future Reborn", style = MaterialTheme.typography.titleLarge)
                            Text("Jour $day  |  Vies : ${s.totalLives}  |  Échos : ${s.echoes}")
                            Text("Joie : $joy  |  Coût/jour : ${pretty1(dailyCost)}",
                                style = MaterialTheme.typography.bodySmall)
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
                        Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Boutique") })
                        Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Upgrades") })
                        Tab(selected = tab == 3, onClick = { tab = 3 }, text = { Text("Journal") })
                    }

                    Spacer(Modifier.height(12.dp))

                    when (tab) {
                        0 -> ActionTab(
                            s = s,
                            onActivity = vm::setActivity,
                            onJob = vm::setJob,
                            reincPreview = reincPreview,
                            onRequestReincarnate = { showReincarnateDialog = true }
                        )
                        1 -> ShopTab(
                            s = s,
                            onSelectHousing = vm::selectHousing,
                            onSelectFood = vm::selectFood,
                            onBuyOther = vm::buyOther,
                            onToggleOtherActive = vm::toggleOtherActive
                        )
                        2 -> UpgradeTab(s = s, onBuy = vm::buyUpgrade)
                        3 -> LogTab(s = s)
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
    reincPreview: Engine.ReincarnationPreview,
    onRequestReincarnate: () -> Unit
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

                Divider()
                Text(
                    "Vitesse activités : x${pretty2(Engine.activitySpeedMultiplier(s))} (Adaptation + Esprit)",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Vitesse jobs : x${pretty2(Engine.jobSpeedMultiplier(s))} (Linguistique)",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Attente au démarrage d'un job : ${pretty0(Engine.jobStartDelaySeconds(s))}s (réduit par Charisme)",
                    style = MaterialTheme.typography.bodySmall
                )
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
                            val missing = Defs.missingActivityRequirements(s, a.id)
                            if (missing.isNotEmpty()) {
                                missing.forEach { req ->
                                    Text(
                                        "Prérequis : $req",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            } else {
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
                            val missing = Defs.missingJobRequirements(s, j.id)
                            if (missing.isNotEmpty()) {
                                missing.forEach { req ->
                                    Text(
                                        "Prérequis : $req",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            } else {
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
        }

        Button(onClick = onRequestReincarnate, modifier = Modifier.fillMaxWidth()) {
            Text("Réincarner (+${reincPreview.echoGain} échos)")
        }
    }
}

@Composable
fun ShopTab(
    s: GameState,
    onSelectHousing: (HousingId) -> Unit,
    onSelectFood: (FoodId?) -> Unit,
    onBuyOther: (OtherId) -> Unit,
    onToggleOtherActive: (OtherId) -> Unit
) {
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier.verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Card {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Boutique", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Joie : ${Defs.joy(s)}  |  Coût/jour total : ${pretty1(Defs.dailyCost(s))}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "La joie multiplie les gains d'XP (+1% par point).",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Logement (un seul choix)
        Card {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Logement", style = MaterialTheme.typography.titleSmall)

                Defs.housingItems.forEach { item ->
                    val selected = s.selectedHousing == item.id
                    ElevatedButton(
                        onClick = { onSelectHousing(item.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text((if (selected) "✓ " else "") + item.name)
                    }
                    Text(
                        "Coût/jour : ${pretty1(item.costPerDay)}  |  Joie +${item.joy}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(item.description, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // Nourriture (un seul choix)
        Card {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Nourriture", style = MaterialTheme.typography.titleSmall)

                OutlinedButton(
                    onClick = { onSelectFood(null) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (s.selectedFood == null) "✓ Aucun" else "Aucun")
                }

                Defs.foodItems.forEach { item ->
                    val selected = s.selectedFood == item.id
                    ElevatedButton(
                        onClick = { onSelectFood(item.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text((if (selected) "✓ " else "") + item.name)
                    }
                    Text(
                        "Coût/jour : ${pretty1(item.costPerDay)}  |  Joie +${item.joy}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(item.description, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // Autre (multi-achat)
        Card {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Autre", style = MaterialTheme.typography.titleSmall)

                if (Defs.otherItems.isEmpty()) {
                    Text(
                        "Aucun objet ici pour le moment. Ajoute tes objets dans GameModel.kt -> Defs.otherItems.",
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    Defs.otherItems.forEach { item ->
                        val owned = s.ownedOther.contains(item.id)
                        val active = s.activeOther.contains(item.id)

                        Card {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(item.name, style = MaterialTheme.typography.titleMedium)
                                Text(item.description, style = MaterialTheme.typography.bodySmall)
                                Text(
                                    "Coût/jour : ${pretty1(item.costPerDay)}  |  Joie +${item.joy}",
                                    style = MaterialTheme.typography.bodySmall
                                )

                                if (!owned) {
                                    Button(onClick = { onBuyOther(item.id) }) { Text("Acheter") }
                                } else {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Actif", modifier = Modifier.weight(1f))
                                        Switch(
                                            checked = active,
                                            onCheckedChange = { onToggleOtherActive(item.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
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

fun pretty1(v: Double): String = String.format(Locale.US, "%.1f", v)
fun pretty2(v: Double): String = String.format(Locale.US, "%.2f", v)
fun pretty0(v: Double): String = String.format(Locale.US, "%.0f", v)
