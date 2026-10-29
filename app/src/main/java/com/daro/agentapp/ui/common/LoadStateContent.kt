package com.daro.agentapp.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.daro.agentapp.R
import com.daro.agentapp.ui.state.LoadState

/** Retry is optional: callers must decide whether repeating an operation is safe. */
@Composable
fun <T> LoadStateContent(
    state: LoadState<T>,
    modifier: Modifier = Modifier,
    emptyMessage: String = stringResource(R.string.empty_content),
    onRetry: (() -> Unit)? = null,
    content: @Composable (T) -> Unit,
) {
    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when (state) {
            LoadState.Loading -> CircularProgressIndicator()
            LoadState.Empty -> Text(emptyMessage)
            is LoadState.Content -> content(state.value)
            is LoadState.Error -> {
                ErrorMessage(state.error)
                if (onRetry != null) Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
            }
        }
    }
}
