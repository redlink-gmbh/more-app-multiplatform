package io.redlink.more.more_app_mutliplatform.database.schemas

import io.realm.kotlin.types.RealmInstant
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import io.redlink.more.more_app_mutliplatform.extensions.toRealmInstant
import io.redlink.more.more_app_mutliplatform.services.network.openapi.model.ObservationSchedule
import org.mongodb.kbson.ObjectId

class ScheduleSchema : RealmObject {
    @PrimaryKey
    var scheduleId: ObjectId = ObjectId.invoke()
    var observationId: String = ""
    var start: RealmInstant? = null
    var end: RealmInstant? = null
    var done: Boolean = false

    companion object {
        fun toSchema(schedule: ObservationSchedule, observationId: String): ScheduleSchema {
            return ScheduleSchema().apply {
                this.observationId = observationId
                start = schedule.start?.toRealmInstant()
                end = schedule.end?.toRealmInstant()
            }
        }
    }
}