package com.elkrrai.techtalk.presentation.feed.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.elkrrai.techtalk.domain.model.tech.TechnologyInfo
import com.elkrrai.techtalk.presentation.feed.state.FeedFilter
import com.elkrrai.techtalk.presentation.feed.state.SeenStatus
import com.elkrrai.techtalk.presentation.feed.state.TopicFilterOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    filter: FeedFilter,
    subscribedTechnologies: List<TechnologyInfo>,
    availableTopics: List<TopicFilterOption>,
    onSeenStatusChanged: (SeenStatus) -> Unit,
    onTechnologyFilterChanged: (Long?) -> Unit,
    onTopicFilterToggled: (Long) -> Unit,
    onClearFilters: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(onDismissRequest = onDismiss, modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Status", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SeenStatus.entries.forEach { status ->
                    FilterChip(
                        selected = filter.seenStatus == status,
                        onClick = { onSeenStatusChanged(status) },
                        label = { Text(status.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            Text("Technology", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = filter.technologyId == null,
                    onClick = { onTechnologyFilterChanged(null) },
                    label = { Text("All") }
                )
                subscribedTechnologies.forEach { tech ->
                    FilterChip(
                        selected = filter.technologyId == tech.id,
                        onClick = { onTechnologyFilterChanged(tech.id) },
                        label = { Text(tech.name) }
                    )
                }
            }

            if (availableTopics.isNotEmpty()) {
                Text("Topic", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    availableTopics.forEach { topic ->
                        FilterChip(
                            selected = topic.id in filter.topicIds,
                            onClick = { onTopicFilterToggled(topic.id) },
                            label = { Text(topic.name) }
                        )
                    }
                }
            }

            TextButton(onClick = onClearFilters, modifier = Modifier.padding(top = 16.dp)) {
                Text("Clear filters")
            }
        }
    }
}
