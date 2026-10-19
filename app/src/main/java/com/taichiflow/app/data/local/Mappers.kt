package com.taichiflow.app.data.local

import com.taichiflow.app.data.catalog.MovementCatalog
import com.taichiflow.app.domain.model.ActivityType
import com.taichiflow.app.domain.model.Attribute
import com.taichiflow.app.domain.model.BreathPattern
import com.taichiflow.app.domain.model.Difficulty
import com.taichiflow.app.domain.model.HistoryRecord
import com.taichiflow.app.domain.model.MovementProgress
import com.taichiflow.app.domain.model.PracticeCategory
import com.taichiflow.app.domain.model.PracticeSession

private fun <T : Enum<T>> safeEnum(values: Array<T>, name: String): T? = values.firstOrNull { it.name == name }

fun HistoryEntity.toDomain(): HistoryRecord {
    val scores = buildMap<Attribute, Int> {
        if (balance > 0) put(Attribute.BALANCE, balance)
        if (stability > 0) put(Attribute.STABILITY, stability)
        if (mobility > 0) put(Attribute.MOBILITY, mobility)
        if (coordination > 0) put(Attribute.COORDINATION, coordination)
        if (breathing > 0) put(Attribute.BREATHING, breathing)
        if (rhythm > 0) put(Attribute.RHYTHM, rhythm)
        if (focus > 0) put(Attribute.FOCUS, focus)
        if (flow > 0) put(Attribute.FLOW, flow)
    }
    return HistoryRecord(
        id = id,
        sessionId = sessionId,
        name = name,
        type = safeEnum(ActivityType.values(), type) ?: ActivityType.PRACTICE,
        category = if (category.isBlank()) null else safeEnum(PracticeCategory.values(), category),
        timestamp = timestamp,
        durationSec = durationSec,
        score = score,
        xp = xp,
        scores = scores,
        details = details,
        movementIds = movementIds.split(',').filter { it.isNotBlank() },
        completion = completion,
        isRecord = isRecord,
        recordLabel = recordLabel,
        daily = daily
    )
}

fun historyEntity(
    sessionId: String,
    name: String,
    type: ActivityType,
    category: PracticeCategory?,
    timestamp: Long,
    durationSec: Int,
    score: Int,
    xp: Int,
    scores: Map<Attribute, Int>,
    details: String,
    movementIds: List<String>,
    completion: Int,
    isRecord: Boolean,
    recordLabel: String,
    daily: Boolean
) = HistoryEntity(
    sessionId = sessionId,
    name = name,
    type = type.name,
    category = category?.name ?: "",
    timestamp = timestamp,
    durationSec = durationSec,
    score = score,
    xp = xp,
    balance = scores[Attribute.BALANCE] ?: 0,
    stability = scores[Attribute.STABILITY] ?: 0,
    mobility = scores[Attribute.MOBILITY] ?: 0,
    coordination = scores[Attribute.COORDINATION] ?: 0,
    breathing = scores[Attribute.BREATHING] ?: 0,
    rhythm = scores[Attribute.RHYTHM] ?: 0,
    focus = scores[Attribute.FOCUS] ?: 0,
    flow = scores[Attribute.FLOW] ?: 0,
    details = details,
    movementIds = movementIds.joinToString(","),
    completion = completion,
    isRecord = isRecord,
    recordLabel = recordLabel,
    daily = daily
)

fun CustomSessionEntity.toDomain(): PracticeSession {
    val moves = movementIds.split(',').filter { it.isNotBlank() }
    val resolved = MovementCatalog.resolve(moves)
    val focus = resolved.flatMap { listOf(it.primary, it.secondary) }
        .groupingBy { it }.eachCount()
        .entries.sortedByDescending { it.value }
        .take(2).map { it.key }
        .ifEmpty { listOf(Attribute.FLOW) }
    val difficulty = resolved.maxByOrNull { it.difficulty.rank }?.difficulty ?: Difficulty.BEGINNER
    val category = when (focus.first()) {
        Attribute.BALANCE, Attribute.STABILITY -> PracticeCategory.BALANCE
        Attribute.MOBILITY -> PracticeCategory.MOBILITY
        Attribute.COORDINATION, Attribute.RHYTHM -> PracticeCategory.COORDINATION
        Attribute.BREATHING -> PracticeCategory.BREATHING
        Attribute.FOCUS -> PracticeCategory.RECOVERY
        Attribute.FLOW -> PracticeCategory.FLOW
    }
    return PracticeSession(
        id = "custom_$id",
        name = name,
        category = category,
        difficulty = difficulty,
        focus = focus,
        movementIds = moves,
        workSec = workSec,
        restSec = restSec,
        rounds = rounds,
        breath = BreathPattern(inhale, hold, exhale, holdOut),
        description = "Your custom session · ${moves.size} movements",
        isCustom = true
    )
}

fun MovementProgressEntity.toDomain() = MovementProgress(movementId, timesCompleted, totalSec, lastPracticed)
