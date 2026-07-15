# Store-listing images

Screenshots are present under `phoneScreenshots/` (`1.png`–`3.png`, captured on
a device). One asset is still missing:

## `icon.png` (to add)

A **512 × 512 PNG** launcher icon. The app ships only vector drawables
(`app/src/main/res/drawable/ic_launcher_*.xml`), so render a 512px PNG from the
adaptive icon — e.g. in Android Studio via **Image Asset Studio**, or export the
foreground+background layers composited at 512×512. Place it here as `icon.png`.

See [docs/fdroid-submission.md](../../../../../docs/fdroid-submission.md) for how
these assets fit into the overall submission.
