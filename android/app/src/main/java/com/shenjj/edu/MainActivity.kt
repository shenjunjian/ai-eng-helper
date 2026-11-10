package com.shenjj.edu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.shenjj.edu.ui.nav.TalkFlowNavHost
import com.shenjj.edu.ui.theme.TalkFlowTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TalkFlowTheme {
                TalkFlowNavHost()
            }
        }
    }
}
