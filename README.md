# ItemsAdderBlocksInjector

Hack to inject blocks into the game blocks registry and change their texture on the client without mods.

⚠️ Requires ItemsAdder 3.1.5

### Example uses of this plugin
- Add compatibility to Iris world generator and similar plugins

### Notes:
- Seems FastAsyncWorldEdit breaks because it caches the NMS Block data by iterating the Material class of Bukkit, which doesn't contain the injected custom blocks.
There is no way to edit enums and push new ones, so this is fixable only by editing FAWE code.
- WorldEdit //undo and //copy won't work with the injected custom blocks, they would be reverted back to NOTE_BLOCK (or mushroom, TRIPWIRE, CHORUS_PLANT)
