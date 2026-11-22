# SHAHRVAND 1.0 — UI/UX prototype

## Scope
This branch contains the first Android UI prototype and a responsive admin review mockup. It is intentionally frontend-only.

## Included
- First-run profile form: first name, last name, age, optional profile photo.
- Four citizen app sections: Home, Missions, Ranking, Profile.
- Mission templates and a personal activity proposal form.
- Evidence selection through Android photo/video picker.
- Demo submission state, sample XP, sample Top 30 ranking, and sample achievements.
- Four ranking periods: today, week, month, year.
- Top 30 list with a pinned personal rank row.
- Tajik, Russian, and English interface labels.
- Responsive admin review mockup at `admin/index.html`.

## Explicitly not connected
- No Supabase.
- No API or remote database.
- No real account creation or authentication.
- No file upload; selecting evidence only updates the local UI state.
- No real XP credit, ranking calculation, or administrator decision.
- Demo state is in memory and may reset when the app process restarts.

## UI acceptance checklist
- [ ] Review registration and profile layout on a small Android screen.
- [ ] Verify all four app tabs and mission detail navigation.
- [ ] Verify image/video picker opens and returns a selected URI.
- [ ] Verify the four ranking period controls and pinned personal position.
- [ ] Verify Tajik, Russian, and English labels for all visible controls.
- [ ] Review admin queue layout and local approve/reject interactions.
- [ ] Run Android debug build and resolve compiler errors before calling this build test-ready.

## Next UI iteration
1. Review spacing, typography, colors, and Tajik ornament details on the target device.
2. Refine mission submission states and empty/error states.
3. Complete the admin review workflow UI.
4. Only after UI approval, design the backend contracts and persistence layer.
