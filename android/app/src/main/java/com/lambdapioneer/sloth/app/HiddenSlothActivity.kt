package com.lambdapioneer.sloth.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.ui.graphics.toArgb
import com.lambdapioneer.sloth.app.models.HiddenSlothViewModel
import com.lambdapioneer.sloth.app.ui.HiddenSlothScreen
import com.lambdapioneer.sloth.app.ui.theme.Teal

class HiddenSlothActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Teal.toArgb()

        val model: HiddenSlothViewModel by viewModels { HiddenSlothViewModel.Factory }
        setContent { HiddenSlothScreen(model) }
    }
}
