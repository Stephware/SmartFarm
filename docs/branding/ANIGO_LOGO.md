# AniGo logo usage

This is the approved AniGo logo treatment for the MVC web frontend, Super Admin, and Kotlin Android app. The artwork is a production vector trace of the selected pine-and-gold image, preserving the angular A, gold sun, curved field lines with the **second field strand in gold**, and the rounded AniGo wordmark.

## Approved colors
- Pine green: `#134D30`
- Harvest gold: `#CBA14E`
- Cream icon background: `#F9F8F4`
- Dark surface wordmark/mark: cream `#FFFDF6` replaces pine green only; gold is unchanged.

## Asset rules
- `src/SmartFarm.Web/wwwroot/images/branding/anigo-logo.svg` – canonical horizontal SVG for light surfaces.
- `src/SmartFarm.Web/wwwroot/images/branding/anigo-logo-on-dark.svg` – same geometry with cream instead of green for dark surfaces.
- `src/SmartFarm.Web/wwwroot/images/branding/anigo-mark.svg` – icon-only A; usable as a favicon.
- `src/SmartFarm.Web/Views/Shared/_AniGoBrandStyles.cshtml` – shared web sizing and dark-mode switching, inlined without another stylesheet request.
- `mobile/SmartFarm.Android/app/src/main/res/drawable/ani_go_logo*.xml` – Android shared-header full wordmark.
- `mobile/SmartFarm.Android/app/src/main/res/drawable/ani_go_mark.xml` – Android compact header mark.
- `mobile/SmartFarm.Android/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` – Android adaptive app icon using the A mark.

## Implementation
The web shared layouts and landing footer display the SVG assets rather than separate leaf icons and typed names. Login and registration repeat the logo within their panels. The common native Android app bar supplies branding across welcome, authentication, and signed-in screens. The Android app label remains **AniGo**.

Do not alter the symbol geometry, text, or color distribution in future UI changes; scale it proportionally. On a pine-green sidebar use the cream/gold variant for contrast. Preserve accessible labeling on logo links and never duplicate text next to the wordmark image.
