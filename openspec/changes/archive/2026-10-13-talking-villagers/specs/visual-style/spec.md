## MODIFIED Requirements

### Requirement: Motion and reduced motion
The game's transitions SHALL follow the cozy motion storyboard's timings:
- **Run press:** 80 ms to 0.96 scale.
- **Result:** 200 ms cross-fade.
- **Checklist:** a 120 ms stagger per row, a 200 ms color change and a 180 ms check stroke.
- **Failure:** one 300 ms shake of ±6 dp, then a 220 ms expand of the reason.
- **Completion:** a 600 ms glow, then the completion card sliding up 24 dp and fading in over 300 ms.
- **Panel unlock:** a 250 ms expand and fade, then two 600 ms pulse rings.
- **Talking:**
  - typing at 40 characters per second;
  - the mouth swaps every 120 ms while typing;
  - a 4 dp bob over 90 ms each time the mouth opens;
  - a 120 ms blink about every 3.6 s;
  - the ▼ blinking while it waits.
- **Big dialogue:** the scrim fades in while the bust and box slide up, and both fade out on close.

When the operating system asks for reduced motion, the game SHALL:
- skip the shake, the pulse rings, the typing, the mouth flaps, the bob, the blinking, the ▼ blinking and the big dialogue's slide;
- show lines whole;
- keep the color changes and fades.

A launch option SHALL be able to force reduced motion on or off.

#### Scenario: Reduced motion on macOS
- **WHEN** "Reduce motion" is on in macOS Accessibility settings and a requirement fails after a Run
- **THEN** the failed row turns red and its reason expands without shaking, and the villager's reaction appears whole

#### Scenario: Forcing reduced motion
- **WHEN** the game is started with reduced motion forced on
- **THEN** opening a level that unlocks a panel shows the panel and its tip without pulse rings

#### Scenario: Reduced-motion celebration
- **WHEN** reduced motion is on and a Run first completes a level
- **THEN** the big dialogue fades in with each line shown whole and the bust still
