# SHAHRVAND — Build and Deployment

**Applies to:** `uiux/v1-prototype`  
**Inspected implementation commit:** `108c0c2ea45eeb7f51dbab5b7ff44cbe5ef091f3`  
**Documentation snapshot:** 2026-11-24

## 1. Branch selection

The product source is on `uiux/v1-prototype`, not the default branch `main`.

Before building, check out the implementation branch:

```bash
git clone https://github.com/Khayom1/SHAHRVAND.git
cd SHAHRVAND
git switch uiux/v1-prototype
```

If the repository is already cloned, fetch and switch:

```bash
git fetch origin
git switch uiux/v1-prototype
git pull --ff-only origin uiux/v1-prototype
```

Do not run the product build from `main` and expect the Android module or browser prototype to exist there.

## 2. Android debug APK

### Requirements

- JDK 17.
- Gradle 8.13 (the version configured in the Android CI workflow).
- Android SDK with platform 36 installed.
- Android Gradle Plugin 8.13.2 and Kotlin plugin 2.2.21 are declared by the project plugins.

There is no Gradle wrapper (`gradlew`) in the inspected branch. Use an installed Gradle 8.13 executable or open the project in Android Studio with a compatible Gradle installation.

### Build

From the repository root:

```bash
gradle :app:assembleDebug
```

Expected output APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

The Android CI workflow runs this same Gradle task and uploads the APK as the `shahrvand-ui-debug-apk` artifact.

### Runtime permissions

The manifest declares precise location and location foreground-service permissions. The app requests `ACCESS_FINE_LOCATION` at runtime before starting a GPS mission. GPS must be enabled on the test device.

The code path for GPS tracking should be validated on a physical Android device. A successful APK build does not test location quality, background execution, notification behaviour, manufacturer power restrictions, or battery use.

## 3. Browser prototype

The browser frontend is a standalone static file:

```text
web/index.html
```

It has no npm dependencies or build step in the inspected tree. It can be opened directly in a browser or served from the repository root using a static file server. Browser state is stored locally in the active browser profile.

The admin mock is a separate file:

```text
admin/index.html
```

Open it separately for UI review. It uses hard-coded sample submissions and is not connected to the browser client, Android app, or a server.

## 4. GitHub Actions workflows

### On `uiux/v1-prototype`

| Workflow | Trigger | Steps |
|---|---|---|
| [`android-ui.yml`](../.github/workflows/android-ui.yml) | Push to `uiux/**`; pull request targeting `main` | Checkout, set up JDK 17, set up Gradle 8.13, run `gradle :app:assembleDebug`, upload debug APK. |
| [`web-preview.yml`](../.github/workflows/web-preview.yml) | Push to `uiux/**` when `web/**` or the workflow file changes; manual dispatch | Extract inline JavaScript, run `node --check`, upload `web/` as a Pages artifact, deploy Pages. |

The web-preview workflow uploads only `web/`. It does not publish `admin/` or `docs/`.

Observed results:
- Android build run [#38](https://github.com/Khayom1/SHAHRVAND/actions/runs/45174529186) succeeded on commit `108c0c2`.
- Browser preview deployment run [#10](https://github.com/Khayom1/SHAHRVAND/actions/runs/45139722485) succeeded on commit `1b5b358`. This is the latest successful browser-preview run observed during the inspection; it is not a run for the current branch-head commit.

### On `main`

Two Pages workflows exist and both trigger on pushes to `main`:

- [`jekyll-gh-pages.yml`](../.github/workflows/jekyll-gh-pages.yml) builds the repository with Jekyll and deploys the generated site.
- [`static.yml`](../.github/workflows/static.yml) uploads the repository root as a static Pages artifact and deploys it.

On the inspected main-branch commit `c2256cc`, Jekyll run [#2](https://github.com/Khayom1/SHAHRVAND/actions/runs/45138421459) succeeded. Static run [#1](https://github.com/Khayom1/SHAHRVAND/actions/runs/45138421430) failed at the Pages deploy step: the logs report that no artifact named `github-pages` could be found, although the preceding upload step reported success.

These are two competing Pages workflow definitions on `main`, in addition to the prototype branch's browser-preview publisher. Their presence makes the intended canonical site source ambiguous. Retain one deliberate Pages publishing workflow and disable/remove the unused publishers as a separate, reviewed change.

## 5. GitHub Pages settings

The repository's Settings → Pages screen was not readable through the available inspection connection, so the currently selected Pages source setting is **not independently verified** here.

For the `web-preview.yml` workflow to be the intended publisher, the repository Pages source should be configured for **GitHub Actions**:

1. Open Repository → Settings → Pages.
2. Under **Build and deployment**, set **Source** to **GitHub Actions**.
3. Keep the intended Pages workflow enabled.
4. Check the workflow run and deployment environment after publishing.

The expected project-site URL recorded in the earlier prototype notes is:

```text
https://khayom1.github.io/SHAHRVAND/
```

The URL could not be independently fetched during this inspection. A successful historical workflow run is evidence that a deployment workflow completed at that time; it is not a guarantee that the current deployment is reachable or reflects the current branch head.

## 6. Current CI coverage

The Android workflow currently compiles and packages a debug APK. It does not run a unit-test or Android instrumentation-test task. No test source set was present in the inspected tree.

The web workflow checks JavaScript syntax with Node.js and then deploys the static page. It does not run accessibility checks, browser interaction tests, security tests, or end-to-end scenarios.

For this reason, use the term **CI build passed** rather than **application fully tested** when reporting the current status.
