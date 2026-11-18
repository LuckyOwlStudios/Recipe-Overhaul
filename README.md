# Pin Recipe

A Minecraft 1.21.1 mod for **Fabric** and **NeoForge** that lets you pin recipes from the vanilla recipe book,
and adds a recipe book to the brewing stand that you fill by reading Potion Scrolls.
Pinned recipes show up beside your inventory whenever it's open, and never on the in-game HUD.

## How to use

1. Open your inventory (or a crafting table) and open the recipe book.
2. Hover over a recipe and press **B** to pin it. Press **B** on it again to unpin it.
3. Close the recipe book: the pinned recipe stays beside your inventory, with its ingredients and result.
   Ingredients you don't have in your inventory are highlighted red.
4. Click the **x** on a pinned recipe to unpin it.
5. In a screen that can make the recipe, click the pinned recipe to fill it in, like clicking a recipe in the
   recipe book. Shift-click fills as many as you can. Missing items show as a ghost recipe. Crafting recipes fill
   the 2x2 grid (if they fit) or a crafting table; smelting, blasting and smoking recipes fill their furnace;
   brewing recipes fill the brewing stand.

- Click a red (missing) ingredient on a pin, or a ghost item in the crafting grid or brewing stand, to pin the
  recipe for that ingredient if you know it. Hovering it says so. A ghost Awkward Potion in the brewing stand also
  switches the ghost to Water Bottle + Nether Wart. For "any planks" slots it picks the kind you already have.
- Up to 4 recipes can be pinned; pinning a 5th drops the oldest.
- The key can be changed in Options → Controls → Key Binds → Pin Recipe (it can also be bound to a mouse button).
- Pins are saved to `config/pinrecipe.json`, so they survive restarts.
- On small screens the panel switches to a narrower layout. If there's still no room while the recipe book is open,
  the pins hide until you close the book, so they never cover it.
- Pins show on the survival inventory, crafting table, furnace, blast furnace, smoker and brewing stand.
  The creative inventory doesn't have a recipe book, so it doesn't show them.

## Brewing recipe book

- Open a brewing stand and click the green book button on the right to open the brewing recipe book.
- It lists every brewing recipe you've unlocked: potions, their Long and II versions, splash and lingering
  conversions, and brewing recipes added by other mods.
- Hover a recipe to see its base bottle, ingredient and effects. Recipes you have the items for get a grey
  background; ones you're missing items for get a red one.
- Search by name with the search bar, and use the toggle next to it to show all recipes or only the ones you can
  brew right now.
- Click a recipe to move its bottles and ingredient into the stand. Anything you're missing shows as a faded
  "ghost" item in the stand's empty slots.
- Hover a ghost item to see its name, like vanilla's crafting ghosts.
- Hover a recipe and press **B** to pin it, the same as in the vanilla recipe book.

### Potion Scroll

- Craft one from **paper + nether wart + glass bottle** (shapeless). The recipe unlocks once you pick up nether wart.
- Witches drop one 10% of the time when killed by a player, plus 5% per level of Looting (25% with Looting III).
- Use it to learn a random brewing recipe you don't know yet. A "New Brewing Recipe!" toast shows the potion.
  It's used up, unless you already know every recipe.
- Sneak while using it to read the whole stack: one recipe per scroll, with a single toast for the lot. If you
  run out of recipes to learn, the leftover scrolls stay in your hand.
- Unlocks only fill the book: you can still brew anything without unlocking it, like vanilla's recipe book.
- Unlocks are saved per player and kept through death.
- Operators can use `/brewbook unlockall` and `/brewbook reset` to test.

The mod must be installed on both the server and the client, because it adds an item.

## Building

Requires Java 21.

```
./gradlew build
```

Jars are written to `fabric/build/libs/` and `neoforge/build/libs/` (use the one without `-sources`/`-javadoc`).
The Fabric jar needs Fabric API.

To run a dev client: `./gradlew :fabric:runClient` or `./gradlew :neoforge:runClient`.

## Layout

Based on [jaredlll08/MultiLoader-Template](https://github.com/jaredlll08/MultiLoader-Template).

- `common/`: all the mod logic (pin list, overlay drawing, key handling, mixin accessors)
- `fabric/`: Fabric entry point and event hooks
- `neoforge/`: NeoForge entry point and event hooks
