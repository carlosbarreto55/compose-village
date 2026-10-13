# Compose Village — Cozy design (direction A)

The game should feel like a place: a small village whose shops light up as the player helps each villager. This folder is the reference for implementing that look in Compose Multiplatform.

Live, editable source: the "Compose Village UI" canvas in Claude Design.

## Contents

| Path | What it is |
|---|---|
| `screens/01-before-run.html` | Level 2 (Open Sign) before the first Run: sketch ghosted in the preview, empty checklist |
| `screens/02-run-3-of-4.html` | After a Run with one failing requirement: villager reaction, failure reason, highlighted line |
| `screens/03-level-complete.html` | All requirements pass: lit sign, completion card, next level |
| `screens/motion-storyboard.html` | Keyframes, timings and Compose APIs for the five key animations |
| `screens/villager-icons.html` | Profession icon set in both treatments (this folder uses the cozy badges) |
| `icons/*.svg` | One profession glyph per villager, 24×24, `stroke="currentColor"` |

Open the HTML files in any browser. They are static mockups (no interaction).

## Color tokens

| Token | Hex | Use |
|---|---|---|
| `ground` | `#F3EADB` | App background (parchment) |
| `groundRaised` | `#FBF4E8` | Header and footer bars |
| `surface` | `#FFF9EF` | Cards (villager, preview, checklist) |
| `outline` | `#E2D3BC` | Card borders, dividers |
| `ink` | `#2B2118` | Primary text |
| `inkMuted` | `#6B5A48` | Secondary text |
| `wood` | `#7A4E2D` | Sign frame, icon badge rings, brand mark |
| `woodDark` | `#5A3820` | Brand wordmark, glyph strokes on badges |
| `primary` | `#2F6B45` | Run button, passed requirements, progress |
| `primaryShadow` | `#1E4A2F` | 3 dp bottom shadow under primary buttons |
| `error` | `#A3402A` | Failed requirement icon |
| `errorContainer` | `#F8E3DC` | Failed requirement background |
| `onErrorContainer` | `#7E2E1C` | Failed requirement text |
| `glow` | `#F2C46B` | Lit shop / sign glow, completion card border |
| `chip` | `#F3E3C8` | "New: Row" concept chips |

Editor (dark, warm):

| Token | Hex |
|---|---|
| `editorBg` | `#2A2320` |
| `editorDivider` | `#3A302B` |
| `editorText` | `#F1E6D6` |
| `lineNumber` | `#7D6C5B` |
| `syntaxKeyword` | `#E6A15C` |
| `syntaxString` | `#A8D08D` |
| `syntaxFunction` | `#8FC1E3` |
| `syntaxComment` | `#A89784` |
| `syntaxAnnotation` | `#D9B8F0` |

## Typography

| Role | Font | Size / weight |
|---|---|---|
| Display, titles, buttons | Fredoka | 18–24 sp, 600–700 |
| Body, UI text | Atkinson Hyperlegible | 14–18 sp, 400 / 700 |
| Code, console | JetBrains Mono | 13–15 sp, 400; line height 1.75 |

All three are free on Google Fonts. Atkinson Hyperlegible was chosen for readability, since players read requests and failure messages closely.

## Shape and spacing

- Card radius 22 dp; editor and console radius 18 dp; buttons and chips fully rounded.
- Page padding 24 × 28 dp; gap between cards 16 dp; between columns 24 dp.
- Primary buttons are 48 dp tall (completion CTA 52 dp) with a 3 dp solid bottom shadow in `primaryShadow`.
- Touch targets are at least 44 dp.

## Layout

Header (brand, level breadcrumb with progress dots, build status, Run) → two columns → footer.

- **Left (wide):** villager card (avatar, name, profession badge, request, concept chips; reaction bubble after a Run) above the editor (file tab, "Need a hint?", collapsed imports, code, status line).
- **Right:** preview in a wooden sign frame with a "Your screen / sketch" toggle and an "Overlay sketch" checkbox, then the checklist, then the console (only in levels that use it).
- **Footer:** the street's shops; lit = completed, dashed outline = current level.

On narrow windows the right column wraps under the left one.

## Screen states

1. **Before Run:** preview shows the villager's sketch at 40% opacity with "Press Run to build yours on top of it". Checklist items are empty circles. Console explains that a customer will press buttons after Run.
2. **Run with failures:** "Run" becomes "Run again" and the status shows the build time. The villager reacts in a speech bubble. Passed items get a filled `primary` check; the failed item expands in `errorContainer` with a reason that explains what happened without giving away the code. The relevant code line gets a warm highlight.
3. **Level complete:** sign frame glows (`glow`), villager face turns delighted, completion card shows "<Shop> is open!", three stats (lines, tries, hints) and "Next: <level>". The footer shop for this level lights up.

## Villager icons

One glyph per villager profession, shown in a round badge: tint fill, 3 dp `wood` ring, glyph stroked in `woodDark`.

| Villager | Profession | Level | Icon | Badge tint |
|---|---|---|---|---|
| Rosa | Baker | 1 · Hello Main Street | `baker.svg` | `#F6DFB8` |
| Tom | Florist | 2 · Open Sign | `florist.svg` | `#F7D3DA` |
| Marco | Chef | 3 · Restaurant Menu | `chef.svg` | `#E9EBEE` |
| Dona Lúcia | Teacher | 4 · Contest Scoreboard | `teacher.svg` | `#D7E8D2` |
| Iara | Boat captain | 5 · Ferry Times | `boat-captain.svg` | `#CFE3F0` |
| Seu Raimundo | Fishmonger | Market chapter | `fishmonger.svg` | `#D3ECEA` |
| Bia | Mail carrier | Harbor chapter | `mail-carrier.svg` | `#F1DCC8` |
| Davi | Mayor | City Hall chapter | `mayor.svg` | `#E3D9F0` |

Locked levels keep the glyph but drop the tint (`#E2D6C2` fill, `#A8927A` ring, 55% opacity).

The SVGs use only paths, circles and rects with `currentColor`, so they convert directly to `ImageVector` (for example with Android Studio's Vector Asset import or a small script) and can be tinted at the call site.

## Motion

See `screens/motion-storyboard.html` for keyframes. Summary:

| Moment | Timing | Compose |
|---|---|---|
| Run → build | press 80 ms to 0.96 scale; progress bar while building; 200 ms cross-fade to result | `animateFloatAsState`, `LinearProgressIndicator`, `Crossfade` |
| Checklist resolves | 120 ms stagger per item; 200 ms color tween; 180 ms check stroke | `LaunchedEffect` + `delay`, `animateColorAsState`, `Animatable` |
| Failure nudge | one 300 ms shake (±6 dp); reason expands in 220 ms | `Animatable` with `keyframes`, `AnimatedVisibility(expandVertically)` |
| Level complete | glow 600 ms; face cross-fade 250 ms; card slides 24 dp up + fades in 300 ms | `updateTransition`, `AnimatedVisibility(slideInVertically + fadeIn)` |
| Panel unlock | 250 ms expand + fade; two 600 ms pulse rings; tip until first interaction | `AnimatedVisibility`, `Animatable` |

When the OS asks for reduced motion, skip the shake and the pulse rings; keep the color and fade transitions.
