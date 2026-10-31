# NexusPortal Layer — renderer

Open `index.html`. The demo draws sample commands with a transparent canvas and offers a floating `%` toggle, pause, clear and local annotation actions.

## Annotation envelope (draft v0)

```json
{"type":"highlight","id":"demo-1","x":0.12,"y":0.2,"w":0.5,"h":0.12,"ttlMs":10000}
```

Coordinates are normalized to the viewport, from 0 to 1. Supported types: `highlight`, `arrow`, `circle`, `text`, `symbol`, `clear`. `arrow` uses `x,y,x2,y2`; `circle` uses `x,y,r`. Text and symbols use `value` and optional `direction` (`auto`, `ltr`, `rtl`). The renderer validates types, bounds, length and TTL. This is a **display-only** schema; it cannot invoke actions in another app.

## Architectural boundary

Browser demo is interactive and therefore not touch-through. In Android, the native shell must put the annotation canvas in a separate non-touchable overlay window, while the `%` control is its own touchable overlay. User consent and capture belong to native code. Never expose an unrestricted native JavaScript bridge to remote pages.
