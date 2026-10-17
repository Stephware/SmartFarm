package com.smartfarm.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.smartfarm.mobile.features.auth.AuthViewModel
import com.smartfarm.mobile.features.farms.FarmViewModel
import com.smartfarm.mobile.navigation.SmartFarmNavHost
import com.smartfarm.mobile.ui.theme.SmartFarmTheme

class MainActivity : ComponentActivity() {
    private val authentication: AuthViewModel by viewModels {
        AuthViewModel.Factory((application as SmartFarmApplication).container.authRepository)
    }

    private val farms: FarmViewModel by viewModels {
        FarmViewModel.Factory((application as SmartFarmApplication).container.farmRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { SmartFarmTheme { SmartFarmNavHost(authentication, farms) } }
    }
}


