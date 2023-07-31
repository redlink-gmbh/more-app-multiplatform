package io.redlink.more.more_app_mutliplatform.services.network

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.statement.HttpResponse
import io.ktor.utils.io.core.Closeable
import io.realm.kotlin.internal.platform.WeakReference
import io.redlink.more.app.android.services.network.errors.NetworkServiceError
import io.redlink.more.more_app_mutliplatform.services.network.openapi.api.ConfigurationApi
import io.redlink.more.more_app_mutliplatform.services.network.openapi.api.DataApi
import io.redlink.more.more_app_mutliplatform.services.network.openapi.api.RegistrationApi
import io.redlink.more.more_app_mutliplatform.services.network.openapi.model.AppConfiguration
import io.redlink.more.more_app_mutliplatform.services.network.openapi.model.DataBulk
import io.redlink.more.more_app_mutliplatform.services.network.openapi.model.Error
import io.redlink.more.more_app_mutliplatform.services.network.openapi.model.PushNotificationServiceType
import io.redlink.more.more_app_mutliplatform.services.network.openapi.model.PushNotificationToken
import io.redlink.more.more_app_mutliplatform.services.network.openapi.model.Study
import io.redlink.more.more_app_mutliplatform.services.network.openapi.model.StudyConsent
import io.redlink.more.more_app_mutliplatform.services.store.CredentialRepository
import io.redlink.more.more_app_mutliplatform.services.store.EndpointRepository
import io.redlink.more.more_app_mutliplatform.util.Scope
import kotlinx.coroutines.cancel
import kotlinx.serialization.json.Json

private const val TAG = "NetworkService"

class NetworkService(
    private val endpointRepository: EndpointRepository,
    private val credentialRepository: CredentialRepository
) : Closeable {
    private var httpClient: HttpClient? = null
    private var configurationApi: ConfigurationApi? = null
    private var dataApi: WeakReference<DataApi>? = null

    private var engineUseCounter = 10

    private fun initConfigApi() {
        if (configurationApi == null) {
            Napier.d { "Initializing ConfigurationApi and DataApi..." }
            credentialRepository.credentials()?.let {
                httpClient = getHttpClient().apply {
                    val url = endpointRepository.endpoint()
                    configurationApi = ConfigurationApi(
                        baseUrl = url,
                        engine,
                        httpClientConfig = { engineConfig })
                    configurationApi?.setUsername(it.apiId)
                    configurationApi?.setPassword(it.apiKey)
                }
            }
        }
    }

    private fun initDataApi() {
        if (dataApi == null || dataApi?.get() == null) {
            credentialRepository.credentials()?.let {
                httpClient = getHttpClient().apply {
                    val api = DataApi(
                        baseUrl = endpointRepository.endpoint(),
                        engine,
                        httpClientConfig = { engineConfig }
                    )
                    api.setUsername(it.apiId)
                    api.setPassword(it.apiKey)
                    dataApi = WeakReference(api)
                }
            }
        }
    }

    suspend fun deleteParticipation(): Pair<Boolean, NetworkServiceError?> {
        try {
            credentialRepository.credentials()?.let {
                Napier.d { "Deleting Participation..." }
                val httpClient = getHttpClient()
                val url = endpointRepository.endpoint()
                val registrationApi =
                    RegistrationApi(baseUrl = url, httpClientEngine = httpClient.engine)

                registrationApi.setUsername(it.apiId)
                registrationApi.setPassword(it.apiKey)

                val registrationResponse =
                    registrationApi.unregisterFromStudy() ?: return Pair(false, NetworkServiceError(0, "Response null"))
                Napier.d(registrationResponse.response.toString(), tag = TAG)
                close()
                if (registrationResponse.success) {
                    Napier.d { "Participation deleted!" }
                    return Pair(true, null)
                }
                println("Error; Code: ${registrationResponse.response.status.value}")
                val error = createErrorBody(
                    registrationResponse.response.status.value,
                    registrationResponse.response
                )
                return Pair(
                    false,
                    error
                )
            }
            return Pair(false, NetworkServiceError(null, "No credentials"))
        } catch (err: Exception) {
            err.printStackTrace()
            return Pair(false, getException(err))
        }
    }

    suspend fun validateRegistrationToken(
        registrationToken: String,
        endpoint: String? = null
    ): Pair<Study?, NetworkServiceError?> {
        try {
            Napier.d { "Validating Registration token..." }
            val httpClient = getHttpClient()
            val url = endpoint ?: endpointRepository.endpoint()
            val registrationApi =
                RegistrationApi(baseUrl = url, httpClientEngine = httpClient.engine)
            val registrationResponse =
                registrationApi.getStudyRegistrationInfo(moreRegistrationToken = registrationToken) ?: return Pair(null, NetworkServiceError(0, "Response null"))
            Napier.d(registrationResponse.response.toString(), tag = TAG)
            if (registrationResponse.success) {
                registrationResponse.body().let {
                    Napier.d { "Registration token valid!" }
                    return Pair(it.get(), null)
                }
            }
            val error = createErrorBody(
                registrationResponse.response.status.value,
                registrationResponse.response
            )
            return Pair(
                null,
                error
            )

        } catch (err: Exception) {
            return Pair(null, getException(err))
        }
    }

    suspend fun sendConsent(
        registrationToken: String,
        studyConsent: StudyConsent,
        endpoint: String? = null
    ): Pair<AppConfiguration?, NetworkServiceError?> {
        try {
            Napier.d { "Sending Consent..." }
            val httpClient = getHttpClient()
            val url = endpoint ?: endpointRepository.endpoint()
            val registrationApi = RegistrationApi(
                baseUrl = url,
                httpClient.engine,
                httpClientConfig = { httpClient.engineConfig })
            val consentResponse = registrationApi.registerForStudy(registrationToken, studyConsent) ?: return Pair(null, NetworkServiceError(0, "Response null"))
            if (consentResponse.success) {
                consentResponse.body().let {
                    Napier.d { "Credentials received!" }
                    return Pair(it.get(), null)
                }
            }
            return Pair(
                null,
                createErrorBody(consentResponse.response.status.value, consentResponse.response)
            )
        } catch (e: Exception) {
            return Pair(null, getException(e))
        }
    }

    suspend fun getStudyConfig(): Pair<Study?, NetworkServiceError?> {
        initConfigApi()
        try {
            Napier.d { "Downloading study data..." }
            val configResponse =
                configurationApi?.getStudyConfiguration() ?: return Pair(
                    null,
                    NetworkServiceError(null, "No credentials set!")
                )
            if (configResponse.success) {
                configResponse.body().let {
                    Napier.d { "Loading study data success!" }
                    return Pair(it.get(), null)
                }
            }

            return Pair(
                null,
                createErrorBody(configResponse.response.status.value, configResponse.response)
            )
        } catch (e: Exception) {
            return Pair(null, getException(e))
        }
    }

    suspend fun sendNotificationToken(token: String): Pair<Boolean, NetworkServiceError?> {
        initConfigApi()
        configurationApi?.let {
            try {
                Napier.d { "Sending notification token..." }
                val tokenResponse = it.setPushNotificationToken(
                    serviceType = PushNotificationServiceType.FCM,
                    pushNotificationToken = PushNotificationToken(token = token)
                ) ?: return Pair(false, NetworkServiceError(0, "Response null"))
                if (tokenResponse.success) {
                    Napier.d { "Uploading notification token success!" }
                    return Pair(true, null)
                }
                return Pair(
                    false,
                    createErrorBody(tokenResponse.status, tokenResponse.response)
                )
            } catch (err: Exception) {
                return Pair(false, getException(err))
            }
        }
        return Pair(false, getException(Exception("No credentials found!")))
    }

    suspend fun sendData(data: DataBulk): Pair<Set<String>, NetworkServiceError?> {
        initDataApi()
        try {
            Napier.i { "Sending bulk ${data.bulkId} with ${data.dataPoints.size} datapoints wwith first being ${data.dataPoints.first()}..." }
            val dataApiResponse = WeakReference(dataApi?.get()?.storeBulk(data) ?: return Pair(
                emptySet(),
                NetworkServiceError(null, "No credentials set!")
            ))
            dataApiResponse.get()?.let { dataApiResponse ->
                if (dataApiResponse.success) {
                    dataApiResponse.body().let {
                        Napier.d { "Sent data!" }
                        dataApiResponse.response.cancel()
                        return Pair(it.get()?.toSet() ?: emptySet(), null)
                    }
                }
            }
            dataApiResponse.get()?.response?.cancel()

            return Pair(
                emptySet(),
                createErrorBody(dataApiResponse.get()?.response?.status?.value ?: 500, dataApiResponse.get()?.response)
            )
        } catch (e: Exception) {
            return Pair(emptySet(), getException(e))
        }
    }

    fun iosSendData(
        data: DataBulk,
        completionHandler: (WeakReference<Pair<Set<String>, NetworkServiceError?>>) -> Unit
    ) {
        Scope.launch {
            completionHandler(
                WeakReference(sendData(data))
            )
        }
    }

    private fun createErrorBody(code: Int, responseBody: HttpResponse?): NetworkServiceError {
        return try {
            if (responseBody == null) {
                return NetworkServiceError(code = code, message = "Error")
            }
            val error =
                Json.decodeFromString<Error>(
                    responseBody.toString()
                )
            NetworkServiceError(code = code, message = error.msg ?: "Error")
        } catch (e: Exception) {
            getException(e)
        }
    }

    private fun getException(exception: Exception): NetworkServiceError {
        val errorResponse = when (exception) {
            else -> "System error!"
        }
        Napier.e("Exception: ${exception.stackTraceToString()}", tag = TAG)
        exception.printStackTrace()
        return NetworkServiceError(null, errorResponse)
    }

    override fun close() {
        Napier.d { "Clearing the Http engine..." }
        engineUseCounter = 10
        configurationApi = null
        dataApi = null
        httpClient?.close()
        httpClient = null
    }
}

