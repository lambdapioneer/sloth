package com.lambdapioneer.sloth.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

@Composable
fun SlothTheme(
    content: @Composable() () -> Unit,
) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Teal,
            secondary = LightTeal,
            tertiary = AlternativeTeal
        ),
        content = content
    )
}
