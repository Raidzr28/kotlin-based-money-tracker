---
name: Money Manager
description: Money read as a level in a lit column of water, not a stack of metric cards.
colors:
  spray: "#DCEAF8"
  surf: "#6FB4FA"
  shelf: "#17406C"
  trench: "#113052"
  deep: "#0D2540"
  night: "#071628"
  abyss: "#04101D"
  waterline: "#8FD8FF"
  kelp: "#43D39E"
  coral: "#FF7A6B"
  sun: "#F6C566"
  expense-ink: "#C6DCF2"
  glass: "#2E4E9BE8"
  glass-edge: "#3DBFE0FF"
  caustic-high: "#382E86D8"
  caustic-low: "#14061A2E"
  surf-ink: "#15558F"
  kelp-ink: "#0E7A55"
  coral-ink: "#A33227"
  sun-ink: "#8A6300"
  expense-ink-light: "#1F4463"
  spray-light: "#EFF5FB"
typography:
  money-hero:
    fontFamily: "Archivo Variable"
    fontSize: "54sp"
    fontWeight: 700
    lineHeight: "58sp"
    letterSpacing: "-0.038em"
    fontFeature: "tnum"
    fontVariation: "wdth 118"
  money-large:
    fontFamily: "Archivo Variable"
    fontSize: "30sp"
    fontWeight: 600
    lineHeight: "36sp"
    letterSpacing: "-0.03em"
    fontFeature: "tnum"
    fontVariation: "wdth 115"
  money-medium:
    fontFamily: "Archivo Variable"
    fontSize: "20sp"
    fontWeight: 600
    lineHeight: "26sp"
    letterSpacing: "-0.02em"
    fontFeature: "tnum"
    fontVariation: "wdth 115"
  money-row:
    fontFamily: "Archivo Variable"
    fontSize: "16sp"
    fontWeight: 500
    lineHeight: "21sp"
    letterSpacing: "-0.012em"
    fontFeature: "tnum"
    fontVariation: "wdth 112"
  money-small:
    fontFamily: "Archivo Variable"
    fontSize: "13sp"
    fontWeight: 500
    lineHeight: "17sp"
    letterSpacing: "normal"
    fontFeature: "tnum"
    fontVariation: "wdth 112"
  display-large:
    fontFamily: "Archivo Variable"
    fontSize: "52sp"
    fontWeight: 700
    lineHeight: "56sp"
    letterSpacing: "-0.035em"
    fontFeature: "tnum"
    fontVariation: "wdth 118"
  headline-large:
    fontFamily: "Archivo Variable"
    fontSize: "28sp"
    fontWeight: 600
    lineHeight: "34sp"
    letterSpacing: "-0.02em"
    fontVariation: "wdth 100"
  headline-small:
    fontFamily: "Archivo Variable"
    fontSize: "19sp"
    fontWeight: 600
    lineHeight: "25sp"
    letterSpacing: "-0.012em"
    fontVariation: "wdth 100"
  title-medium:
    fontFamily: "Archivo Variable"
    fontSize: "15sp"
    fontWeight: 600
    lineHeight: "21sp"
    letterSpacing: "0sp"
    fontVariation: "wdth 100"
  body-large:
    fontFamily: "Roboto"
    fontSize: "16sp"
    fontWeight: 400
    lineHeight: "23sp"
  body-medium:
    fontFamily: "Roboto"
    fontSize: "14sp"
    fontWeight: 400
    lineHeight: "20sp"
  body-small:
    fontFamily: "Roboto"
    fontSize: "12sp"
    fontWeight: 400
    lineHeight: "17sp"
  label-large:
    fontFamily: "Roboto"
    fontSize: "14sp"
    fontWeight: 500
    letterSpacing: "normal"
  label-medium:
    fontFamily: "Roboto"
    fontSize: "12sp"
    fontWeight: 500
    letterSpacing: "0.03em"
  label-small:
    fontFamily: "Roboto"
    fontSize: "11sp"
    fontWeight: 500
    letterSpacing: "0.07em"
rounded:
  extra-small: "8dp"
  small: "14dp"
  medium: "20dp"
  large: "28dp"
  extra-large: "36dp"
  column: "32dp"
  hero-foot: "40dp"
  pill: "50%"
spacing:
  gutter: "20dp"
  plate-padding: "18dp"
  row-gap: "14dp"
  section-top: "28dp"
  section-bottom: "12dp"
  chip-gap: "8dp"
  tab-bottom: "96dp"
  detail-bottom: "36dp"
components:
  plate-surface:
    backgroundColor: "{colors.glass}"
    textColor: "{colors.spray}"
    rounded: "{rounded.large}"
    padding: "18dp"
  plate-sunk:
    backgroundColor: "{colors.deep}"
    textColor: "{colors.spray}"
    rounded: "{rounded.large}"
    padding: "18dp"
  pill:
    backgroundColor: "{colors.glass}"
    textColor: "{colors.spray}"
    typography: "{typography.label-large}"
    rounded: "{rounded.pill}"
    padding: "12dp 16dp"
    height: "48dp"
  pill-emphasis:
    backgroundColor: "{colors.surf}"
    textColor: "#04223F"
    typography: "{typography.label-large}"
    rounded: "{rounded.pill}"
    padding: "12dp 16dp"
    height: "48dp"
  chip:
    backgroundColor: "#091E33"
    textColor: "#A9C4E0"
    typography: "{typography.label-large}"
    rounded: "{rounded.pill}"
    padding: "7dp 14dp"
    height: "36dp"
  chip-selected:
    backgroundColor: "{colors.shelf}"
    textColor: "#CFE5FF"
    typography: "{typography.label-large}"
    rounded: "{rounded.pill}"
    padding: "7dp 14dp"
    height: "36dp"
  flag:
    backgroundColor: "{colors.sun}"
    textColor: "{colors.sun}"
    typography: "{typography.label-medium}"
    rounded: "{rounded.pill}"
    padding: "6dp 11dp"
  ledger-row:
    textColor: "{colors.spray}"
    typography: "{typography.body-large}"
    padding: "8dp 0"
    height: "56dp"
  nav-row:
    textColor: "{colors.spray}"
    typography: "{typography.body-large}"
    padding: "8dp 0"
    height: "56dp"
  marker:
    backgroundColor: "{colors.surf}"
    textColor: "{colors.surf}"
    rounded: "50%"
    size: "42dp"
  fab-primary:
    backgroundColor: "{colors.surf}"
    textColor: "#04223F"
    typography: "{typography.label-large}"
    rounded: "{rounded.medium}"
---

# Design System: Money Manager

## Overview

**Creative North Star: "The Lit Column of Water"**

The screen is a column of water lit from above. Depth, not shadow, is the only elevation mechanism: a surface nearer the top of the stack is lighter and more translucent because it is nearer the light, and a surface buried under others is denser navy. Material 3's tonal elevation already works this way, so the metaphor rides the platform instead of fighting it.

The system's thesis is that the figure and the field are the same fact. Safe-to-spend is set at 54sp over a water surface whose fill height *is* that figure; remaining days run down the right edge as a gauge of ticks. The app refuses the fintech dashboard header — balance plus a row of stat chips — and it refuses a card inset on four sides for the one thing the user opened the app to read. Plates transmit rather than refract: their beds are translucent so the ground's gradient and its slow caustics read through every card. There is no backdrop blur anywhere in the build, by decision — Compose at this version has no backdrop filter, and a fake blur would be the dishonest half of the material.

Density is calm and list-led. One warm hue exists in the whole product and it is rationed. Type is Archivo throughout, run wide for money and normal for prose, tabular wherever a figure stands for real currency. The build compiles but has never been rendered on a device; every value below is read from source, and claims about appearance are deliberately absent.

**Key Characteristics:**
- Depth ladder from `#17406C` down to `#04101D`; elevation is tint and alpha, never a drop shadow.
- Translucent plates lit by a 1.2dp specular hairline along the top edge only.
- Archivo variable, wdth 112–118 for money, wdth 100 for text; `tnum` on every figure.
- One warm hue (`#F6C566` dark / `#8A6300` light), reserved for goals and streaks.
- One authored gesture: filling. The Home water column's rise and swell is its loudest instance.

## Colors

A single blue world with three functional escapes: a kelp green for money in, a coral for trouble, and one warm gold held back for goals.

### Primary
- **Surf** (`{colors.surf}`): the light-struck blue. Water body fill, FAB container, selected navigation indicator source, default marker tint, chart accents. Its light-scheme counterpart is **Surf Ink** (`{colors.surf-ink}`).
- **Waterline** (`{colors.waterline}`): a 2dp stroke along the water's surface and on the depth gauge's spent ticks. The brightest stroke on the screen, because it marks the one number the user opened the app to read.

### Secondary
- **Kelp** (`{colors.kelp}`): money in — incoming amounts, "balanced" states. Light: **Kelp Ink** (`{colors.kelp-ink}`).
- **Coral** (`{colors.coral}`): alert — over budget, overdue, and a water fill that has passed the line. Light: **Coral Ink** (`{colors.coral-ink}`).

### Tertiary
- **Sun** (`{colors.sun}`): goals and streaks. Light: **Sun Ink** (`{colors.sun-ink}`). Deliberately absent from the categorical chart scale.

### Neutral
- **Spray** (`{colors.spray}`): primary text on dark; also the inverse surface.
- **Expense Ink** (`{colors.expense-ink}`): the tone an ordinary expense amount is set in — a cool near-white, not red. Light: `{colors.expense-ink-light}`.
- **Shelf / Trench / Deep / Night / Abyss** (`{colors.shelf}` → `{colors.abyss}`): the five-step depth ladder, index 0 nearest the light. It is the ground gradient (stops at 0 / 0.22 / 0.52 / 0.78 / 1) and the source of every plate bed. The light world runs the same five steps from white down to `#9FBBD8`.
- **Glass / Glass Edge** (`{colors.glass}` / `{colors.glass-edge}`): the plate film and its specular hairline.
- **Caustic High / Caustic Low** (`{colors.caustic-high}` / `{colors.caustic-low}`): three slow radial scatters over the ground, drifting on a 24s cycle.

### Signature: The Submerged Photograph
The only photography in the app, and the only screen that carries any. A goal's picture sits under
`SubmergedPhoto`, which draws the same two-sine swell over it at 62% tint so the image reads
through the water instead of being painted out by it. The level is the goal's own fraction, so the
thing you are saving for goes under as you save. 176×226dp at 24dp corners, `ContentScale.Crop`,
with the percentage and the amount stacked beneath rather than overlaid.

### Merchant Monogram
A ledger row leads with the merchant's initials, not a category glyph. The disc tint is a function
of the merchant's own name (`name.hashCode()` into `categoryScale()`), so one merchant keeps one
colour forever and a column of rows stops looking like a column of the same row. The category
survives as a badge at 46% of the disc, bottom-end, on `surfaceContainerHighest`. One or two
letters: first letters of the first two words, else the first two characters.

### Named Rules

**The One Warm Hue Rule.** `#F6C566` (dark) / `#8A6300` (light) means goals and streaks and nothing else. It is excluded from `categoryScale()` on purpose, so a warm mark anywhere in the app means exactly one thing.

**The Expenses Are Not Red Rule.** Expense amounts take Expense Ink. Only over-budget and overdue reach for alert. In a tracker you log expenses all day; painting each one as an alarm makes the alarm meaningless.

**The Never-Colour-Alone Rule.** Colour never carries the direction of money by itself. Every amount has a sign and sits in a tabular column, and every "over" state pairs its tone with a written label — so a screen reader and a colour-blind eye get the same fact.

## Typography

**Display / Money Font:** Archivo Variable at wdth 112–118 (`ArchivoWide`)
**Text Font:** Archivo Variable at wdth 100 (`ArchivoText`)
**Body / Label Font:** Material 3's default (Roboto), inherited unchanged apart from size, line-height and weight.

**Character:** Archivo run wide gives figures a broad, engineered stance — machinery rather than editorial flourish. The same family at normal width keeps headings in the family without competing with the money. Width is earned: wide means the number is real money.

### Hierarchy
- **Money Hero** (700, 54sp/58sp, -0.038em, `tnum`): safe-to-spend on Home, over the water surface. At most one per screen.
- **Money Large** (600, 30sp/36sp, `tnum`): a screen's headline figure inside a plate.
- **Money Medium** (600, 20sp/26sp, `tnum`): the default `Stat` value.
- **Money Row** (500, 16sp/21sp, `tnum`): every ledger amount; the column that aligns on the decimal.
- **Money Small** (500, 13sp/17sp, `tnum`): dense secondary figures.
- **Display Large / Medium / Small** (700 / 600 / 600, 52 / 40 / 32sp, `tnum`): the Material display roles, re-cut wide and tabular for the same reason.
- **Headline Large → Small** (600, 28 / 23 / 19sp, negative tracking): screen and section titles.
- **Title Large / Medium** (600, 19 / 15sp): plate titles and top-bar titles.
- **Body Large / Medium / Small** (400, 16 / 14 / 12sp): merchant names, prose, subtitles.
- **Label Large / Medium / Small** (500; +0.03em on medium, +0.07em on small): captions above figures, chips, pills, flags.

### Named Rules

**The Real Money Rule.** Any figure standing for real currency is set in a `MoneyType` style — Archivo wide, tabular, never italic — and rendered through `MoneyText`. Counts, percentages and day numbers are not money and use the Material roles.

**The Cents Are Shown Rule.** Money is `Long` minor units formatted by `moneyParts`. Cents are always present and never emphasised: 64% of the base size at 52% alpha. Never `Double`, never rounded away.

**The No Eyebrow Rule.** A section heading carries its own weight: a title, an optional caption beneath it, and an optional trailing text action. No kicker, no numbering, no all-caps label stacked above a heading.

## Layout

One gutter, 20dp, on every screen; content never touches the edge of the water. Tab bodies are `LazyColumn`s with 8dp of top padding, and at the foot either 96dp to clear the FAB or 24dp where there is no FAB to clear — the shell's Scaffold already pads for the navigation bar, so that figure is FAB clearance and nothing else. Detail screens use the same gutter with 36dp at the foot and a transparent top app bar so the water runs behind it.

`Modifier.bleed()` is the single sanctioned escape from the gutter: it widens a list item by 2× the gutter and shifts it back, so the Home hero runs full-bleed. A thesis about a column of water cannot be delivered inside a card inset on four sides.

**The Screen Pays Its Own Status Bar Rule.** The shell's Scaffold applies `systemBars.only(Horizontal + Bottom)` and deliberately not the top, so content begins at the physical top of the display and the Home hero can run under the status bar. Every screen then re-applies the top inset itself, in exactly one place: `TabColumn` through its `insetTop` parameter (true everywhere but Home, which swallows the inset into the hero's own height and padding), `DetailScaffold` through its top app bar's own `TopAppBarDefaults.windowInsets`, and the lock screen through `windowInsetsPadding(systemBars)`. A new screen that renders under the clock has skipped this step. Never add the top inset back at the shell — that is what put the hero in a box in the first place — and never apply it twice on one screen.

Rhythm: 18dp inside a plate, 14dp between a marker and its text, 8dp between chips, 28dp above a section heading and 12dp below it, 4dp between rows stacked inside one plate. Tappable rows are at least 56dp tall, pills at least 48dp, chips at least 36dp.

Responsive behaviour turns on a single breakpoint at **640dp**. Below it, a five-item navigation bar at the bottom; at or above it, a navigation rail on the leading edge — never a stretched phone bar. The water ground is drawn once behind the whole shell and drifts only on Home and Lock; a background that animates on every screen is a battery bill, not a design.

## Elevation & Depth

**There are no shadows in this system.** Not one `Modifier.shadow` anywhere in the UI source, and `tonalElevation` is explicitly zeroed on the navigation bar. Depth is tint and alpha along the five-step ladder. `Modifier.glassPlate(shape, depth)` takes an integer depth clamped to 0–3 and derives everything from it: the glass film loses 18% of its alpha per step down, the specular hairline loses 25% per step, and the bed walks the Material surface-container roles.

### Depth Vocabulary
- **Depth 0** (`surfaceContainerHigh` at 62% + the full glass film): at the surface, the brightest plate on screen. The default.
- **Depth 1** (`surfaceContainer` at 70%): a plate under the page's own emphasis — empty states, secondary blocks.
- **Depth 2** (`surfaceContainerLow` at 78%): a plate nested inside another plate.
- **Depth 3** (`surfaceContainerLowest` at 86%): the floor. Also the bed of every gauge, vessel and water column.
- **Specular edge** (1.2dp stroke, transparent → glass-edge → transparent across a 22dp top-corner arc): the light that reaches a plate. Top edge only — no bottom edge, no outline, no border.

### Named Rules

**The Elevation Is Depth Rule.** A plate's bed alpha and tint are a function of its `depth` argument and nothing else. Never a drop shadow, never a hand-picked alpha, never a border to fake a lift.

**The Transmission Rule.** Every plate lets the ground read through it. The gradient and its caustics are drawn once, behind everything; a plate that paints itself opaque buries the only reason the ground exists.

## Shapes

Corners are generous and rounded throughout: 8 / 14 / 20 / 28 / 36dp across the Material shape roles, with `large` (28dp) as the default plate. The Home hero is a 40dp bottom-corner sweep under a square top, because it is part of the status bar's water rather than a card; a standalone water column uses 32dp.

Anything that is an action or a token — pill, chip, flag, section action, marker — is fully round (50%). Markers are circles at 42dp (38dp in a nav row, 34dp in a dense row) holding a drawn vector glyph at 48% of the disc, on a 16%-alpha tint of the glyph's own colour. Gauges and bars are stadium-ended. Goal photographs are 176×226dp at 24dp.

## Components

### Buttons
- **FAB (primary action):** filled Surf with `onPrimary` content. Extended with a "Log" label on Home, icon-only on Ledger. One FAB, bottom-end, and only where the primary action is real — screens whose add-flow does not exist ship no FAB rather than a decoration.
- **Pill:** the button that sits on the water. Fully round, min 48dp, 16dp/12dp padding, 18dp leading glyph, `labelLarge`. Default fill is the glass film; `emphasis = true` swaps to a filled Surf container with `onPrimary` text.
- **Section action:** a plain text button in Primary at `labelLarge`, round-clipped, 12dp/10dp padding. Never a chevron pretending to be a button.

### Chips
- **Style:** fully round, min 36dp, 14dp/7dp padding, optional 15dp leading glyph, `labelLarge`.
- **State:** unselected is `surfaceContainerLow` with `onSurfaceVariant` text; selected is `primaryContainer` with `onPrimaryContainer`. Selection is a **fill** change, never an outline-colour-only change.
- **Rows** scroll horizontally at 8dp spacing.

### Cards / Containers
- **Corner Style:** 28dp default (`{rounded.large}`).
- **Background:** the glass film over a depth-derived bed; see Elevation & Depth.
- **Shadow Strategy:** none. The `depth` argument only.
- **Border:** none. The specular top-edge hairline is the only stroke.
- **Internal Padding:** 18dp.

### Navigation
- **Bottom bar:** five equal destinations (Home, Ledger, Budgets, Reports, More), `surfaceContainer` at 94% alpha, zero tonal elevation, `primaryContainer` selection indicator. No circular "+" inside the bar — that is an iOS tab-bar habit; the primary action gets a real FAB.
- **Rail:** the same five items at ≥640dp.
- **Nav row:** a 38dp marker, title in `bodyLarge`, optional subtitle in `bodySmall`, optional trailing label, and one 20dp chevron whose semantics are cleared. 56dp minimum. Never a whole card per link.
- **Transitions:** tab to tab is a 190ms fade; forward into a detail slides in one-fifth of the width over 280ms with the same fade; back reverses at 280ms. Every duration and curve comes from `MoneyMotion`, never from a literal at the call site. Every branch checks `motionEnabled()` first and returns `EnterTransition.None` when animations are off.

### Signature: The Water Column
The hero. A container filled to a 0–1 level, its surface drawn as two summed sine waves (5dp and 2.6dp amplitudes at 2.1× and 4.7× across the width) under a 2dp waterline stroke, over a body gradient falling from 92% to 30% alpha with depth. A vertical scrim (72% → 26% → transparent across the top 85%) holds type contrast at any fill level — the world's own light falloff doing the accessibility work. `over = true` inverts the reading and turns the fill to alert, always paired with a sign and a written label. On Home it is 408dp tall and full-bleed, with the depth gauge — one tick per day of the month, every seventh tick long — down its right edge.

### Signature: Hand-drawn Charts
Ring, column pair, sounding line, cash-flow grid, balance trace and due calendar are drawn in Compose `Canvas` against `categoryScale()`, an eight-step cool scale with no warm hue in it. Ring labels ride their own arc as real text nodes instead of sitting in a legend, so TalkBack reads the ring as a list. No chart library: a kit brings its own gridlines and its own categorical rainbow into a committed world, and theming arguments never get them back out.

### Named Rules

**The One Authored Motion Rule.** The app has one authored gesture and it is *filling*. The Home water column's level rise (`MoneyMotion.FillHero`, 1100ms) plus its 6.4s swell is its loudest instance; the ring sweep, the submerged goal photo, the sounding line's draw-in and every `WaterBar` are quieter instances of the same gesture at `MoneyMotion.Fill` (720ms), each a single decelerating pass on first composition and never a loop. Nothing else animates beyond Material's standard grammar: no hover flourishes, no per-section entrances, no second hero motion in a different idiom. Every animated surface reads `motionEnabled()` first and collapses to a zero-duration tween when the system's "Remove animations" setting is on.

**The Arriving Curve Rule.** Entrances use `MoneyMotion.EnterEasing`
(`LinearOutSlowInEasing`), exits use `MoneyMotion.ExitEasing` (`FastOutLinearInEasing`). Material's
`FastOutSlowInEasing` is its *standard* curve and belongs to something travelling between two
on-screen positions; it accelerates in, and nothing that is arriving should accelerate in. Reaching
for the standard curve because it is the familiar one put the wrong easing on every entrance in
this app once already.

**The Reveal Plays Once Rule.** A fill is a one-shot. Never drive one with a bare
`animateFloatAsState` inside a `LazyColumn`: the list disposes an item when it scrolls out, so the
reveal replays on every scroll back and the gesture becomes a loop the user triggers by scrolling.
Use `rememberFill`, which keeps its played flag in `rememberSaveable` and comes back already full.

**The Photography Rule.** The app carries photographs in exactly one place: a savings goal, where
a picture of the thing being saved for does real work. Everywhere else the content is money, and
money has no photograph. No stock imagery in empty states, no decorative texture behind sections,
no illustrated onboarding. A picture that is not the user's own content is not content.

**The Feedback Is Not A Gesture Rule.** Material's own feedback grammar - a press state, a spring
settle on the amount when a key lands, a haptic at the commit - is not a second authored motion and
does not need justifying against the filling gesture. What it must never do is animate something
that is not responding to a user action in that moment.

**The Real FAB Rule.** One floating action per screen, and only where the primary action exists. A FAB that does nothing is worse than no FAB.

## Do's and Don'ts

### Do:
- **Do** express elevation through `Plate(depth = n)` / `Modifier.glassPlate(shape, depth)` and let the depth argument pick the bed and the edge.
- **Do** render every currency figure through `MoneyText` with a `MoneyType` style, so it is tabular, wide, and shows cents at 64% size / 52% alpha.
- **Do** keep money as `Long` minor units and format it through `moneyParts`.
- **Do** reserve `{colors.sun}` for goals and streaks.
- **Do** pair every "over" or "behind pace" tone with a sign, a label, or a written sentence.
- **Do** call `motionEnabled()` before any animation, and draw the ground once per shell with `animate` scoped to the screen that owns the hero.
- **Do** keep the 20dp gutter, and use `Modifier.bleed()` when — and only when — an item is the screen's thesis.
- **Do** leave Dynamic Color off by default and opt-in in Appearance; a wallpaper-derived scheme reorders the depth ladder until the waterline stops meaning anything.

### Don't:
- **Don't** add a drop shadow, an outline, or a hand-tuned alpha to fake elevation.
- **Don't** paint an ordinary expense red.
- **Don't** let colour be the only thing saying which way money went.
- **Don't** put `{colors.sun}` into a chart scale, a category tint, or a general accent.
- **Don't** widen `ArchivoWide` onto prose, or set money in the text width.
- **Don't** round money into a `Double` or drop the cents.
- **Don't** author a motion that is not a fill, or animate the background on a screen with no hero.
- **Don't** hand the top window inset back to the shell's Scaffold, or apply it twice on one screen.
- **Don't** put a circular add button inside the navigation bar, or ship a FAB on a screen whose add-flow does not exist.
- **Don't** stack a kicker or eyebrow above a section heading.
- **Don't** pull in a chart library; the charts are drawn so they belong to this water.
