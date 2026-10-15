# Compose Village — Cozy design (direction A)

The game should feel like a place: a small village whose shops light up as the player helps each villager. This folder is the reference for implementing that look in Compose Multiplatform.

Live, editable source: the "Compose Village UI" canvas in Claude Design.

## Contents

| Path | What it is |
|---|---|
| `screens/01-before-run.html` | Level 2 (Open Sign) before the first Run: sketch ghosted in the preview, empty checklist |
| `screens/02-run-3-of-4.html` | After a Run with one failing requirement: villager reaction, failure reason, highlighted line |
| `screens/03-level-complete.html` | All requirements pass: lit sign, completion card, next level |
| `screens/04-home-first-launch.html` | Home on first launch: the dark street, Rosa's card with Start, "How a level works", the Sandbox |
| `screens/05-home-welcome-back.html` | Home with saved progress: Rosa's shop lit, Tom up next, Continue, open shops |
| `screens/06-level-intro-dialogue.html` | *Animated.* Level opens with Tom talking in a dialogue box: typed text, mouth flaps, ▼ to continue |
| `screens/07-reactions-after-run.html` | *Animated.* Run, then Run again: a failing Run talks in the card's bubble, level complete gets the big dialogue |
| `screens/villager-cast.html` | *Animated.* The five talking busts (Rosa, Tom, Marco, Dona Lúcia, Iara) and the rules for their motion |
| `screens/motion-storyboard.html` | Keyframes, timings and Compose APIs for the five key animations |
| `screens/villager-icons.html` | Profession icon set in both treatments (this folder uses the cozy badges) |
| `screens/dc-lite.js` | Small script that plays the animated mockups outside Claude Design |
| `icons/*.svg` | One profession glyph per villager, 24×24, `stroke="currentColor"` |

Open the HTML files in a browser. Most are static mockups. The *animated* ones load `dc-lite.js` from the same folder, so serve the folder (for example `python3 -m http.server --directory design-docs/cozy/screens`) instead of opening them from the file system if your browser blocks local scripts. Add `?reducedMotion=1` or `?speed=60` to their URL to try those settings.

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

## Home screen

The game opens on Main Street instead of inside a level. The street is the menu: one building per level, then the Sandbox at the end.

- **Street card:** sky (`#F6E7CE`), two clouds, a hill (`#EDDCBF`) and a road (`#E3CDA9` with a dashed centre line). Each building is a button with the villager's badge on its roof, the shop name, the level, and a status.
- **Shop states:**
  - Open: lit facade, `glow` window and sign, a soft glow behind it, "✓ Open".
  - Current: dark facade with a dashed `wood` outline, "Start here" or "Up next" pill.
  - Not yet: dark facade, locked badge, "Not open yet".
- **First launch:** "Welcome to Compose Village", Rosa's card with her request and **Start: 1 · Hello Main Street**, "How a level works" in three steps, and the Sandbox.
- **With saved progress:** "Welcome back" and "N of M shops open" in the header. **Pick up where you left off** shows the current villager, "Your code is saved", their wish count and **Continue**. "Open shops" lists finished levels with their stats and **Visit**.

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

## Talking villagers

Villagers speak like characters in a classic RPG: a large bust beside a dialogue box, text typed out letter by letter, and their mouth moving while it types. See `06-level-intro-dialogue.html`, `07-reactions-after-run.html` and `villager-cast.html`.

**When they talk**
- **Level intro:** when a level opens, the screen dims (`ink` at 55%) and the villager introduces themselves and asks for their sign over 3–4 lines. **Skip intro** closes it at any time; the last line offers **Let's build it!**. **Talk to <name>** on the villager card plays it again.
- **Failing Run:** the reaction types out in the card's speech bubble with the small avatar talking. It never covers the editor, so the player keeps coding.
- **Level complete:** the big dialogue comes back with the villager delighted, sparkles around them and the dialogue box glowing. The last line offers **See <name>'s shop**, which closes it on the lit sign and the open shop.

**Dialogue box**
- `surface` fill, 4 dp `wood` border with an inner `outline` ring, 22 dp radius and a 6 dp `woodDark` bottom shadow.
- A name tab (`wood` pill: name in Fredoka 18, profession in `chip`) overlaps the top edge.
- Text is Atkinson Hyperlegible 24 sp, line height 1.5, with room for three lines so the box never jumps.
- Footer: "2 of 4" on the left. Once a line has finished typing, a blinking ▼ "Click to continue" appears on the right.
- Clicking the box while text is typing shows the whole line; clicking again goes to the next line. In the game, Space and Enter should do the same.

**Busts.** Every villager uses the same rig, so one composable can draw them all. Only the eyes, the mouth and the bob move.

| Part | Behaviour | Compose |
|---|---|---|
| Typing | 40 characters per second | `LaunchedEffect(line)` + `delay` |
| Mouth | swaps between open and closed every 120 ms while typing; closed when the line is out | `rememberInfiniteTransition`, gated on typing |
| Bob | 4 dp up while the mouth is open, 90 ms ease-out | `animateDpAsState` |
| Blink | 120 ms, about every 3.6 s, offset per villager | `LaunchedEffect` loop with `delay` |
| Big dialogue | scrim fades in, bust and box slide up | `AnimatedVisibility(fadeIn + slideInVertically)` |

| Villager | Look | Card tint |
|---|---|---|
| Rosa | baker's hat, brown hair, white jacket, flour on her cheek | `#F6DFB8` |
| Tom | green cap with a pink flower, green shirt, apron | `#F7D3DA` |
| Marco | tall chef's hat, moustache, red neckerchief, double-breasted jacket | `#E9EBEE` |
| Dona Lúcia | grey hair in a bun, round glasses, green cardigan | `#D7E8D2` |
| Iara | captain's hat with an anchor badge, long black hair, navy coat | `#CFE3F0` |

**Reduced motion:** each line appears whole. No mouth flaps, no bob, no blinking, and the ▼ stays still.
