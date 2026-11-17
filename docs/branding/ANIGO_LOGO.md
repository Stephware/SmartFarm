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
- `src/SmartFarm.Web/wwwroot/images/branding/anigo-mark.svg` – original pine/gold icon-only A, for light surfaces and the favicon.
- `src/SmartFarm.Web/wwwroot/images/branding/anigo-mark-on-dark.svg` – **Phase 1** icon-only A in cream/gold for charcoal, pine and other dark surfaces. Same paths/viewBox as the original.
- `src/SmartFarm.Web/Views/Shared/_AniGoBrandStyles.cshtml` – shared web sizing and dark-mode switching, inlined without another stylesheet request.
- `mobile/SmartFarm.Android/app/src/main/res/drawable/ani_go_logo*.xml` – Android shared-header full wordmark.
- `mobile/SmartFarm.Android/app/src/main/res/drawable/ani_go_mark.xml` – original pine/gold Android compact mark for cream/light backgrounds.
- `mobile/SmartFarm.Android/app/src/main/res/drawable/ani_go_mark_on_dark.xml` – **Phase 1** cream/gold Android compact mark for dark surfaces; matching path data, viewport and dimensions.
- `mobile/SmartFarm.Android/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` – Android adaptive app icon using the A mark.

## Implementation
The web shared layouts and landing footer display the SVG assets rather than separate leaf icons and typed names. Login and registration repeat the logo within their panels. The common native Android app bar supplies branding across welcome, authentication, and signed-in screens. The Android app label remains **AniGo**.

Do not alter the symbol geometry, text, or color distribution in future UI changes; scale it proportionally. On a pine-green sidebar use the cream/gold variant for contrast. Preserve accessible labeling on logo links and never duplicate text next to the wordmark image.

## Background-aware variants (Phase 1, assets only)

| Actual background beneath logo | Full wordmark | A-only mark |
| --- | --- | --- |
| White, cream, light card | `anigo-logo.svg` / `ani_go_logo.xml` | `anigo-mark.svg` / `ani_go_mark.xml` |
| Dark charcoal, pine sidebar, dark card | `anigo-logo-on-dark.svg` / `ani_go_logo_on_dark.xml` | `anigo-mark-on-dark.svg` / `ani_go_mark_on_dark.xml` |
| Busy photo | Use a deliberate dark overlay with cream/gold or a solid cream tile with pine/gold | Same surface-dependent rule |

Select by the **surface directly behind the logo**, not by global dark mode alone. Do not place the pine-colored original directly on a charcoal or dark-green background. Preserve a clear quiet area and proportional scaling. The gold hue remains `#CBA14E` in both variants; no glow, stroke, crop, or symbol redraw is authorized.

This Phase 1 change **only provides the missing official dark compact assets**. It deliberately does **not** change logo position, wordmark frequency, auth headings, Android app bars, favicon selection, or launcher geometry. Phase 2 will select the correct assets where they belong, Phase 3 will remove redundant wordmarks, and Phase 4 will review actual renders, contrast and navigation across themes and sizes.

Asset parity is guarded by `tests/web-ui/anigo-brand-assets.test.cjs`: the web/Android dark marks must match the original exact geometry, viewport and path order, with only `#134D30` changing to `#FFFDF6`.
