package com.smartfarm.mobile

import com.smartfarm.mobile.ui.components.CropArtworkKind
import com.smartfarm.mobile.ui.components.cropArtworkKind
import org.junit.Assert.assertEquals
import org.junit.Test

class CropArtworkTest {
    @Test fun mapsRecognizedCropsWithoutGuessingUnknownVarieties() {
        assertEquals(CropArtworkKind.Tomato, cropArtworkKind("Tomato"))
        assertEquals(CropArtworkKind.Tomato, cropArtworkKind("Kamatis"))
        assertEquals(CropArtworkKind.Eggplant, cropArtworkKind("Japanese Eggplant"))
        assertEquals(CropArtworkKind.Eggplant, cropArtworkKind("Talong"))
        assertEquals(CropArtworkKind.Rice, cropArtworkKind("Palay"))
        assertEquals(CropArtworkKind.Corn, cropArtworkKind("Sweet corn"))
        assertEquals(CropArtworkKind.Generic, cropArtworkKind("Okra"))
        assertEquals(CropArtworkKind.Generic, cropArtworkKind(null))
    }
}
