package com.smartfarm.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.smartfarm.mobile.navigation.SmartFarmNavHost
import com.smartfarm.mobile.ui.theme.SmartFarmTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { SmartFarmTheme { SmartFarmNavHost() } }
    }
}
