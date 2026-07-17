package com.bhoomibot.os

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.bhoomibot.os.navigation.AppNavigation
import com.bhoomibot.os.ui.theme.BhoomiBotTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BhoomiBotTheme { AppNavigation() } }
    }
}
