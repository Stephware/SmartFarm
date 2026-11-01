package com.smartfarm.mobile.features.farms

import android.annotation.SuppressLint
import android.location.Geocoder
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

private data class PlaceResult(val name: String, val latitude: Double, val longitude: Double)

private const val MAP_HTML = """
<!doctype html><html lang="en"><head><meta name="viewport" content="width=device-width,initial-scale=1">
<link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" integrity="sha256-p4NxAoJBhIIN+hmNHrzRCf9tD/miZyoHS5obTRR9BMY=" crossorigin="">
<style>html,body,#map{height:100%;width:100%;margin:0}body{background:#e5eee9;font-family:sans-serif}
#fallback{position:absolute;top:15px;left:15px;z-index:1000;background:white;padding:6px;font-size:12px}</style>
</head><body><div id="map"></div><div id="fallback">Loading map...</div>
<script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js" integrity="sha256-20nQCchB9co0qIjJZRGuk2/Z9VM+kNiyxNV1lvTlZBo=" crossorigin=""></script><script>
if (window.L) {
  document.getElementById("fallback").remove();
  const map=L.map("map",{scrollWheelZoom:false}).setView([15.3,121],6);
  L.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png",{
    maxZoom:19,attribution:'&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap contributors</a>'
  }).addTo(map);
  let marker=null;
  window.selectFarmPin=function(lat,lon){
    if (!Number.isFinite(lat)||!Number.isFinite(lon)||Math.abs(lat)>90||Math.abs(lon)>180) return;
    if(marker)marker.setLatLng([lat,lon]);
    else {
      marker=L.marker([lat,lon],{draggable:true}).addTo(map);
      marker.on("dragend",()=>{
        const p=marker.getLatLng();
        window.AniGoPin.pick(p.lat.toFixed(6),p.lng.toFixed(6));
      });
    }
    map.setView([lat,lon],16);
  };
  map.on("click",p=>window.AniGoPin.pick(p.latlng.lat.toFixed(6),p.latlng.lng.toFixed(6)));
}
</script></body></html>
"""

private class LocationBridge(private val update: (Double, Double) -> Unit) {
    @JavascriptInterface
    fun pick(latitude: String, longitude: String) {
        val lat = latitude.toDoubleOrNull() ?: return
        val lon = longitude.toDoubleOrNull() ?: return
        if (!lat.isFinite() || !lon.isFinite() || lat !in -90.0..90.0 || lon !in -180.0..180.0) return
        Handler(Looper.getMainLooper()).post { update(lat, lon) }
    }
}

@Composable
fun FarmLocationPicker(
    location: String,
    latitude: String,
    longitude: String,
    enabled: Boolean,
    onSelected: (String, String, String) -> Unit
) {
    var opened by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { opened = true }, enabled = enabled) {
        Text(if (latitude.isNotBlank() && longitude.isNotBlank()) "Adjust farm pin"
             else "Find farm on map")
    }
    Text(
        if (latitude.isNotBlank() && longitude.isNotBlank()) "Farm pin selected for weather forecasts."
        else "Search a place or tap a map pin to enable local weather forecasts.",
        style = MaterialTheme.typography.bodySmall
    )
    if (opened) {
        FarmLocationDialog(location, latitude, longitude,
            onDismiss = { opened = false },
            onConfirm = { name, lat, lon ->
                onSelected(name, lat, lon)
                opened = false
            })
    }
}

@Suppress("DEPRECATION")
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun FarmLocationDialog(
    initialName: String,
    initialLat: String,
    initialLon: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(initialName) }
    var query by remember { mutableStateOf(initialName) }
    var lat by remember { mutableStateOf(initialLat.toDoubleOrNull()) }
    var lon by remember { mutableStateOf(initialLon.toDoubleOrNull()) }
    var results by remember { mutableStateOf(emptyList<PlaceResult>()) }
    var searching by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("Tap the map to place a pin, or search an address.") }
    var webView by remember { mutableStateOf<WebView?>(null) }

    fun select(latitude: Double, longitude: Double) {
        lat = latitude
        lon = longitude
        val a = String.format(Locale.US, "%.6f", latitude)
        val b = String.format(Locale.US, "%.6f", longitude)
        webView?.evaluateJavascript("window.selectFarmPin && window.selectFarmPin($a,$b)", null)
        message = "Pin selected. Confirm the farm's address or nearby area."
    }

    DisposableEffect(Unit) {
        onDispose {
            webView?.removeJavascriptInterface("AniGoPin")
            webView?.destroy()
            webView = null
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large, modifier = Modifier.widthIn(max = 540.dp)) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("Choose farm location", style = MaterialTheme.typography.titleLarge)
                Text("Search a barangay or nearby place, then move the pin to the farm.",
                    style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(query, { query = it }, label = { Text("Search address or place") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        val text = query.trim()
                        if (text.length !in 3..120) {
                            message = "Enter at least 3 characters."
                        } else if (!Geocoder.isPresent()) {
                            message = "Address search is unavailable on this device. Tap the map instead."
                        } else {
                            searching = true
                            results = emptyList()
                            scope.launch {
                                try {
                                    val found = withContext(Dispatchers.IO) {
                                        Geocoder(context, Locale.getDefault())
                                            .getFromLocationName(text, 5).orEmpty()
                                            .filter { it.hasLatitude() && it.hasLongitude() }
                                            .map {
                                                PlaceResult(
                                                    (it.getAddressLine(0) ?: text).take(250),
                                                    it.latitude, it.longitude)
                                            }
                                    }
                                    results = found
                                    message = if (found.isEmpty()) "No results. Tap the map instead."
                                              else "Choose a place below, then adjust the pin."
                                } catch (cancelled: CancellationException) {
                                    throw cancelled
                                } catch (_: Exception) {
                                    message = "Search unavailable. Tap the map instead."
                                } finally {
                                    searching = false
                                }
                            }
                        }
                    }, enabled = !searching) { Text("Search") }
                    if (searching) CircularProgressIndicator()
                }
                results.take(3).forEach { result ->
                    OutlinedButton(onClick = {
                        name = result.name
                        select(result.latitude, result.longitude)
                        results = emptyList()
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text(result.name, maxLines = 2)
                    }
                }
                AndroidView(
                    factory = {
                        WebView(context).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = false
                            settings.allowFileAccess = false
                            settings.allowContentAccess = false
                            settings.setSupportMultipleWindows(false)
                            settings.userAgentString =
                                "AniGoSmartFarm/1.0 (+https://github.com/Stephware/SmartFarm)"
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    if (lat != null && lon != null) {
                                        val a = String.format(Locale.US, "%.6f", lat)
                                        val b = String.format(Locale.US, "%.6f", lon)
                                        view?.evaluateJavascript(
                                            "window.selectFarmPin && window.selectFarmPin($a,$b)", null)
                                    }
                                }
                                override fun shouldOverrideUrlLoading(
                                    view: WebView, request: android.webkit.WebResourceRequest
                                ): Boolean = true
                            }
                            addJavascriptInterface(LocationBridge { a, b ->
                                select(a, b)
                            }, "AniGoPin")
                            loadDataWithBaseURL("https://github.com/Stephware/SmartFarm/",
                                MAP_HTML, "text/html", "UTF-8", null)
                            webView = this
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(250.dp)
                )
                Text(message, style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(name, { name = it }, label = { Text("Farm address or area") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss) { Text("Cancel") }
                    Button(onClick = {
                        onConfirm(
                            name.trim(), String.format(Locale.US, "%.6f", requireNotNull(lat)),
                            String.format(Locale.US, "%.6f", requireNotNull(lon)))
                    }, enabled = name.trim().length in 2..250 && lat != null && lon != null) {
                        Text("Use this location")
                    }
                }
                Text("Map: © OpenStreetMap contributors. Device address search availability varies.",
                    style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
