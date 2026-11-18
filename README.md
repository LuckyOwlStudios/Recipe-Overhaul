# Pin Recipe

A client-side Minecraft 1.21.1 mod for **Fabric** and **NeoForge** that lets you pin recipes from the vanilla recipe book.
Pinned recipes show up beside your inventory whenever it's open, and never on the in-game HUD.

## How to use

1. Open your inventory (or a crafting table) and open the recipe book.
2. Hover over a recipe and press **B** to pin it. Press **B** on it again to unpin it.
3. Close the recipe book: the pinned recipe stays beside your inventory, with its ingredients and result.
   Ingredients you don't have in your inventory are highlighted red.
4. Click the **x** on a pinned recipe to unpin it.

- Up to 4 recipes can be pinned; pinning a 5th drops the oldest.
- The key can be changed in Options → Controls → Key Binds → Pin Recipe (it can also be bound to a mouse button).
- Pins are saved to `config/pinrecipe.json`, so they survive restarts.
- On small screens the panel switches to a narrower layout. If there's still no room while the recipe book is open,
  the pins hide until you close the book, so they never cover it.
- Only the survival inventory and the crafting table show pins; the creative inventory doesn't have a recipe book.

The mod is client-only: servers don't need it.

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
