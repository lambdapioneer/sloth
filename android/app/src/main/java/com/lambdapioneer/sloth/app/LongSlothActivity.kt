package com.lambdapioneer.sloth.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.ui.graphics.toArgb
import com.lambdapioneer.sloth.app.models.LongSlothViewModel
import com.lambdapioneer.sloth.app.ui.LongSlothScreen
import com.lambdapioneer.sloth.app.ui.theme.Teal

class LongSlothActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Teal.toArgb()

        val model: LongSlothViewModel by viewModels { LongSlothViewModel.Factory }
        setContent { LongSlothScreen(model) }
    }
}
