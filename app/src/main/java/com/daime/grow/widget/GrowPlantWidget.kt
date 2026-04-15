package com.daime.grow.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.unit.ColorProvider as GlanceColorProvider
import com.daime.grow.GrowApplication
import com.daime.grow.MainActivity
import com.daime.grow.R
import com.daime.grow.data.local.entity.PlantEventEntity
import com.daime.grow.data.local.entity.HarvestBatchEntity
import com.daime.grow.data.local.entity.PlantEntity
import com.daime.grow.data.reminder.ReminderScheduler
import com.daime.grow.domain.model.Plant
import com.daime.grow.domain.model.calculateCultivationDays
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val widgetShell = ColorProvider(day = Color(0xAAF6F8F2), night = Color(0xCC151B16))
private val widgetGlass = ColorProvider(day = Color(0xCCFFFFFF), night = Color(0x444B564A))
private val widgetPanel = ColorProvider(day = Color(0xB8F2F7ED), night = Color(0x66333D34))
private val widgetPrimary = ColorProvider(day = Color(0xFF1F6D3C), night = Color(0xFF8BD5A2))
private val widgetDrying = ColorProvider(day = Color(0xFFBE7433), night = Color(0xFFF0B06D))
private val widgetCuring = ColorProvider(day = Color(0xFF7A57B0), night = Color(0xFFD3B9FF))
private val widgetTextPrimary = ColorProvider(day = Color(0xFF132017), night = Color(0xFFF3F7F2))
private val widgetTextSecondary = ColorProvider(day = Color(0xFF556456), night = Color(0xFFC1CCC1))
private val widgetBadge = ColorProvider(day = Color(0xFF6D8041), night = Color(0xFFC8D97F))
private val widgetPageKey = intPreferencesKey("widget_page")
private val widgetFeedbackKey = androidx.datastore.preferences.core.stringPreferencesKey("widget_feedback")
private const val widgetPageCount = 3
private val plantIdParam = ActionParameters.Key<Long>("plant_id")
private val batchIdParam = ActionParameters.Key<Long>("batch_id")

data class HarvestWidgetSnapshot(
    val count: Int,
    val id: Long? = null,
    val plantName: String? = null,
    val harvestDate: Long? = null,
    val nextBurpDate: Long? = null,
    val lastBurpDate: Long? = null
)

data class GrowWidgetSnapshot(
    val totalPlants: Int,
    val plantId: Long? = null,
    val plantName: String? = null,
    val stage: String? = null,
    val days: Int? = null,
    val nextWateringDate: Long? = null,
    val feedback: String? = null,
    val drying: HarvestWidgetSnapshot = HarvestWidgetSnapshot(0),
    val curing: HarvestWidgetSnapshot = HarvestWidgetSnapshot(0)
)

class GrowPlantWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = GrowWidgetRepository.loadSnapshot(context)
        val openAppAction = actionStartActivity(
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        )

        provideContent {
            val preferences = currentState<Preferences>()
            GrowPlantWidgetContent(
                context = context,
                snapshot = snapshot.copy(feedback = preferences[widgetFeedbackKey]),
                openAppAction = openAppAction,
                currentPage = preferences[widgetPageKey] ?: 0
            )
        }
    }
}

class GrowPlantWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GrowPlantWidget()
}

object GrowWidgetUpdater {
    suspend fun refreshAll(context: Context) {
        GrowPlantWidget().updateAll(context)
    }
}

class PreviousWidgetPageAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[widgetPageKey] = ((prefs[widgetPageKey] ?: 0) - 1).floorMod(widgetPageCount)
            prefs[widgetFeedbackKey] = ""
        }
        GrowPlantWidget().updateAll(context)
    }
}

class NextWidgetPageAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[widgetPageKey] = ((prefs[widgetPageKey] ?: 0) + 1) % widgetPageCount
            prefs[widgetFeedbackKey] = ""
        }
        GrowPlantWidget().updateAll(context)
    }
}

class QuickWaterAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val plantId = parameters[plantIdParam] ?: return
        val appContainer = (context.applicationContext as GrowApplication).appContainer
        val plantDao = appContainer.database.plantDao()
        val plantEventDao = appContainer.database.plantEventDao()
        val scheduler = ReminderScheduler(context)
        val plant = plantDao.getPlantById(plantId) ?: return
        val now = System.currentTimeMillis()
        val nextDate = now + 24L * 60L * 60L * 1_000L

        plantDao.updateNextWateringDate(plantId, nextDate)
        plantEventDao.insert(
            PlantEventEntity(
                plantId = plantId,
                type = "Rega",
                note = "Rega rápida pelo widget",
                createdAt = now
            )
        )
        scheduler.scheduleForPlant(
            Plant(
                id = plant.id,
                name = plant.name,
                strain = plant.strain,
                stage = plant.stage,
                medium = plant.medium,
                days = plant.days,
                photoUri = plant.photoUri,
                nextWateringDate = nextDate,
                createdAt = plant.createdAt,
                sharedOnMural = plant.sharedOnMural,
                isHydroponic = plant.isHydroponic
            )
        )
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[widgetFeedbackKey] = "Rega registrada agora"
        }
        GrowPlantWidget().updateAll(context)
    }
}

class BurpBatchAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val batchId = parameters[batchIdParam] ?: return
        val appContainer = (context.applicationContext as GrowApplication).appContainer
        val harvestDao = appContainer.database.harvestDao()
        val scheduler = ReminderScheduler(context)
        val batch = harvestDao.getBatchById(batchId) ?: return
        val now = System.currentTimeMillis()
        val daysSinceHarvest = ((now - batch.harvestDate) / (1000 * 60 * 60 * 24)).toInt()
        val nextIntervalHours = when (batch.status) {
            "DRYING" -> when {
                daysSinceHarvest < 3 -> 12
                daysSinceHarvest < 7 -> 24
                else -> 48
            }
            "CURING" -> when {
                daysSinceHarvest < 14 -> 24
                daysSinceHarvest < 28 -> 48
                daysSinceHarvest < 42 -> 72
                else -> 168
            }
            else -> 24
        }

        harvestDao.updateBatch(
            batch.copy(
                lastBurpDate = now,
                nextBurpDate = now + nextIntervalHours * 60L * 60L * 1_000L
            )
        )
        scheduler.scheduleBurpReminder(batchId, nextIntervalHours.toLong())
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[widgetFeedbackKey] = "Respiro atualizado"
        }
        GrowPlantWidget().updateAll(context)
    }
}

class StartCuringAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val batchId = parameters[batchIdParam] ?: return
        val appContainer = (context.applicationContext as GrowApplication).appContainer
        val harvestDao = appContainer.database.harvestDao()
        val scheduler = ReminderScheduler(context)
        val batch = harvestDao.getBatchById(batchId) ?: return

        harvestDao.updateBatch(batch.copy(status = "CURING"))
        scheduler.scheduleBurpReminder(batchId, 24)
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[widgetFeedbackKey] = "Lote movido para cura"
        }
        GrowPlantWidget().updateAll(context)
    }
}

private object GrowWidgetRepository {
    suspend fun loadSnapshot(context: Context): GrowWidgetSnapshot {
        val appContainer = (context.applicationContext as GrowApplication).appContainer
        val plantDao = appContainer.database.plantDao()
        val harvestDao = appContainer.database.harvestDao()

        val plants = plantDao.getAllNow()
        val featuredPlant = plants.pickFeaturedPlant()

        return GrowWidgetSnapshot(
            totalPlants = plants.size,
            plantId = featuredPlant?.id,
            plantName = featuredPlant?.name,
            stage = featuredPlant?.stage,
            days = featuredPlant?.let { calculateCultivationDays(it.days, it.createdAt) },
            nextWateringDate = featuredPlant?.nextWateringDate,
            drying = harvestDao.getDryingBatchesNow().toWidgetSnapshot(),
            curing = harvestDao.getCuringBatchesNow().toWidgetSnapshot()
        )
    }
}

private fun List<PlantEntity>.pickFeaturedPlant(): PlantEntity? {
    return minWithOrNull(
        compareBy<PlantEntity>(
            { it.nextWateringDate == null },
            { it.nextWateringDate ?: Long.MAX_VALUE },
            { -calculateCultivationDays(it.days, it.createdAt) },
            { -it.createdAt }
        )
    )
}

private fun List<HarvestBatchEntity>.toWidgetSnapshot(): HarvestWidgetSnapshot {
    val first = firstOrNull()
    return HarvestWidgetSnapshot(
        count = size,
        id = first?.id,
        plantName = first?.plantName,
        harvestDate = first?.harvestDate,
        nextBurpDate = first?.nextBurpDate,
        lastBurpDate = first?.lastBurpDate
    )
}

@Composable
private fun GrowPlantWidgetContent(
    context: Context,
    snapshot: GrowWidgetSnapshot,
    openAppAction: Action,
    currentPage: Int
) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(widgetShell)
            .cornerRadius(28.dp)
            .padding(2.dp)
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(widgetGlass)
                .cornerRadius(26.dp)
                .clickable(openAppAction)
                .padding(14.dp)
        ) {
            if (snapshot.totalPlants == 0 && snapshot.drying.count == 0 && snapshot.curing.count == 0) {
                EmptyWidgetState(context = context, openAppAction = openAppAction)
            } else {
                PagedWidgetState(
                    context = context,
                    snapshot = snapshot,
                    openAppAction = openAppAction,
                    currentPage = currentPage
                )
            }
        }
    }
}

@Composable
private fun EmptyWidgetState(
    context: Context,
    openAppAction: Action
) {
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally
    ) {
        Text(
            text = context.getString(R.string.widget_empty_title),
            style = TextStyle(
                color = widgetTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = GlanceModifier.height(8.dp))
        Text(
            text = context.getString(R.string.widget_empty_subtitle),
            style = TextStyle(
                color = widgetTextSecondary,
                fontSize = 12.sp
            )
        )
        Spacer(modifier = GlanceModifier.height(18.dp))
        WidgetActionChip(
            label = context.getString(R.string.widget_open_app),
            action = openAppAction,
            accent = widgetPrimary
        )
    }
}

@Composable
private fun PagedWidgetState(
    context: Context,
    snapshot: GrowWidgetSnapshot,
    openAppAction: Action,
    currentPage: Int
) {
    Column(modifier = GlanceModifier.fillMaxSize()) {
        GlassStrip(
            leading = {
                Text(
                    text = pageTitle(context, currentPage),
                    style = TextStyle(
                        color = pageColor(currentPage),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            },
            trailing = {
                PageDots(currentPage = currentPage)
            }
        )

        Spacer(modifier = GlanceModifier.height(10.dp))

        when (currentPage) {
            0 -> FocusPage(context, snapshot)
            1 -> DryingPage(context, snapshot)
            else -> CuringPage(context, snapshot)
        }

        if (!snapshot.feedback.isNullOrBlank()) {
            Spacer(modifier = GlanceModifier.height(10.dp))
            FeedbackPill(snapshot.feedback)
        }

        Spacer(modifier = GlanceModifier.height(12.dp))

        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            WidgetActionChip(
                label = "Voltar",
                action = actionRunCallback<PreviousWidgetPageAction>(),
                accent = pageColor(currentPage)
            )
            Spacer(modifier = GlanceModifier.width(8.dp))
            WidgetActionChip(
                label = context.getString(R.string.widget_open_app),
                action = openAppAction,
                accent = widgetPrimary
            )
            Spacer(modifier = GlanceModifier.width(8.dp))
            WidgetActionChip(
                label = "Avancar",
                action = actionRunCallback<NextWidgetPageAction>(),
                accent = pageColor(currentPage)
            )
        }
    }
}

@Composable
private fun FocusPage(
    context: Context,
    snapshot: GrowWidgetSnapshot
) {
    Text(
        text = snapshot.plantName.orEmpty(),
        style = TextStyle(
            color = widgetTextPrimary,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold
        ),
        maxLines = 1
    )
    Spacer(modifier = GlanceModifier.height(6.dp))
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        WidgetBadge(snapshot.stage.orEmpty())
        Spacer(modifier = GlanceModifier.width(8.dp))
        Text(
            text = "${snapshot.days ?: 0} dias",
            style = TextStyle(
                color = widgetTextSecondary,
                fontSize = 12.sp
            )
        )
    }
    Spacer(modifier = GlanceModifier.height(12.dp))
    FrostedCard(
        title = "Proxima rega",
        value = formatNextWatering(context, snapshot.nextWateringDate),
        accent = widgetPrimary
    )
    if (snapshot.plantId != null) {
        Spacer(modifier = GlanceModifier.height(10.dp))
        InlineActionRow(
            primaryLabel = "Regar",
            primaryAction = actionRunCallback<QuickWaterAction>(
                actionParametersOf(plantIdParam to snapshot.plantId)
            ),
            primaryAccent = widgetPrimary
        )
    }
    Spacer(modifier = GlanceModifier.height(10.dp))
    MiniInfoLine(
        label = "Colecao",
        value = context.getString(R.string.widget_total_plants, snapshot.totalPlants)
    )
}

@Composable
private fun DryingPage(
    context: Context,
    snapshot: GrowWidgetSnapshot
) {
    if (snapshot.drying.count == 0) {
        EmptyHarvestState("Nenhum lote em secagem")
        return
    }

    Text(
        text = snapshot.drying.plantName.orEmpty(),
        style = TextStyle(
            color = widgetTextPrimary,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold
        ),
        maxLines = 1
    )
    Spacer(modifier = GlanceModifier.height(10.dp))
    FrostedCard(
        title = "Lotes em secagem",
        value = "${daysSince(snapshot.drying.harvestDate)} dias",
        accent = widgetDrying
    )
    if (snapshot.drying.id != null) {
        Spacer(modifier = GlanceModifier.height(10.dp))
        InlineActionRow(
            primaryLabel = "Respiro",
            primaryAction = actionRunCallback<BurpBatchAction>(
                actionParametersOf(batchIdParam to snapshot.drying.id)
            ),
            primaryAccent = widgetDrying,
            secondaryLabel = "Iniciar cura",
            secondaryAction = actionRunCallback<StartCuringAction>(
                actionParametersOf(batchIdParam to snapshot.drying.id)
            ),
            secondaryAccent = widgetCuring
        )
    }
    Spacer(modifier = GlanceModifier.height(10.dp))
    MiniInfoLine(
        label = "Proximo respiro",
        value = formatBurpTime(context, snapshot.drying.nextBurpDate)
    )
}

@Composable
private fun CuringPage(
    context: Context,
    snapshot: GrowWidgetSnapshot
) {
    if (snapshot.curing.count == 0) {
        EmptyHarvestState("Nenhum lote em cura")
        return
    }

    Text(
        text = snapshot.curing.plantName.orEmpty(),
        style = TextStyle(
            color = widgetTextPrimary,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold
        ),
        maxLines = 1
    )
    Spacer(modifier = GlanceModifier.height(10.dp))
    FrostedCard(
        title = "Lotes em cura",
        value = "${daysSince(snapshot.curing.harvestDate)} dias",
        accent = widgetCuring
    )
    if (snapshot.curing.id != null) {
        Spacer(modifier = GlanceModifier.height(10.dp))
        InlineActionRow(
            primaryLabel = "Respiro",
            primaryAction = actionRunCallback<BurpBatchAction>(
                actionParametersOf(batchIdParam to snapshot.curing.id)
            ),
            primaryAccent = widgetCuring
        )
    }
    Spacer(modifier = GlanceModifier.height(10.dp))
    MiniInfoLine(
        label = "Ultimo respiro",
        value = formatRelativeTime(snapshot.curing.lastBurpDate)
    )
}

@Composable
private fun EmptyHarvestState(text: String) {
    Column(modifier = GlanceModifier.fillMaxWidth()) {
        Text(
            text = text,
            style = TextStyle(
                color = widgetTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = GlanceModifier.height(8.dp))
        Text(
            text = "Abra o app para acompanhar os lotes.",
            style = TextStyle(
                color = widgetTextSecondary,
                fontSize = 12.sp
            )
        )
    }
}

@Composable
private fun GlassStrip(
    leading: @Composable () -> Unit,
    trailing: @Composable () -> Unit
) {
    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(widgetPanel)
            .cornerRadius(18.dp)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            leading()
            Spacer(modifier = GlanceModifier.width(8.dp))
            trailing()
        }
    }
}

@Composable
private fun PageDots(currentPage: Int) {
    Row(
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        repeat(widgetPageCount) { index ->
            val active = index == currentPage
            Box(
                modifier = GlanceModifier
                    .background(if (active) pageColor(currentPage) else widgetTextSecondary)
                    .cornerRadius(999.dp)
                    .padding(
                        horizontal = if (active) 6.dp else 3.dp,
                        vertical = 3.dp
                    )
            ) {}
            if (index < widgetPageCount - 1) {
                Spacer(modifier = GlanceModifier.width(4.dp))
            }
        }
    }
}

@Composable
private fun InlineActionRow(
    primaryLabel: String,
    primaryAction: Action,
    primaryAccent: GlanceColorProvider,
    secondaryLabel: String? = null,
    secondaryAction: Action? = null,
    secondaryAccent: GlanceColorProvider? = null
) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        WidgetActionChip(
            label = primaryLabel,
            action = primaryAction,
            accent = primaryAccent
        )
        if (secondaryLabel != null && secondaryAction != null && secondaryAccent != null) {
            Spacer(modifier = GlanceModifier.width(8.dp))
            WidgetActionChip(
                label = secondaryLabel,
                action = secondaryAction,
                accent = secondaryAccent
            )
        }
    }
}

@Composable
private fun FeedbackPill(message: String) {
    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(widgetPanel)
            .cornerRadius(14.dp)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(
            text = message,
            style = TextStyle(
                color = widgetTextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
private fun FrostedCard(
    title: String,
    value: String,
    accent: GlanceColorProvider
) {
    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(accent)
            .cornerRadius(20.dp)
            .padding(1.dp)
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(widgetPanel)
                .cornerRadius(19.dp)
                .padding(12.dp)
        ) {
            Text(
                text = title,
                style = TextStyle(
                    color = widgetTextSecondary,
                    fontSize = 11.sp
                )
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = value,
                style = TextStyle(
                    color = widgetTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Composable
private fun WidgetBadge(text: String) {
    Box(
        modifier = GlanceModifier
            .background(widgetBadge)
            .cornerRadius(999.dp)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = TextStyle(
                color = ColorProvider(day = Color.White, night = Color(0xFF203018)),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
private fun WidgetActionChip(
    label: String,
    action: Action,
    accent: GlanceColorProvider
) {
    Box(
        modifier = GlanceModifier
            .background(accent)
            .cornerRadius(999.dp)
            .padding(1.dp)
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(widgetGlass)
                .cornerRadius(999.dp)
                .clickable(action)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = label,
                style = TextStyle(
                    color = widgetTextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

@Composable
private fun MiniInfoLine(
    label: String,
    value: String
) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        Text(
            text = "$label: ",
            style = TextStyle(
                color = widgetTextSecondary,
                fontSize = 11.sp
            )
        )
        Text(
            text = value,
            style = TextStyle(
                color = widgetTextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

private fun formatNextWatering(context: Context, nextWateringDate: Long?): String {
    if (nextWateringDate == null) {
        return context.getString(R.string.widget_watering_unscheduled)
    }

    val now = System.currentTimeMillis()
    val oneDayMs = 24L * 60L * 60L * 1_000L
    val todayStart = now - (now % oneDayMs)
    val diffDays = ((nextWateringDate - todayStart) / oneDayMs).toInt()

    return when {
        diffDays < 0 -> context.getString(R.string.widget_watering_overdue)
        diffDays == 0 -> context.getString(R.string.widget_watering_today)
        diffDays == 1 -> context.getString(R.string.widget_watering_tomorrow)
        else -> SimpleDateFormat("dd/MM", Locale.forLanguageTag("pt-BR")).format(Date(nextWateringDate))
    }
}

private fun pageTitle(context: Context, currentPage: Int): String = when (currentPage) {
    0 -> "Planta"
    1 -> "Secagem"
    else -> "Cura"
}

private fun pageColor(currentPage: Int) = when (currentPage) {
    0 -> widgetPrimary
    1 -> widgetDrying
    else -> widgetCuring
}

private fun daysSince(timestamp: Long?): Int {
    if (timestamp == null) return 0
    return ((System.currentTimeMillis() - timestamp) / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(0)
}

private fun formatBurpTime(context: Context, timestamp: Long?): String {
    if (timestamp == null) return context.getString(R.string.widget_watering_unscheduled)
    return SimpleDateFormat("dd/MM HH:mm", Locale.forLanguageTag("pt-BR")).format(Date(timestamp))
}

private fun formatRelativeTime(timestamp: Long?): String {
    if (timestamp == null) return "Nao feito"
    val diff = System.currentTimeMillis() - timestamp
    val hours = diff / (1000 * 60 * 60)
    val days = hours / 24
    return when {
        days > 0 -> "${days}d atras"
        hours > 0 -> "${hours}h atras"
        else -> "Agora"
    }
}

private fun Int.floorMod(other: Int): Int = ((this % other) + other) % other
