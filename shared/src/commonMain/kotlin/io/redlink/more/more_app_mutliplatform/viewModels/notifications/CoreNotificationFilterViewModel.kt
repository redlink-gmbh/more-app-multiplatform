package io.redlink.more.more_app_mutliplatform.viewModels.notifications

import io.ktor.utils.io.core.Closeable
import io.redlink.more.more_app_mutliplatform.extensions.append
import io.redlink.more.more_app_mutliplatform.extensions.asClosure
import io.redlink.more.more_app_mutliplatform.extensions.remove
import io.redlink.more.more_app_mutliplatform.extensions.set
import io.redlink.more.more_app_mutliplatform.models.NotificationFilterTypeModel
import io.redlink.more.more_app_mutliplatform.models.NotificationModel
import io.redlink.more.more_app_mutliplatform.viewModels.CoreViewModel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow

class CoreNotificationFilterViewModel: CoreViewModel() {
    private var highPriority: Long = 2

    val filters = MutableStateFlow<Map<NotificationFilterTypeModel, Boolean>>(mapOf())
    init {
        val map = getEnumAsList().associateWith { false }.toMutableMap()
        map[NotificationFilterTypeModel.ALL] = true
        filters.set(map)
    }

    fun toggleFilter(filter: NotificationFilterTypeModel) {
        var filterMap = filters.value.toMutableMap()
        if (filter == NotificationFilterTypeModel.ALL) {
            filterMap = filterMap.mapValues { false }.toMutableMap()
            filterMap[NotificationFilterTypeModel.ALL] = true
        }
        else if (filterMap[NotificationFilterTypeModel.ALL] == true){
            filterMap[NotificationFilterTypeModel.ALL] = false
            filterMap[filter] = true
        }
        else if (filterMap[filter] == true) {
            if (filterMap.values.filter { it }.size == 1) {
                filterMap = filterMap.mapValues { false }.toMutableMap()
                filterMap[NotificationFilterTypeModel.ALL] = true
            } else {
                filterMap[filter] = false
            }
        } else {
            filterMap[filter] = true
        }
        filters.set(filterMap)
    }

    override fun viewDidAppear() {

    }

    fun setPlatformHighPriority(priority: Long) {
        highPriority = priority
    }

    fun applyFilter(notificationList: List<NotificationModel>): List<NotificationModel> {
        return if (filterActive()) {
            notificationList.filter { notification ->
                    if (filters.value[NotificationFilterTypeModel.IMPORTANT] == true) {
                        notification.priority == highPriority
                    } else {
                        true
                    } && if (filters.value[NotificationFilterTypeModel.UNREAD] == true) {
                        !notification.read
                    } else {
                        true
                    }
            }
        } else notificationList
    }

    fun filterActive() = filters.value[NotificationFilterTypeModel.ALL] == false

    private fun getEnumAsList(): List<NotificationFilterTypeModel> {
        return NotificationFilterTypeModel.values().toList()
    }

    fun getActiveTypes() = filters.value.filter { it.value }.map { it.key.type }.toSet()

    fun onFilterChange(provideNewstate: (Map<NotificationFilterTypeModel, Boolean>) -> Unit) = filters.asClosure(provideNewstate)
}