(() => {
  "use strict";

  for (const root of document.querySelectorAll("[data-farm-location-picker]")) {
    const form = root.closest("form");
    const locationField = form.querySelector("[data-farm-location-label]");
    const latitudeField = form.querySelector("[data-farm-latitude]");
    const longitudeField = form.querySelector("[data-farm-longitude]");
    const queryField = root.querySelector("[data-farm-search]");
    const searchButton = root.querySelector("[data-farm-search-button]");
    const results = root.querySelector("[data-farm-results]");
    const status = root.querySelector("[data-farm-location-status]");
    const clearButton = root.querySelector("[data-farm-clear]");
    const gpsButton = root.querySelector("[data-farm-gps]");
    const mapEl = root.querySelector("[data-farm-map]");
    if (!locationField || !latitudeField || !longitudeField || !mapEl) continue;

    const setStatus = message => { status.textContent = message; };
    const initialLat = Number(latitudeField.value);
    const initialLon = Number(longitudeField.value);
    const initial = latitudeField.value !== "" && longitudeField.value !== "" &&
      Number.isFinite(initialLat) && Number.isFinite(initialLon) &&
      Math.abs(initialLat) <= 90 && Math.abs(initialLon) <= 180;

    if (!window.L) {
      setStatus("Map could not load. Enter the farm location as text and try again later.");
      continue;
    }
    const map = L.map(mapEl, { scrollWheelZoom: false }).setView(
      initial ? [initialLat, initialLon] : [15.3, 121], initial ? 15 : 6);
    L.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png", {
      maxZoom: 19,
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap contributors</a>'
    }).addTo(map);
    let pin = null;
    const showPoint = (latitude, longitude, zoom = 16) => {
      const lat = Number(latitude), lon = Number(longitude);
      if (!Number.isFinite(lat) || !Number.isFinite(lon) || Math.abs(lat) > 90 || Math.abs(lon) > 180) return;
      latitudeField.value = lat.toFixed(6);
      longitudeField.value = lon.toFixed(6);
      if (pin) pin.setLatLng([lat, lon]);
      else {
        pin = L.marker([lat, lon], { draggable: true }).addTo(map);
        pin.on("dragend", () => {
          const moved = pin.getLatLng();
          showPoint(moved.lat, moved.lng, null);
        });
      }
      if (zoom !== null) map.setView([lat, lon], zoom);
      setStatus("Farm pin saved. Drag it or tap the map to adjust. An address or area name is also required.");
    };
    if (initial) showPoint(initialLat, initialLon, 15);
    map.on("click", e => showPoint(e.latlng.lat, e.latlng.lng, null));

    // A changed description must not silently keep an unrelated previous pin.
    locationField.addEventListener("input", () => {
      latitudeField.value = "";
      longitudeField.value = "";
      if (pin) { map.removeLayer(pin); pin = null; }
      setStatus("Tap the map to select the exact farm location.");
    });
    clearButton.addEventListener("click", () => {
      latitudeField.value = "";
      longitudeField.value = "";
      if (pin) { map.removeLayer(pin); pin = null; }
      setStatus("Pin removed. Weather needs a map pin; the location name can still be saved.");
    });

    const search = async () => {
      const query = queryField.value.trim();
      results.replaceChildren();
      if (query.length < 3 || query.length > 120) {
        setStatus("Type at least 3 characters to search for a place.");
        return;
      }
      searchButton.disabled = true;
      setStatus("Searching locations...");
      try {
        const response = await fetch(root.dataset.searchUrl + "?query=" + encodeURIComponent(query), {
          credentials: "same-origin",
          headers: { "Accept": "application/json" }
        });
        if (!response.ok) throw new Error("Search unavailable");
        const places = await response.json();
        if (!Array.isArray(places) || places.length === 0) {
          setStatus("No matching places. Try a nearby barangay or select the farm on the map.");
          return;
        }
        for (const place of places) {
          const button = document.createElement("button");
          button.type = "button";
          button.textContent = place.name;
          button.addEventListener("click", () => {
            locationField.value = place.name;
            showPoint(place.latitude, place.longitude);
            results.replaceChildren();
          });
          results.append(button);
        }
        setStatus("Choose a result, then adjust the pin to your actual farm.");
      } catch {
        setStatus("Search is unavailable. You can enter the location name and tap the map instead.");
      } finally {
        searchButton.disabled = false;
      }
    };
    searchButton.addEventListener("click", search);
    queryField.addEventListener("keydown", e => {
      if (e.key === "Enter") { e.preventDefault(); search(); }
    });
    gpsButton.addEventListener("click", () => {
      if (!navigator.geolocation) { setStatus("Current location is unavailable on this device."); return; }
      setStatus("Finding your current location...");
      navigator.geolocation.getCurrentPosition(
        location => showPoint(location.coords.latitude, location.coords.longitude),
        () => setStatus("Could not get your location. Search or tap the map instead."),
        { enableHighAccuracy: true, timeout: 12000, maximumAge: 0 });
    });
    form.addEventListener("submit", e => {
      if (Boolean(latitudeField.value) !== Boolean(longitudeField.value)) {
        e.preventDefault();
        setStatus("Choose a complete map location or clear the pin.");
      }
    });
    // Leaflet needs a layout calculation after the form is shown.
    window.addEventListener("load", () => map.invalidateSize());
  }
})();
