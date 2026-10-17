package com.smartfarm.mobile

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.smartfarm.mobile.data.local.SmartFarmDatabase
import com.smartfarm.mobile.data.local.entity.LocalMetadata
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class FoundationTest {
    @Test fun workManagerInitializesWithoutSchedulingWork() {
        val application = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
            as SmartFarmApplication
        assertNotNull(application.container.workManager)
    }

    @Test fun metadataRoundTripsThroughRoom() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, SmartFarmDatabase::class.java).build()
        try {
            database.metadataDao().write(LocalMetadata("foundation", "ready"))
            assertEquals("ready", database.metadataDao().read("foundation"))
        } finally {
            database.close()
        }
    }
}
