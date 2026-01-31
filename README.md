# ItemsAdderBlocksInjector

Hack to inject blocks into the game blocks registry and change their texture on the client without mods.

⚠️ Requirements:
- ItemsAdder 3.1.5+
- Spigot 1.18.2+
- ViaVersion
- ProtocolLib

### Example uses of this plugin
- Add compatibility to Iris world generator and similar plugins

### Notes:
- if you’re using FastAsyncWorldEdit, make sure to update to version 2.14.0 or newer to avoid this issue.
FAWE previously had some issues which have been recently fixed. Read more [here](https://github.com/ItemsAdder/ItemsAdderBlocksInjector/issues/1).
- WorldEdit //undo and //copy won't work with the injected custom blocks, they would be reverted back to NOTE_BLOCK (or mushroom, TRIPWIRE, CHORUS_PLANT)
- Seems that it's not possible to intercept the tab complete of vanilla commands, so I cannot inject the autocompletion for the custom blocks.
