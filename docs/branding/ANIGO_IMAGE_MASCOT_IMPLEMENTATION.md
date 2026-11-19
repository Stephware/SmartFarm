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

### Step 3 — character-led onboarding screens

The existing six-scene journey is retained: intro, welcome, farm, crop plan, fieldwork, and harvest. This step refines the screens without adding account requirements, modifying farm records, or changing navigation.

- The welcome slide introduces Ani as the friendly farm guide while retaining the speech bubble and short talking-to-wave animation from Step 2.
- Each of the four lessons now has an explicit lesson label and a short, distinct **ANI SAYS** explanation, alongside the existing feature demonstration and the sample-only disclosure.
- On ordinary portrait phones, the illustrative carabao leads a vertically stacked stage with a full-width, more readable example card. Tablet and short landscape layouts keep the efficient side-by-side composition; 320dp / 1.6× system-text stacking stays available.
- The progress display distinguishes the welcome slide from the four lessons. Existing Swipe, Back, Next, Skip, Continue, Android Back and opt-in Replay behavior are unchanged.
- New device tests verify all four tips and the compact presentation; required light-phone welcome/farm and dark-phone harvest screenshots extend the existing visual audit from 26 to **29** PNGs.
- Accessibility retains a heading, readable theme-aware contrast, bounded animation, meaningful illustration descriptions, 48dp minimum navigation targets, and optional reduced motion. Existing offline/sync and partial-harvest examples remain accurate.

Final screen composition still needs owner visual review on an actual phone before release. The Step 3 CI run validates code and captures, not artistic approval.

### Step 4 — first-time, authenticated and replay routing

- **First launch:** an unauthenticated device with no `completed_v1` flag opens the illustrated intro, Welcome lesson, and four guides. Skip, native Back from the beginning, or final Continue persists the device-local flag before navigating to the real welcome/sign-in screen.
- **Returning device:** after the flag is set, subsequent launches go to Welcome rather than replaying the tour. Signing out or changing users does not clear this preference; clearing app data restarts the first-run experience.
- **Authenticated session:** an already signed-in user starts in Farms, and an account becoming authenticated while the automatic first-run guide is open bypasses it. Logging in now clears *all* guest destinations; signing out clears *all* authenticated destinations, including when the application originally started in Farms without Welcome in its back stack.
- **Optional replay:** Welcome guests and signed-in My Account users can open the identical illustrated guide without altering the completion flag or signing in/out. The preview has a **Close guide** action, then either **Back to Welcome** or **Back to My Account** on the final lesson; Back returns to the previous page or original screen as appropriate.
- Navigation regression tests use a real Compose NavHost to assert guest/login and authenticated/sign-out back-stack isolation, including direct Farms starts and completed guest/account replays. The full farmer journey test helper also recognizes the optional replay's Close guide label.
- Authentication remains in memory as before. No server, Room, farm permission, or API contract changed.

### Release gates

- Confirm Android build, unit tests, UI instrumentation, and bitmap decoding across device sizes.
- Inspect real screenshots in light/dark, compact, 1.6× text, landscape and tablet views.
- Review PR #145 in draft and reconcile changes from `dev` before any merge.
