/**
 *
 * Please note:
 * This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * Do not edit this file manually.
 *
 */

@file:Suppress(
    "ArrayInDataClass",
    "EnumEntryName",
    "RemoveRedundantQualifierName",
    "UnusedImport"
)

package io.redlink.more.more_app_mutliplatform.services.network.openapi.model


import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.JsonObject

/**
 * 
 *
 * @param dataId 
 * @param observationId 
 * @param observationType 
 * @param dataValue 
 * @param timestamp 
 */
@Serializable

data class ObservationData (

    @SerialName(value = "dataId") @Required val dataId: kotlin.String,

    @SerialName(value = "observationId") @Required val observationId: kotlin.String,

    @SerialName(value = "observationType") @Required val observationType: kotlin.String,

    @SerialName(value = "dataValue") @Required val configuration: JsonObject? = null,

    @SerialName(value = "timestamp") @Required val timestamp: kotlin.String

)

