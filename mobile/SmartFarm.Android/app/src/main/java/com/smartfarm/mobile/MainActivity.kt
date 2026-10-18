package com.smartfarm.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.smartfarm.mobile.features.auth.AuthViewModel
import com.smartfarm.mobile.features.status.StatusViewModel
import com.smartfarm.mobile.features.farms.FarmViewModel
import com.smartfarm.mobile.features.crops.CropViewModel
import com.smartfarm.mobile.features.inventory.InventoryViewModel
import com.smartfarm.mobile.features.harvests.HarvestViewModel
import com.smartfarm.mobile.features.monitoring.MonitoringViewModel
import com.smartfarm.mobile.navigation.SmartFarmNavHost
import com.smartfarm.mobile.ui.theme.SmartFarmTheme

class MainActivity : ComponentActivity() {
    private val authentication: AuthViewModel by viewModels {
        AuthViewModel.Factory((application as SmartFarmApplication).container.authRepository)
    }

    private val farms: FarmViewModel by viewModels {
        FarmViewModel.Factory((application as SmartFarmApplication).container.farmRepository)
    }

    private val crops: CropViewModel by viewModels {
        val container = (application as SmartFarmApplication).container
        CropViewModel.Factory(container.cropRepository, container.farmRepository)
    }

    private val monitoring: MonitoringViewModel by viewModels {
        MonitoringViewModel.Factory((application as SmartFarmApplication).container.monitoringRepository)
    }

    private val inventory: InventoryViewModel by viewModels {
        InventoryViewModel.Factory((application as SmartFarmApplication).container.inventoryRepository)
    }

    private val harvests: HarvestViewModel by viewModels {
        HarvestViewModel.Factory((application as SmartFarmApplication).container.harvestRepository)
    }

    private val status: StatusViewModel by viewModels {
        StatusViewModel.Factory((application as SmartFarmApplication).container.statusRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { SmartFarmTheme { SmartFarmNavHost(authentication, farms, crops, monitoring, inventory, harvests, status) } }
    }
}






