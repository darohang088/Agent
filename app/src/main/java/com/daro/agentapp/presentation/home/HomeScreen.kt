package com.daro.agentapp.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.daro.agentapp.R

@Composable
fun HomeScreen(onItemClick: (HomeMenuItem) -> Unit, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        TextButton(onClick = onBack, modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(stringResource(R.string.back))
        }
        Text(
            stringResource(R.string.home), style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().testTag("home_menu"),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(HomeMenuItem.entries, key = { it.name }) { item ->
                OutlinedButton(onClick = { onItemClick(item) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(item.titleRes))
                }
            }
        }
    }
}
