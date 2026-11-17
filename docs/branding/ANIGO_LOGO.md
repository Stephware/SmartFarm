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
The web shared layouts and landing footer display official SVG assets instead of separate leaf icons and typed names. Guest and desktop web workspace headers use full wordmarks; authenticated mobile web headers use the compact mark. Android photo authentication uses the official cream/gold full wordmark over the dark image; signed-in native app bars use the compact mark. The Android app label remains **AniGo**. Additional duplicated copy and form wordmarks will be audited separately in Phase 3.

Do not alter the symbol geometry, text, or color distribution in future UI changes; scale it proportionally. On a pine-green sidebar use the cream/gold variant for contrast. Preserve accessible labeling on logo links and never duplicate text next to the wordmark image.

## Background-aware variants (Phase 1, assets only)

| Actual background beneath logo | Full wordmark | A-only mark |
| --- | --- | --- |
| White, cream, light card | `anigo-logo.svg` / `ani_go_logo.xml` | `anigo-mark.svg` / `ani_go_mark.xml` |
| Dark charcoal, pine sidebar, dark card | `anigo-logo-on-dark.svg` / `ani_go_logo_on_dark.xml` | `anigo-mark-on-dark.svg` / `ani_go_mark_on_dark.xml` |
| Busy photo | Use a deliberate dark overlay with cream/gold or a solid cream tile with pine/gold | Same surface-dependent rule |

Select by the **surface directly behind the logo**, not by global dark mode alone. Do not place the pine-colored original directly on a charcoal or dark-green background. Preserve a clear quiet area and proportional scaling. The gold hue remains `#CBA14E` in both variants; no glow, stroke, crop, or symbol redraw is authorized.

Phase 1 introduced missing dark compact assets without changing any layout. Phase 2 assigns those assets to the appropriate shared headers. Phase 3 will assess redundant wordmarks and product-name copy; Phase 4 will review actual renders, contrast and navigation across themes and sizes. Favicon and launcher geometry remain unchanged.

Asset parity is guarded by `tests/web-ui/anigo-brand-assets.test.cjs`: the web/Android dark marks must match the original exact geometry, viewport and path order, with only `#134D30` changing to `#FFFDF6`.

## Phase 2 — Placement matrix (implemented)

| Surface | Form | Contrast treatment | Reason |
| --- | --- | --- | --- |
| MVC guest header and landing footer | Complete wordmark | Green/gold on light, cream/gold on dark | Product introduction |
| MVC desktop workspace/sidebar | Complete wordmark | Cream/gold against its always-pine background | Persistent application identity |
| MVC and Super Admin mobile signed-in headers | Standalone A mark | Green/gold on light, cream/gold on dark via shared logo-pair CSS | Save space for farm picker and page controls |
| Android photo-based welcome/login/registration/recovery | Complete approved wordmark | Cream/gold on permanently darkened farm photo | Product introduction, not a hand-drawn or typed substitute |
| Android authenticated app bars | Standalone A mark | Green/gold on light Material surface, cream/gold on dark Material surface | Keep farm and task context prominent |
| Main content/cards/interactive dialogs | No new branding | Show functional page titles and data | Avoid clutter; Phase 3 handles residual duplicates |

Keep `aria-label` on mobile web logo links and a nonempty `contentDescription` on Android branding. Decorative backgrounds remain inaccessible to screen readers. Header mark remains meaningful without duplicating text nearby. At widths below 375dp the Android workspace **still uses the same mark**, rather than switching to a full wordmark.

Phase 2 intentionally leaves the desktop login/registration secondary form logo and several incidental 'AniGo' text mentions untouched; Phase 3 will remove only verified redundancy without rewriting necessary legal, accessibility, navigational, or account-support language. Web/Android placement is checked by `tests/web-ui/anigo-brand-placement.test.cjs`.
