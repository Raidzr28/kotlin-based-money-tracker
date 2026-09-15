# Raster provenance

Every raster shipped in this app, and where it came from.

## app/src/main/res/drawable-nodpi/goal_emergency.jpg
## app/src/main/res/drawable-nodpi/goal_laptop.jpg
## app/src/main/res/drawable-nodpi/goal_bali.jpg

- **Origin:** sourced, not generated. Fetched from Lorem Picsum on 2026-09-12 at
  `https://picsum.photos/seed/moneymanager-goal-{emergency|laptop|bali}/640/800`.
- **Status:** PLACEHOLDER. These are arbitrary stock photographs selected by a seed string, not
  pictures of the things the goals name. They exist so the Goals screen can be judged at real
  density instead of against empty boxes.
- **Licence:** Lorem Picsum serves Unsplash photography. Check the licence of the specific image
  before shipping to Play, or replace it.
- **Replace with:** a photograph the user chooses per goal, stored with the goal record once the
  data layer lands. At that point these three files are deleted.
- **Used by:** `ui/screens/Money.kt` → `GoalsScreen`, through `SubmergedPhoto`.

## app/src/main/res/drawable/ic_launcher_foreground.xml
## app/src/main/res/drawable/ic_launcher_background.xml

- **Origin:** authored by hand as vector drawables for this project. Not generated, not sourced.
- **Subject:** three strata of a falling level over the waterline, the same idea the Home hero
  draws at full size.
- **Status:** ships as-is.
