# Store-listing images (to be added)

These binary assets can't be scaffolded from source — they must be produced and
committed before the F-Droid store listing is complete. F-Droid reads whatever
is present here; missing images just mean a listing without them.

## `icon.png` (required for a good listing)

A **512 × 512 PNG** launcher icon. The app ships only vector drawables
(`app/src/main/res/drawable/ic_launcher_*.xml`), so render a 512px PNG from the
adaptive icon — e.g. in Android Studio via **Image Asset Studio**, or export the
foreground+background layers composited at 512×512. Place it here as `icon.png`.

## `phoneScreenshots/`

At least one screenshot; two or three is better. Capture the three surfaces:

1. The share confirmation screen (before/after diff).
2. The in-app Paste Clean / Paste Raw editor.
3. The Settings screen (or the Quick Settings tile in action).

Requirements: PNG or JPG, portrait. Filenames are shown in **sorted order**, so
name them `1.png`, `2.png`, `3.png`. Save them under
`phoneScreenshots/` alongside this note.

See [docs/fdroid-submission.md](../../../../../docs/fdroid-submission.md) for how
these fit into the overall submission.
