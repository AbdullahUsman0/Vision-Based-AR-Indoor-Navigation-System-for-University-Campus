# Vision-Only AR Indoor Navigation - Execution Plan (Your Track)

This plan is based on:
- Proposal scope: vision-only indoor AR navigation on Android (ARCore), no extra hardware.
- Task board image: your track is the **AR / Android / Rendering** side (right side).

## 1) Your Responsibilities (from right-side track)

### P3c - ARCore camera setup (start now)
- Create Android app with ARCore session support.
- Show live camera preview.
- Read device pose from ARCore each frame (translation + rotation).
- Display pose values on screen for debugging.

### P3d - AR arrow overlay (start now)
- Draw a 3D directional arrow in camera view (OpenGL ES / Sceneform / Filament).
- Start with a hardcoded test pose and verify rendering appears correctly.
- Rotate arrow based on heading.

### P3e - Pose input + room labels (after P3d)
- Read a 6-DoF pose input (initially mock JSON).
- Project waypoint and room-label world coordinates to screen.
- Render label text at projected positions.

### P4d - ARCore motion tracking smoothing (after P3e)
- Use ARCore motion updates between localization refreshes.
- Smooth arrow movement using pose deltas + low-pass filtering.
- Handle intermittent localization updates from server (e.g., 1 update per ~30 frames).

## 2) Recommended Tooling (without Android Studio UI)

You can fully avoid Android Studio editor while still building/running Android:
- Code editor: Cursor only.
- Build system: Gradle Wrapper (`./gradlew`) from terminal.
- SDK management: command-line tools (`sdkmanager`, `adb`).
- Emulator/device run:
  - Preferred: physical Android phone + USB debugging + `adb install`.
  - Optional: lightweight emulator manager (Genymotion) if needed.

Note: ARCore needs Android SDK + platform tools installed, but not Android Studio IDE.

## 3) Team Integration Contract (conflict prevention)

To avoid end-of-project merge conflicts, freeze the interface early:
- Localization side (Abdullah) outputs **one pose JSON schema**.
- AR side (you) consumes that schema only; never parse ad-hoc formats.
- Keep this contract in `docs/pose_contract.md` and version it carefully.

Branch policy:
- `main`: stable, reviewed, no direct commits.
- `feature/ar-overlay`: your rendering and Android work.
- `feature/3d-mapping`: partner's COLMAP/HLoc work.
- `feature/integration`: only for connecting both tracks.

Merge rhythm:
- Short PRs every 1-2 days.
- Rebase/merge from `main` frequently.
- Integration smoke test every Friday.

## 4) What To Do First (next 7 days)

1. Finalize pose contract with your partner (`docs/pose_contract.md`).
2. Bootstrap ARCore app skeleton and camera session.
3. Add on-screen pose debug text (x,y,z + quaternion).
4. Render a static arrow in front of camera.
5. Switch static arrow to heading-aware arrow.
6. Add mock pose JSON reader to simulate server output.
7. Build a mini hallway demo: arrow points to one mock waypoint.

## 5) Definition of Done for each milestone

- P3c done when: app opens camera, ARCore tracking is active, live pose text updates every frame.
- P3d done when: visible 3D arrow is stable in scene and rotates with heading.
- P3e done when: given pose+waypoint JSON, labels and arrow render at correct relative positions.
- P4d done when: arrow remains smooth despite sparse localization updates.

## 6) Weekly Review Checklist

- Interface unchanged or versioned?
- Any direct commits to `main`? (should be no)
- Can app run with only mock JSON input?
- Can partner test integration without your full codebase?
- Are there regression notes/screenshots for each PR?
