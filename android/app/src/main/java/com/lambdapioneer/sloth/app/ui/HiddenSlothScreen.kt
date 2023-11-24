package com.lambdapioneer.sloth.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lambdapioneer.sloth.app.models.HiddenSlothViewModel
import com.lambdapioneer.sloth.app.ui.theme.ErrorRed
import com.lambdapioneer.sloth.app.ui.theme.SlothTheme
import com.lambdapioneer.sloth.app.ui.theme.Teal

@Composable
internal fun HiddenSlothScreen(model: HiddenSlothViewModel) {
    var password by remember { mutableStateOf("test") }
    var content by remember { mutableStateOf("Demo Content") }
    val output = model.output.collectAsState().value
    val busy = model.busy.collectAsState().value
    val error = model.error.collectAsState().value

    HiddenSlothScreenContent(
        password = password,
        content = content,
        output = output,
        busy = busy,
        error = error,
        onPasswordChange = { password = it },
        onContentChange = { content = it },
        onEnsure = { model.ensure() },
        onRatchet = { model.ratchet() },
        onStore = { model.store(password, content) },
        onLoad = { model.load(password) }
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun HiddenSlothScreenContent(
    password: String,
    content: String,
    output: String?,
    busy: Boolean,
    error: String?,
    onPasswordChange: (String) -> Unit = { },
    onContentChange: (String) -> Unit = { },
    onEnsure: () -> Unit = { },
    onRatchet: () -> Unit = { },
    onStore: () -> Unit = { },
    onLoad: () -> Unit = { },
) {
    SlothTheme {
        Column {
            TopAppBar(
                title = { Text("\uD83E\uDDA5 HiddenSloth") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Teal,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                )
            )
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                HiddenSlothMainContent(
                    password = password,
                    content = content,
                    output = output,
                    busy = busy,
                    error = error,
                    onPasswordChange = onPasswordChange,
                    onContentChange = onContentChange,
                    onEnsure = onEnsure,
                    onRatchet = onRatchet,
                    onStore = onStore,
                    onLoad = onLoad
                )
            }
        }
    }
}

@Composable
fun HiddenSlothMainContent(
    password: String,
    content: String,
    output: String?,
    busy: Boolean,
    error: String?,
    onPasswordChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onEnsure: () -> Unit,
    onRatchet: () -> Unit,
    onStore: () -> Unit,
    onLoad: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = "Simple demo for storing and retrieving data with HiddenSloth. In this demo ensureStorage() is not called automatically",
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
        ) {
            Button(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                enabled = !busy,
                onClick = { onEnsure() }
            ) {
                Text("Ensure storage")
            }
            Button(
                modifier = Modifier.weight(1f),
                enabled = !busy,
                onClick = { onRatchet() }
            ) {
                Text("Ratchet storage")
            }
        }

        TextField(
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Password") },
            value = password,
            onValueChange = onPasswordChange,
        )

        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            label = { Text("Content") },
            value = content,
            onValueChange = onContentChange,
        )

        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
        ) {
            Button(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                enabled = !busy,
                onClick = { onStore() }
            ) {
                Text("Store content")
            }
            Button(
                modifier = Modifier.weight(1f),
                enabled = !busy,
                onClick = { onLoad() }
            ) {
                Text("Load content")
            }
        }

        if (error != null) {
            Text(
                text = error,
                fontFamily = FontFamily.Monospace,
                color = ErrorRed,
                modifier = Modifier.padding(top = 8.dp),
            )
        } else {
            Text(
                text = "Retrieved content:",
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = output ?: "<empty>",
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        if (busy) {
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(top = 32.dp)
                    .align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_2)
@Composable
fun HiddenSlothScreenPreview_empty() {
    HiddenSlothScreenContent(
        password = "",
        content = "",
        output = null,
        busy = false,
        error = null,
    )
}

@Preview(showBackground = true, device = Devices.PIXEL_2)
@Composable
fun HiddenSlothScreenPreview_withEntriesBusy() {
    HiddenSlothScreenContent(
        password = "test",
        content = "Demo Content",
        output = "Demo Content",
        busy = true,
        error = null,
    )
}

@Preview(showBackground = true, device = Devices.PIXEL_2)
@Composable
fun HiddenSlothScreenPreview_error() {
    HiddenSlothScreenContent(
        password = "test",
        content = "Demo Content",
        output = "Demo Content",
        busy = false,
        error = "Something went wrong...",
    )
}
