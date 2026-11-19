# AniGo — approved illustrated carabao onboarding

## Implementation scope

Source artwork is the newly generated, user-approved, warm brown chibi carabao with a straw salakot, curved cream horns and green AniGo kerchief. Android does **not** redraw the mascot with Compose Canvas paths anymore.

### Sprite asset
- `mobile/SmartFarm.Android/app/src/main/res/drawable-nodpi/ani_go_carabao_atlas.webp`
- One offline transparent WebP atlas, 1024 × 768 pixels, split into twelve 256 × 256 frames arranged 4 columns by 3 rows.
- Rasterized with soft shading, consistent outline and farmer hat; no network access or external asset CDN.
- `AniGoCarabao.kt` decodes the atlas once per composition and displays the selected frame as a real bitmap using Compose `Image`. Frame selection is pure and unit-tested.

| Frame index | Visual | Use |
| --- | --- | --- |
| 0–3 | Four full-body walking images | 2.55-second opening entrance, leg-frame switching |
| 4 | Closed mouth, smiling bust | Talk animation first frame |
| 5 | Open mouth, smiling bust | Talk animation second frame |
| 6 | Waving carabao | Welcome / settled intro |
| 7 | Pointing carabao | Farm setup lesson |
| 8 | Seedling planting carabao | Crop planning lesson |
| 9 | Clipboard writing carabao | Fieldwork lesson |
| 10 | Produce basket carabao | Partial harvest lesson |
| 11 | Arms-up celebration | Optional completion/success illustration |

### Actual user journey

1. First app launch, no active authenticated account and no `completed_v1` preference → skippable animated **intro** with walking image switching.
2. **Welcome to AniGo** → friendly talking carabao mouth-image switching and short speech bubble.
3. **Your farm, organized** → illustrative farm and plots, pointing pose.
4. **Grow with a plan** → illustrative crop milestones, planting pose.
5. **Record work as you go** → sample watering/inspection/photo and accurate `Pending sync` language, clipboard pose.
6. **From field to harvest** → two partial harvest records (50 kg + 35 kg), basket pose; Continue takes the user to the real Welcome/Log in/Register page.

After Skip or completion, the device-local completion flag prevents automatic replay. Currently authenticated users bypass automatic onboarding and go to farms, while deliberate Replay from Welcome/My Account remains available. The existing AuthRepository intentionally retains tokens **in memory only**, so a *fully restarted process* requires sign-in again; no persistent login has been introduced for this design task.

### Motion and accessibility

- Walking frames cycle during the entrance, with subtle translation; an optional open/closed mouth swap follows arrival.
- The welcome carabao alternates mouth images without audio for about two seconds, and stops when inactive.
- Feature-page gestures use brief bounded transforms synchronized with illustrative example cards.
- Reduced-motion preference bypasses the intro and shows static poses and complete instructional cards.
- All content is sample-only, does not write to farm/inventory/harvest databases, and does not require network connectivity.
- Existing four lesson-focused instrumentation tests intentionally bypass the extra Welcome page using `includeWelcome=false`. New tests verify the complete six-scene production flow and welcome screenshots.

### Release gates

- Confirm Android build, unit tests, UI instrumentation, and bitmap decoding across device sizes.
- Inspect real screenshots in light/dark, compact, 1.6× text, landscape and tablet views.
- Review PR #145 in draft and reconcile changes from `dev` before any merge.
