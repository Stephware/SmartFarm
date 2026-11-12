package com.smartfarm.mobile

import com.smartfarm.mobile.features.farms.FarmSetupNext
import com.smartfarm.mobile.features.farms.nextFarmSetupStep
import org.junit.Assert.assertEquals
import org.junit.Test

class FarmSetupGuideTest {
    @Test fun guidesNewOwnersWithoutInventingCropState() {
        assertEquals(FarmSetupNext.AddPlot, nextFarmSetupStep(false, false, false))
        assertEquals(FarmSetupNext.AddPlot, nextFarmSetupStep(false, true, false))
        assertEquals(FarmSetupNext.Loading, nextFarmSetupStep(true, false, false))
        assertEquals(FarmSetupNext.PlanOrStartCrop, nextFarmSetupStep(true, true, false))
        assertEquals(FarmSetupNext.Complete, nextFarmSetupStep(true, true, true))
    }
}
