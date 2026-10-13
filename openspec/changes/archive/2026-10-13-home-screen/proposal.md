## Why

The game opens straight into the Sandbox with sample code, so a new player has no welcome, no first villager and no idea how a level works. The cozy design (`design-docs/cozy/screens/04-home-first-launch.html`, `05-home-welcome-back.html`) makes Main Street the game's front door: one building per level, lit as the player opens shops, with a clear "Start" or "Continue". Now that `village-progress` saves completion, tries and drafts, the street can show real progress.

## What Changes

- **Home screen.** A new screen the game opens on, instead of the Sandbox. It has:
  - a header with the brand and, with saved progress, "N of M shops open";
  - a large **street card** with sky, clouds, a hill and a road. Each level is a building with the villager's badge on its roof, the shop name, "<number> · <title>" and a status. The Sandbox is the last building.
- **Shop states.**
  - Open: lit facade, `glow` window and sign, "✓ Open".
  - Current: the first level not yet completed. Dark facade with a dashed `wood` outline, and a "Start here" pill (nothing completed yet) or an "Up next" pill.
  - Not yet: dark facade, locked badge, "Not open yet". It stays clickable, because there are no locked levels (`village-progress`).
  - Coming soon: a placeholder shop for a level that isn't bundled yet. Not clickable.
- **First launch** (no level completed and no saved code): "Welcome to Compose Village", a **Your first villager** card with Rosa's request, her "New: …" chips and **Start: 1 · Hello Main Street**, the three-step **How a level works** card, and **Open the Sandbox**.
- **Welcome back** (saved progress):
  - "Welcome back" and a line naming the latest open shop and who is waiting;
  - **Pick up where you left off**: the current level's villager and request, "Your code is saved" or "Starter code is ready", "N of M wishes" and **Continue: <title>**;
  - **Open shops**: each completed level with its saved Lines and Tries and **Visit**;
  - the Sandbox card.
- **All shops open:** the welcome back form without a current level. The pick-up card says every shop on the street is open and points to the Sandbox.
- **Placeholder shops.** Marco's Kitchen, Dona Lúcia's School and Iara's Ferry are shown as "Coming soon" from an app-side list until their levels are bundled. A placeholder is dropped once a bundled level has its villager.
- **Navigation.** "Main Street" in the level screen's breadcrumb becomes a link back to the home screen. The level screen's footer street stays, and its shops still switch levels directly.
- **Wishes count.** Saved progress also records how many requirements the latest Run met, so the pick-up card can show "N of M wishes" after a restart.

Not in this change: talking villagers and busts (the separate `talking-villagers` change); the "hints" stat on Open shops (`hints-and-editor-aids`).

## Capabilities

### New Capabilities
- `home-screen`: the home screen's layout, the street card and its shop states, the first-launch and welcome-back forms, placeholder shops, and navigation between the home, the levels and the Sandbox.

### Modified Capabilities
- `level-screen`: the Header's breadcrumb street name becomes a link to the home screen.
- `player-progress`: Saved progress also records the number of requirements the latest Run met.

## Impact

- **`app`:** a top-level destination (home, level or Sandbox) in the controller and `Main.kt`. New home composables: street card, building, pick-up card, Open shops, How a level works, Sandbox card. The placeholder shop list. A `wishes` field in `LevelProgress`. A clickable breadcrumb in `Header`. `WindowCheck` gains home-screen checks.
- **`levels`:** none.
- **Save file:** an additive field with a default. Old files load unchanged and the format version stays 1.
- **Order:** archive `cozy-level-screen` and then `village-progress` first. This change modifies requirements those changes add.
