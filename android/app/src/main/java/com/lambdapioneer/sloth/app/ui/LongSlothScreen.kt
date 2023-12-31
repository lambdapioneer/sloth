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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lambdapioneer.sloth.app.models.LongSlothViewModel
import com.lambdapioneer.sloth.app.ui.theme.ErrorRed
import com.lambdapioneer.sloth.app.ui.theme.SlothTheme
import com.lambdapioneer.sloth.app.ui.theme.Teal

@Composable
internal fun LongSlothScreen(model: LongSlothViewModel) {
    var password by remember { mutableStateOf("test") }
    val key = model.key.collectAsState().value
    val busy = model.busy.collectAsState().value
    val error = model.error.collectAsState().value
    val context = LocalContext.current

    LongSlothScreenContent(
        password = password,
        key = key,
        busy = busy,
        error = error,
        onRunBenchmark = { model.runBenchmark(context) },
        onPasswordChange = { password = it },
        onGenerateKey = { model.generateKey(it) },
        onReDeriveKey = { model.deriveKey(it) },
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun LongSlothScreenContent(
    password: String,
    key: ByteArray?,
    busy: Boolean = false,
    error: String? = null,
    onRunBenchmark: () -> Unit = {},
    onPasswordChange: (String) -> Unit = {},
    onGenerateKey: (String) -> Unit = {},
    onReDeriveKey: (String) -> Unit = {},
) {
    SlothTheme {
        Column {
            TopAppBar(
                title = { Text("\uD83E\uDDA5 LongSloth") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Teal,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                )
            )
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                LongSlothMainContent(
                    password = password,
                    key = key,
                    busy = busy,
                    error = error,
                    onRunBenchmark = onRunBenchmark,
                    onPasswordChange = onPasswordChange,
                    onGenerateKey = onGenerateKey,
                    onReDeriveKey = onReDeriveKey
                )
            }
        }
    }
}

@Composable
fun LongSlothMainContent(
    password: String,
    key: ByteArray?,
    busy: Boolean = false,
    error: String?,
    onRunBenchmark: () -> Unit,
    onPasswordChange: (String) -> Unit,
    onGenerateKey: (String) -> Unit,
    onReDeriveKey: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = "Simple demo for creating new LongSloth keys and re-deriving them afterwards.",
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            enabled = !busy,
            onClick = { onRunBenchmark() }
        ) {
            Text("Run benchmark (target duration 1 second)")
        }

        TextField(
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Password") },
            value = password,
            onValueChange = onPasswordChange,
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
                onClick = { onGenerateKey(password) }
            ) {
                Text("Create new key")
            }
            Button(
                modifier = Modifier.weight(1f),
                enabled = !busy,
                onClick = { onReDeriveKey(password) }
            ) {
                Text("Re-derive key")
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
                text = "Derived key:",
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = key?.encodeAsHex() ?: "<empty>",
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

private fun ByteArray.encodeAsHex(): String {
    return this.joinToString(",") { String.format("%02x", it) }
}

@Preview(showBackground = true, device = Devices.PIXEL_2)
@Composable
fun LongSlothScreenPreview_empty() {
    LongSlothScreenContent("", null)
}

@Preview(showBackground = true, device = Devices.PIXEL_2)
@Composable
fun LongSlothScreenPreview_withEntriesBusy() {
    LongSlothScreenContent("p4ssw0rd$", byteArrayOf(0x42, 0x13, 0x37), busy = true)
}

@Preview(showBackground = true, device = Devices.PIXEL_2)
@Composable
fun LongSlothScreenPreview_error() {
    LongSlothScreenContent("something", null, error = "Something went wrong")
}
