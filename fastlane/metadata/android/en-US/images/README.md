# Store-listing images

All assets for the F-Droid store listing are present:

- `icon.png` — 512 × 512 launcher icon, composited from the app's adaptive-icon
  vector layers (`app/src/main/res/drawable/ic_launcher_{background,foreground}.xml`).
- `phoneScreenshots/1.png`–`3.png` — captured on-device (editor, share
  confirmation, settings).

If the launcher icon design changes, re-render `icon.png` from the updated
vector layers at 512 × 512.

See [docs/fdroid-submission.md](../../../../../docs/fdroid-submission.md) for how
these assets fit into the overall submission.
