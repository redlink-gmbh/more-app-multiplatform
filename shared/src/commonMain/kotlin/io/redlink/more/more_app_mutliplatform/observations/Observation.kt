package io.redlink.more.more_app_mutliplatform.observations

import io.github.aakira.napier.Napier
import io.redlink.more.more_app_mutliplatform.database.repository.ScheduleRepository
import io.redlink.more.more_app_mutliplatform.database.schemas.ObservationDataSchema
import io.redlink.more.more_app_mutliplatform.models.ScheduleState
import io.redlink.more.more_app_mutliplatform.observations.observationTypes.ObservationType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

abstract class Observation(val observationType: ObservationType) {
    private val scheduleRepository = ScheduleRepository()
    private var dataManager: ObservationDataManager? = null
    private var running = false
    private val observationIds = mutableSetOf<String>()
    private val scheduleIds = mutableMapOf<String, String>()
    private val config = mutableMapOf<String, Any>()
    private var configChanged = false

    protected var lastCollectionTimestamp: Instant = Clock.System.now()


    fun apply(observationId: String, scheduleId: String) {
        observationIds.add(observationId)
        scheduleIds[scheduleId] = observationId
    }

    fun remove(observationId: String, scheduleId: String) {
        observationIds.remove(observationId)
        scheduleIds.remove(scheduleId)
    }

    fun start(observationId: String, scheduleId: String): Boolean {
        observationIds.add(observationId)
        scheduleIds[scheduleId] = observationId
        if (running && configChanged) {
            stopAndFinish(scheduleId)
        }
        configChanged = false
        return if (!running) {
            Napier.i { "Observation with type ${observationType.observationType} starting" }
            applyObservationConfig(config)
            running = start()
            return running
        } else true
    }

    fun stop(scheduleId: String) {
        if (observationIds.size <= 1) {
            stop {
                finish()
                observationShutdown(scheduleId)
            }
        } else {
            finish()
        }
    }

    fun observationDataManagerAdded() = dataManager != null

    fun setDataManager(observationDataManager: ObservationDataManager) {
        dataManager = observationDataManager
    }

    fun observationConfig(settings: Map<String, Any>) {
        this.lastCollectionTimestamp = (settings[CONFIG_LAST_COLLECTION_TIMESTAMP] as? Long)?.let {
            Instant.fromEpochSeconds(it, 0)
        } ?: Clock.System.now()
        if (settings.isNotEmpty()) {
            val newConfig = this.config + settings
            if (newConfig != this.config) {
                configChanged = true
                this.config += newConfig
            }
        }
    }

    protected fun collectionTimestampToNow() {
        this.lastCollectionTimestamp = Clock.System.now()
    }

    protected abstract fun start(): Boolean

    protected abstract fun stop(onCompletion: () -> Unit)

    abstract fun observerAccessible(): Boolean

    protected abstract fun applyObservationConfig(settings: Map<String, Any>)

    open fun bleDevicesNeeded(): Set<String> = emptySet()

    open fun ableToAutomaticallyStart() = true

    fun storeData(data: Any, timestamp: Long = -1, onCompletion: () -> Unit = {}) {
        val dataSchemas = ObservationDataSchema.fromData(observationIds.toSet(), setOf(
            ObservationBulkModel(data, timestamp)
        )).map { observationType.addObservationType(it) }
        Napier.d { "Observation ${observationType.observationType} recorded new data: $data" }
        dataManager?.add(dataSchemas, scheduleIds.keys)
        onCompletion()
    }

    fun storeData(data: List<ObservationBulkModel>, onCompletion: () -> Unit) {
        val dataSchemas = ObservationDataSchema.fromData(observationIds.toSet(), data).map { observationType.addObservationType(it) }
        Napier.d { "Observation ${observationType.observationType} recorded ${data.size} new datapoints!" }
        dataManager?.add(dataSchemas, scheduleIds.keys)
        onCompletion()
    }

    fun stopAndFinish(scheduleId: String) {
        stop {
            finish()
            observationShutdown(scheduleId)
        }
    }

    fun stopAndSetState(state: ScheduleState = ScheduleState.ACTIVE, scheduleId: String?) {
        stop {
            finish()
            scheduleIds.keys.forEach { scheduleRepository.setRunningStateFor(it, state) }
            scheduleId?.let {
                observationShutdown(it)
            }
        }
    }

    fun stopAndSetDone(scheduleId: String) {
        stop {
            finish()
            scheduleIds.keys.forEach { scheduleRepository.setCompletionStateFor(it, true) }
            observationShutdown(scheduleId)
            removeDataCount()
        }
    }

    open fun store(start: Long = -1, end: Long = -1, onCompletion: () -> Unit) {
        dataManager?.store()
        onCompletion()
    }

    private fun observationShutdown(scheduleId: String) {
        val observationId = scheduleIds.remove(scheduleId)
        observationId?.let { observationIds.remove(it) }
        if (observationIds.isEmpty()) {
            config.clear()
            configChanged = false
            running = false
        }
    }

    protected fun finish() {
        dataManager?.saveAndSend()
    }

    fun removeDataCount() {
        scheduleIds.keys.forEach {
            dataManager?.removeDataPointCount(it)
        }
        scheduleIds.clear()
    }

    fun isRunning() = running

    companion object {
        const val CONFIG_TASK_START = "observation_start_date_time"
        const val CONFIG_TASK_STOP = "observation_stop_date_time"
        const val SCHEDULE_ID = "schedule_id"
        const val CONFIG_LAST_COLLECTION_TIMESTAMP = "observation_last_collection_timestamp"
    }
}
