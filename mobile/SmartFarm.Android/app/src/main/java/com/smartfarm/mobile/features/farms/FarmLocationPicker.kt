package com.smartfarm.mobile.features.farms

import com.smartfarm.mobile.R
import com.smartfarm.mobile.localization.uiText

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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
        Text(if (latitude.isNotBlank() && longitude.isNotBlank()) uiText(R.string.ui_adjust_farm_pin_2515999, "Adjust farm pin")
             else uiText(R.string.ui_find_farm_on_map_e84ca4f, "Find farm on map"))
    }
    Text(
        if (latitude.isNotBlank() && longitude.isNotBlank()) uiText(R.string.ui_farm_pin_selected_for_weather_forecasts_306eac8, "Farm pin selected for weather forecasts.")
        else uiText(R.string.ui_search_a_place_or_tap_a_map_pin_to_enable_local_weat_8b4e935, "Search a place or tap a map pin to enable local weather forecasts."),
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
    var message by remember { mutableStateOf(uiText(R.string.ui_tap_the_map_to_place_a_pin_or_search_an_address_42ee28e, "Tap the map to place a pin, or search an address.")) }
    var webView by remember { mutableStateOf<WebView?>(null) }

    fun select(latitude: Double, longitude: Double, selectedName: String? = null,
        resolveAddress: Boolean = false) {
        lat = latitude
        lon = longitude
        val a = String.format(Locale.US, "%.6f", latitude)
        val b = String.format(Locale.US, "%.6f", longitude)
        // Coordinates are usable even if the device geocoder has no address.
        // Never require a second manual entry after choosing the current position.
        name = selectedName?.takeIf { it.isNotBlank() }
            ?: uiText(R.string.gps_near_coordinates, "Near coordinates: %1\$s, %2\$s", a, b)
        webView?.evaluateJavascript("window.selectFarmPin && window.selectFarmPin($a,$b)", null)
        message = uiText(R.string.ui_pin_selected_confirm_the_farm_s_address_or_nearby_ar_99e5dca,
            "Pin selected. Confirm the farm's address or nearby area.")
        if (resolveAddress && Geocoder.isPresent()) {
            scope.launch {
                try {
                    val address = withContext(Dispatchers.IO) {
                        Geocoder(context, Locale.getDefault())
                            .getFromLocation(latitude, longitude, 1).orEmpty()
                            .firstOrNull()?.getAddressLine(0)?.take(250)
                    }
                    if (lat == latitude && lon == longitude && !address.isNullOrBlank())
                        name = address
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    // Keep the exact selected coordinates and honest fallback label.
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun useDeviceLocation() {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) {
            message = uiText(R.string.ui_location_permission_is_needed_you_can_still_search_o_67fe3be, "Location permission is needed. You can still search or tap the map.")
            return
        }
        try {
            val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val provider = when {
                coarse && manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ->
                    LocationManager.NETWORK_PROVIDER
                fine && manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ->
                    LocationManager.GPS_PROVIDER
                else -> null
            }
            if (provider == null) {
                message = uiText(R.string.ui_turn_on_device_location_or_select_your_farm_on_the_m_9b4f562, "Turn on device location, or select your farm on the map.")
                return
            }
            message = uiText(R.string.ui_getting_your_approximate_position_8c33aae, "Getting your approximate position...")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                manager.getCurrentLocation(provider, null, context.mainExecutor) { found ->
                    if (found != null) select(found.latitude, found.longitude, resolveAddress = true)
                    else message = uiText(R.string.ui_location_unavailable_search_or_tap_the_map_instead_05d7442, "Location unavailable. Search or tap the map instead.")
                }
            } else {
                val found = manager.getLastKnownLocation(provider)
                if (found != null && System.currentTimeMillis() - found.time in 0..300000) {
                    select(found.latitude, found.longitude, resolveAddress = true)
                } else {
                    message = uiText(R.string.ui_no_recent_device_location_search_or_tap_the_map_inst_153b918, "No recent device location. Search or tap the map instead.")
                }
            }
        } catch (_: SecurityException) {
            message = uiText(R.string.ui_location_permission_unavailable_search_or_tap_the_ma_a6b7955, "Location permission unavailable. Search or tap the map.")
        } catch (_: IllegalArgumentException) {
            message = uiText(R.string.ui_location_service_unavailable_search_or_tap_the_map_8f86ce0, "Location service unavailable. Search or tap the map.")
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.any { it }) useDeviceLocation()
        else message = uiText(R.string.ui_location_access_denied_search_or_tap_the_map_instead_119bd10, "Location access denied. Search or tap the map instead.")
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
                Text(uiText(R.string.ui_choose_farm_location_d248443, "Choose farm location"), style = MaterialTheme.typography.titleLarge)
                Text(uiText(R.string.ui_search_a_barangay_or_nearby_place_then_move_the_pin__26f2210, "Search a barangay or nearby place, then move the pin to the farm."),
                    style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(query, { query = it }, label = { Text(uiText(R.string.ui_search_address_or_place_49925f2, "Search address or place")) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        val fine = ContextCompat.checkSelfPermission(context,
                            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        val coarse = ContextCompat.checkSelfPermission(context,
                            Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        if (fine || coarse) useDeviceLocation()
                        else locationPermissionLauncher.launch(arrayOf(
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION))
                    }) { Text(uiText(R.string.ui_use_my_location_30cbc33, "Use my location")) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        val text = query.trim()
                        if (text.length !in 3..120) {
                            message = uiText(R.string.ui_enter_at_least_3_characters_57c9e61, "Enter at least 3 characters.")
                        } else if (!Geocoder.isPresent()) {
                            message = uiText(R.string.ui_address_search_is_unavailable_on_this_device_tap_the_ceaafe6, "Address search is unavailable on this device. Tap the map instead.")
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
                                    message = if (found.isEmpty()) uiText(R.string.ui_no_results_tap_the_map_instead_a823be3, "No results. Tap the map instead.")
                                              else uiText(R.string.ui_choose_a_place_below_then_adjust_the_pin_c63c0f1, "Choose a place below, then adjust the pin.")
                                } catch (cancelled: CancellationException) {
                                    throw cancelled
                                } catch (_: Exception) {
                                    message = uiText(R.string.ui_search_unavailable_tap_the_map_instead_719611f, "Search unavailable. Tap the map instead.")
                                } finally {
                                    searching = false
                                }
                            }
                        }
                    }, enabled = !searching) { Text(uiText(R.string.ui_search_49c266b, "Search")) }
                    if (searching) CircularProgressIndicator()
                }
                results.take(3).forEach { result ->
                    OutlinedButton(onClick = {
                        select(result.latitude, result.longitude, selectedName = result.name)
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
                OutlinedTextField(name, { name = it }, label = { Text(uiText(R.string.ui_farm_address_or_area_a673ac8, "Farm address or area")) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss) { Text(uiText(R.string.ui_cancel_19766ed, "Cancel")) }
                    Button(onClick = {
                        onConfirm(
                            name.trim(), String.format(Locale.US, "%.6f", requireNotNull(lat)),
                            String.format(Locale.US, "%.6f", requireNotNull(lon)))
                    }, enabled = name.trim().length in 2..250 && lat != null && lon != null) {
                        Text(uiText(R.string.ui_use_this_location_69d62e1, "Use this location"))
                    }
                }
                Text(uiText(R.string.ui_map_openstreetmap_contributors_device_address_search_43a985a, "Map: © OpenStreetMap contributors. Device address search availability varies."),
                    style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
