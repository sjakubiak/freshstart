package com.freshstart.app.data

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.MealType
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Mass
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Today's movement as reported by Health Connect (Fitbit, Pixel Watch, Google Fit and so on). */
data class DayActivity(val steps: Long, val activeCalories: Int)

/**
 * Two-way link with Health Connect, the on-phone health data hub Pixel phones use.
 * Weigh-ins come in from smart scales and Fitbit, and go out along with meals.
 * Steps and active calories are read for display.
 */
class HealthSync(private val context: Context) {
    val permissions = setOf(
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getWritePermission(WeightRecord::class),
        HealthPermission.getWritePermission(NutritionRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class)
    )

    private val client by lazy { HealthConnectClient.getOrCreate(context) }
    private val zone get() = ZoneId.systemDefault()

    fun status(): Int = HealthConnectClient.getSdkStatus(context)

    fun isAvailable() = status() == HealthConnectClient.SDK_AVAILABLE

    suspend fun grantedPermissions(): Set<String> =
        if (isAvailable()) client.permissionController.getGrantedPermissions() else emptySet()

    private suspend fun has(permission: String) = grantedPermissions().contains(permission)

    /** Weigh-ins recorded by other apps over the last year, latest one per day. */
    suspend fun readWeights(): Map<String, Double> {
        if (!has(HealthPermission.getReadPermission(WeightRecord::class))) return emptyMap()
        val end = java.time.Instant.now()
        val start = end.minus(java.time.Duration.ofDays(365))
        val records = client.readRecords(
            ReadRecordsRequest(WeightRecord::class, TimeRangeFilter.between(start, end), pageSize = 1000)
        ).records
        return records
            .filter { it.metadata.dataOrigin.packageName != context.packageName }
            .sortedBy { it.time }
            .associate { it.time.atZone(zone).toLocalDate().toString() to it.weight.inKilograms }
    }

    suspend fun activityOn(date: LocalDate): DayActivity? {
        val granted = grantedPermissions()
        val canSteps = granted.contains(HealthPermission.getReadPermission(StepsRecord::class))
        val canCalories = granted.contains(HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class))
        if (!canSteps && !canCalories) return null
        val metrics = buildSet {
            if (canSteps) add(StepsRecord.COUNT_TOTAL)
            if (canCalories) add(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL)
        }
        val start = date.atStartOfDay(zone).toInstant()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant()
        val result = client.aggregate(AggregateRequest(metrics, TimeRangeFilter.between(start, end)))
        return DayActivity(
            steps = result[StepsRecord.COUNT_TOTAL] ?: 0L,
            activeCalories = result[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories?.toInt() ?: 0
        )
    }

    suspend fun writeWeight(entry: WeightEntry) {
        if (!has(HealthPermission.getWritePermission(WeightRecord::class))) return
        val time = LocalDate.parse(entry.date).atTime(LocalTime.of(8, 0)).atZone(zone)
        client.insertRecords(
            listOf(
                WeightRecord(
                    time = time.toInstant(),
                    zoneOffset = time.offset,
                    weight = Mass.kilograms(entry.kg),
                    metadata = Metadata.manualEntry(
                        clientRecordId = "weight-${entry.date}",
                        clientRecordVersion = System.currentTimeMillis()
                    )
                )
            )
        )
    }

    suspend fun deleteWeight(date: String) {
        if (!has(HealthPermission.getWritePermission(WeightRecord::class))) return
        client.deleteRecords(WeightRecord::class, emptyList(), listOf("weight-$date"))
    }

    suspend fun writeFood(entry: FoodEntry) {
        if (!has(HealthPermission.getWritePermission(NutritionRecord::class))) return
        val hour = when (entry.meal) {
            Meal.BREAKFAST -> 8
            Meal.LUNCH -> 12
            Meal.DINNER -> 18
            Meal.SNACK -> 15
        }
        val start = LocalDate.parse(entry.date).atTime(hour, 0).atZone(zone)
        val end = start.plusMinutes(15)
        client.insertRecords(
            listOf(
                NutritionRecord(
                    startTime = start.toInstant(),
                    startZoneOffset = start.offset,
                    endTime = end.toInstant(),
                    endZoneOffset = end.offset,
                    energy = Energy.kilocalories(entry.calories.toDouble()),
                    name = entry.name,
                    mealType = when (entry.meal) {
                        Meal.BREAKFAST -> MealType.MEAL_TYPE_BREAKFAST
                        Meal.LUNCH -> MealType.MEAL_TYPE_LUNCH
                        Meal.DINNER -> MealType.MEAL_TYPE_DINNER
                        Meal.SNACK -> MealType.MEAL_TYPE_SNACK
                    },
                    metadata = Metadata.manualEntry(
                        clientRecordId = "food-${entry.id}",
                        clientRecordVersion = 1
                    )
                )
            )
        )
    }

    suspend fun deleteFood(id: Long) {
        if (!has(HealthPermission.getWritePermission(NutritionRecord::class))) return
        client.deleteRecords(NutritionRecord::class, emptyList(), listOf("food-$id"))
    }
}
