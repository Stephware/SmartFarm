package com.smartfarm.mobile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun RecordPage(title: String? = null, onBack: (() -> Unit)? = null,
    backLabel: String = "Back", content: LazyListScope.() -> Unit) {
    androidx.compose.runtime.CompositionLocalProvider(LocalPageNavigation provides
        (LocalPageNavigation.current ?: title?.let { PageNavigation(it, backLabel, onBack) })) {
    Scaffold(topBar = { FarmAppBar(title = title, onBack = onBack, backLabel = backLabel) }) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding(), contentAlignment = Alignment.TopCenter) {
            val gutter = if (maxWidth < 600.dp) 16.dp else 28.dp
            LazyColumn(Modifier.widthIn(max = 840.dp).fillMaxWidth().testTag("record-list"),
                contentPadding = PaddingValues(horizontal = gutter, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
        }
    }
    }
}
