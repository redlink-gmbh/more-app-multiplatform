package io.redlink.more.more_app_mutliplatform.models

import io.redlink.more.more_app_mutliplatform.database.schemas.StudySchema
import io.redlink.more.more_app_mutliplatform.services.network.openapi.model.Study


data class PermissionModel(
    val studyTitle: String,
    val studyParticipantInfo: String,
    val studyConsentInfo: String,
    val consentInfo: List<PermissionConsentModel>
) {
    companion object {
        fun create(study: Study): PermissionModel {
            val observationConsent = mutableListOf<PermissionConsentModel>()
            observationConsent.add(PermissionConsentModel("Study Consent", study.consentInfo))
            observationConsent.addAll( study.observations.map { PermissionConsentModel(it.observationTitle, it.participantInfo) })
            return PermissionModel(study.studyTitle, study.participantInfo, study.consentInfo, observationConsent)
        }
        fun createFromSchema(studySchema: StudySchema): PermissionModel {
            val observationConsent = mutableListOf<PermissionConsentModel>()
            observationConsent.add(PermissionConsentModel(studySchema.studyTitle, studySchema.consentInfo))
            observationConsent.addAll(studySchema.observations.map { PermissionConsentModel(it.observationTitle, it.participantInfo) })
            return PermissionModel(studySchema.studyTitle, studySchema.participantInfo, studySchema.consentInfo, observationConsent)
        }
    }
}