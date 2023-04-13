package io.redlink.more.more_app_mutliplatform.viewModels.simpleQuestion

import io.github.aakira.napier.Napier
import io.redlink.more.more_app_mutliplatform.database.repository.ObservationRepository
import io.redlink.more.more_app_mutliplatform.database.repository.ScheduleRepository
import io.redlink.more.more_app_mutliplatform.models.SimpleQuestionModel
import io.redlink.more.more_app_mutliplatform.models.TaskDetailsModel
import io.redlink.more.more_app_mutliplatform.observations.Observation
import io.redlink.more.more_app_mutliplatform.observations.ObservationFactory
import io.redlink.more.more_app_mutliplatform.observations.simpleQuestionObservation.SimpleQuestionObservation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class SimpleQuestionCoreViewModel(
    private val scheduleId: String,
    private val observationFactory: ObservationFactory
) {
    private val scheduleRepository: ScheduleRepository = ScheduleRepository()
    private val observationRepository: ObservationRepository = ObservationRepository()

    val simpleQuestionModel = MutableStateFlow<SimpleQuestionModel?>(null)
    private var observation: Observation = SimpleQuestionObservation()

    private val scope = CoroutineScope(Dispatchers.Default + Job())


    init {
        observationFactory.observation("question-observation")?.let { observation = it }
        scope.launch {
            scheduleRepository.scheduleWithId(scheduleId).firstOrNull()?.let{ scheduleSchema ->
                observationRepository.observationById(scheduleSchema.observationId).firstOrNull()?.let { observationSchema ->
                    simpleQuestionModel.emit(SimpleQuestionModel.createModelFrom(observationSchema))
                }
            }
        }
    }

    fun finishQuestion(data: String, setObservationToDone: Boolean){
        simpleQuestionModel.value?.observationId?.let {
            observation.start(it, scheduleId)
            observation.storeData(object { val answer = data })
            scheduleRepository.setCompletionStateFor(scheduleId, true)
            observation.stop(it)
        }
    }
}
