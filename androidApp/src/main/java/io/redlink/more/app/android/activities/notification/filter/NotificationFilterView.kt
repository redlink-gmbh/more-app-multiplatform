package io.redlink.more.app.android.activities.notification.filter

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Done
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.redlink.more.app.android.R
import io.redlink.more.app.android.extensions.getString
import io.redlink.more.app.android.extensions.getStringResource
import io.redlink.more.app.android.shared_composables.HeaderDescription
import io.redlink.more.app.android.shared_composables.HeaderTitle
import io.redlink.more.app.android.shared_composables.IconInline
import io.redlink.more.app.android.shared_composables.MoreDivider
import io.redlink.more.app.android.ui.theme.MoreColors

@Composable
fun NotificationFilterView(viewModel: NotificationFilterViewModel) {
    LazyColumn {
        item {
            HeaderTitle(
                title = getString(R.string.more_select_filter),
                modifier = Modifier.padding(top = 20.dp)
            )
            MoreDivider(modifier = Modifier.padding(vertical = 10.dp))
        }

        itemsIndexed(viewModel.currentFilters.entries.sortedBy { it.key.sortIndex }) { _, entry ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if(entry.value)
                    IconInline(
                        icon = Icons.Rounded.Done,
                        color = MoreColors.Approved,
                        contentDescription = getStringResource(id = R.string.more_filter_selected)
                    )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .clickable(onClick = { viewModel.toggleFilter(entry.key) })
                        .padding(4.dp)
                ) {
                    HeaderDescription(
                        description = entry.key.type,
                        color = MoreColors.Secondary
                    )
                }
            }
            MoreDivider(modifier = Modifier.padding(vertical = 10.dp))
        }
    }

}