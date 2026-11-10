# Spatial App Design Doc: Gate Rush

Design-only handoff for `com.example.gaterush.p7c91a3e2`. Read `intent-brief.md` first. Executable authority: `design-spec.json`. Preview scope: `web_design_validation_only`. No Android, scaffold, Editor, git, or managed-workflow operations are owned by this handoff.

## 1. Frame

**Subject:** third-person toy sci-fi auto-run shooter with durability gates and combinable rewards. The player selects and drags the avatar left/right; running and firing are automatic.

**Viewer and context:** one seated or standing player, visible surrounding room, short sessions, hand/controller pointing. A near floating course is a candidate, not a physically accepted placement.

**Single job:** choose lanes to break rewarding gates, build firepower, avoid collisions, and defeat each finish target.

| Core task | Spatial affordance | Why it earns space | 2D counterfactual |
| --- | --- | --- | --- |
| Aim at gates and enemies | Depth, distance, direction | Stereo separation makes approaching targets and downstream shots distinct | A perspective runner on a monitor supports the same rules, but depth is only pictorial |
| Select and steer runner | Body, position | Capturing the avatar creates a direct steering relation; drag is lateral only | Mouse/touch dragging is efficient; space is not inherently better for this part, so no full-body gesture is added |
| Anticipate route choices | Time, depth | An approaching gate row gives visible time-to-contact | A scrolling board can do this; real depth adds a clearer separation of near action and far preparation |
| Read running and impacts | Motion, simulation | Separate legs, recoil, fragments and near/far roadside parallax show actual travel | Flat animation also works; the spatial scene adds object-relative motion, not a moving camera |
| Read HP, pause, retry | No extra spatial affordance | Keep these on one small AttachmentPanel | Ordinary 2D controls are already best; no dashboard, widget wall, or auxiliary navigation |

| Assumption | Confidence | Change if wrong |
| --- | --- | --- |
| Five local courses, about one minute each | High: accepted brief | Revise level event records, not the interaction model |
| No original title/video/screenshots are available | High | Compare against supplied evidence later; currently no pixel-perfect claim |
| Floor and near-course placement are comfortable | Low: unverified | Parent adjusts physical placement after device review |
| Models can be generated as separable matte toy parts | Medium | Parent repairs asset prompts, scale and pivots; required load failure blocks play |
| Optional audio can be omitted | Medium | Visual feedback remains complete; fallback case explicitly informs the player |

## 2. Explore

**Generation:** the user supplied summaries of three completed fresh independent explorations. This thread compares those summaries; it did not spawn subagents or claim fresh generation. No subagent capability is available here.

### A. Near Floating Mixed Stage Course

Full Space, Stage Mixed. The course is a real depth composition in the visible room. A single AttachmentPanel carries HUD and contextual actions. Select/capture lateral avatar dragging; shots and scenery move through depth. Signature: cyan running toy behind shattering yellow durability gates. Main risk: near-object comfort and hand-ray usability require device review.

### B. Large Shared Planar Projected Board

Shared Space, large Planar board. A projected course and native controls prioritize legibility at mid-distance. One page, no side navigation. Steering happens on the board projection. Main risk: space contributes little beyond a larger monitor; the runner can read as a diagram.

### C. Bounded Shared Volumetric Arcade Diorama

Shared Space, bounded Volumetric diorama. A toy course is inspected in depth; compact contextual controls sit near it. Main risks: clipping at boundaries, unclear ray/collider ownership and incoming content competing with the volume limits.

| Direction | Task efficiency | Spatial value | PICO comfort | Domain fit | Risk | Distinctiveness |
| --- | --- | --- | --- | --- | --- | --- |
| A | High: one captured drag | High: approaching lanes and shots | Candidate only; fixed root helps | High: action-first runner | Near placement/input needs review | Strong shatter-and-gait composition |
| B | High: readable projected board | Low/moderate: primarily pictorial depth | Candidate; larger occluding surface | Moderate: game becomes board-like | Lowest layout risk, reduced spatial payoff | Familiar flat runner |
| C | Moderate: volume selection complexity | High: compact genuine depth | Candidate; close inspection needed | High: toy arcade object | Highest clipping/streaming risk | Distinct diorama |

**Choose A.** It best preserves auto-running action, depth anticipation and roadside motion with minimal UI. **Reject B** because readability does not compensate for weaker stereo/path value or a board-like presentation. **Reject C** because a streaming five-level course and projectile/effect overflow are poorly matched to a bounded volume. These are alternatives in structure, interaction, reading distance and spatial form, not color swaps.

## 3. Plan

**Executable spec:** schema `1.0`, final package revision `12` (initial build revision `9`, three JSON-first patch rounds; last round is the user's Chinese-label request). This is distinct from the parent-managed design revision `9`, which this thread did not mutate. Implementation rules are stored in `nodes[course].props`, including geometry, input, game loop, gate semantics, pickups, animations, scenery, effects, all five event schedules, asset plan, load policy and preview rendering templates. Product title, instructions, rewards, level names and actions are Chinese.

### Visual Tokens

All native roles are unchanged same-name Vibrant references. Web uses the bundle's installed `system` fallback, not a custom override.

| Native role | Required value | Use |
| --- | --- | --- |
| fillPrimary | inherit Vibrant fillPrimary | Native button default |
| fillSecondary | inherit Vibrant fillSecondary | Available native surface role |
| fillTertiary | inherit Vibrant fillTertiary | Native progress track |
| fillLight | inherit Vibrant fillLight | Native light treatment |
| labelPrimaryLight | inherit Vibrant labelPrimaryLight | Native command content |
| labelPrimary | inherit Vibrant labelPrimary | Authored HUD text |
| labelSecondary | inherit Vibrant labelSecondary | Native supporting content |
| labelTertiary | inherit Vibrant labelTertiary | Native tertiary content |
| labelQuaternary | inherit Vibrant labelQuaternary | Native quaternary content |
| lightenHover | inherit Vibrant lightenHover | Native hover |
| lightenPressed | inherit Vibrant lightenPressed | Native pressed |
| error | inherit Vibrant error | Native error availability |
| alert | inherit Vibrant alert | Native warning availability |
| passable | inherit Vibrant passable | Native success availability |
| interaction | inherit Vibrant interaction | Native loading indicator |
| dividerLine | inherit Vibrant dividerLine | Unused; no authored content borders |

| Brand token | Hex | Domain use |
| --- | --- | --- |
| sky | #40BBDD | Runner, rails, shield |
| mint | #88D7B4 | Supply, spread, heal |
| coral | #F18482 | Foes, blast, warning lanes |
| sun | #F3C65D | Gates, fire rate, armour |
| violet | #AAA0D9 | Pierce and distant props |
| cream | #F7F2DE | Toy joints, labels, sparks |
| road | #B4C9CA | Matte physical course top |
| ink | #233B4A | Visors and domain number contrast |

Reward strips name the upgrade and amount, so color is never the only distinction. Durability has a `耐久` label and a countdown; HP names `生命`; boss names `核心`; shield/heal show names and ring/pulse shapes. The physical road is a bounded domain object, not an app-root panel fill.

**Type:** system face, exact `headlineSmall`, `titleMedium`, `titleSmall`, `bodyMedium`, `bodyMediumMultiline`, `bodySmall`, `labelLarge`. All ordinary authored text is below 20sp. The one reviewed exception is `headlineLarge` at 24sp for shooting-critical durability numbers in the course. Numbers must remain recognizable as the gate recedes and as damage changes them; the small named reward remains subordinate. Keep the surface checker warning rather than hiding it.

**Depth:** follows physical course distance, not list index. Core action is near the runner; far scenery is contextual. Stage root and viewer camera remain stable. No app-owned root fill, background, border, blur, material field, stepped card depths, TabBar, SideNavigation or extra Toolbar.

### Signature

The cyan toy has separately swinging legs, a recoiling cream blaster and visible upgrade attachments. Yellow gates chip and shatter into bounded fragments. Three roadside motion layers and flowing markings make travel observable while the HUD remains still.

### Surface Sizing

| Surface | Type | Content | Default | Minimum | Maximum |
| --- | --- | --- | --- | --- | --- |
| course | Stage Mixed, Full Space | Physical action | 0.84 x 0.60 x 1.80 m | Same | Same |
| attachment | Single Stage AttachmentPanel, Planar UI | HUD/contextual actions | 720 x 224 dp | 600 x 264 dp | 800 x 300 dp |

The course is a fixed-size gameplay object, not a user-resizable volume. Candidate floor Y is 0.75m, near edge 0.85m forward; avatar height 0.23m. `+Y` is up; `+X` is right; `+Z` is toward the wearer, so forward course travel is `-Z`. Runtime dp/physical conversions must use `PhysicalLengthConverter`, not a fixed ratio. The preview has explicitly declared default/minimum schematic viewports; these are not headset sizes.

Content, spacing, native component overhead, readable text floors and target envelope were checked internally. Native buttons retain `regular`; their authored action shells provide a 56dp minimum interaction envelope without changing native internals. Physical FOV, ray performance, occlusion and comfort remain unverified.

### Layout

One primary focus: runner, current target and route. Density: one HP value, one remaining time, one level label, one combined upgrade line, one feedback line. Menu actions replace the HUD in the same AttachmentPanel.

```text
          far pods / soft scenery
       [incoming drone] [supply]
            DURABILITY
               24
           Spread +1
        yellow gate / rails
             runner
       flowing markings / near lamps

       01 / First Break   HP   time   Pause
       upgrades / short action feedback
       hold runner, drag sideways, release keeps lane
```

### Gameplay Contract

Gate number = remaining durability. Bullets subtract effective damage, clamp at zero, remove the collider and grant the separately named reward exactly once immediately on break. Unbroken lane collision deals damage and grants nothing; bypassing is permitted. A crate is a passive auto-pickup; bullets pass through it. Spread, Pierce, Blast and Fire rate stack independently; Shield consumes charges; Heal restores 25 HP. Fire rate never changes forward speed. Caps give `MAX` feedback.

Each level resets HP, stacks, elapsed time and lateral X. Retry restarts the same level; Continue prepares the next. All levels have 60-second deadlines and required finish targets. Tutorial teaches first break; Forked Fire introduces alternatives; Moving Hazard adds obstacles and faster drones; Armour Line rewards pierce/blast; Core Breaker adds a telegraphed machine-core fight. Full schedules and damage values are JSON, not prose-only promises.

Capture begins only on avatar pick, preserves grab offset, then tolerates leaving the silhouette. Release/cancel keeps lateral X. Pause cancels capture and freezes time without recentering. Resume discards excess wall time. Simultaneous hits are deduplicated; break processing precedes collision. The runner advances slightly in the first seconds and has a continuing hip-driven gait against a scrolling course; neither root nor camera translates.

### State Graph

| State | Task/focus | Components | Data/entry/exit |
| --- | --- | --- | --- |
| loading | Prepare required kit | Native progress, loading text | Sample/load result -> ready or load-error |
| ready | Read level, start | Start, Rules, Exit | Current level; Start resets level |
| running | Steer and build weapon | Course, HP/time/upgrades, Pause | Fixed-step loop; automatic clear/failure |
| paused | Hold progress | Resume, Rules, Restart, Exit | Preserve lane/HP/stacks; destructive actions confirm |
| failed | Retry quickly | Retry course, Exit | HP-zero or surviving finish target |
| won | Continue | Continue, Exit | Clear summary; level 5 continues to complete |
| complete | End campaign | Play again, Exit | All five courses completed |
| rules | Learn input/rewards | Rules text, Back | Back returns to ready or paused origin |
| load-error | Recover required kit | Retry assets, Exit only | No playable primitive fallback |
| exited | End session | Return to start | Parent handles actual app exit |

Every transition names an exact trigger and action in JSON. Restart/leave an active paused run require a native alert dialog; cancel/Escape retains progress. Optional audio fallback stays playable and is distinct from required-model failure.

### Components And Map

| Element/task | Source | SpatialUI mapping | API inspected | Public event |
| --- | --- | --- | --- | --- |
| Contextual commands | State graph | sui-button, regular | js/button.js | click-action |
| Required loading | assets.required | sui-circular-progress | js/progress.js | Determinate/indeterminate attributes |
| Restart/exit confirmation | Pending transition.confirmation | sui-alert-dialog with native button slots | js/dialogs.js | dismiss-request, click-action |
| Authored labels | Static JSON or bindings | sui-text | js/text.js | variant/color/content |
| Runner, course and effects | course.props + game state | Canvas domain_visual | Declared toy-course renderer | avatar-drag, level-cleared, runner-failed, load result, pause-request |

No icon-only action or explicit icon slot is needed. Text-labeled native actions avoid ambiguous icons; no custom SVG sources are declared. Canvas is justified because SpatialUI has no course/runner simulation component. It does not replace any standard UI control.

### Asset Boundary

RunnerTorso, RunnerLeg reused L/R, Blaster, GateFrame, HoverEnemy, SupplyCrate and Lamp each have separate prompts, physical target dimensions, pivot requirements, material constraints and parent acceptance criteria in JSON. Editor primitives provide road/rails/markings/scenery, armour/Boss attachments and pooled effect templates. No model was generated or accepted by this designer. The parent reports that six candidates are being reused and corrected torso generation is owned by Editor Run `run-736be0bd-b7bf-43b7-827c-8672ca94d7a6`; the prior torso became a whole figure. The parent also reports releasing the old supervisor/orphan Editor after `WORKFLOW_RUNTIME_RESUME_INVALID`. These are parent-reported facts, not independently verified asset or workflow evidence. The offline preview's procedural geometry is schematic, never asset acceptance evidence. All seven required models must succeed before runtime Start; failure offers only retry/exit.

## 4. Critique Before Build

**Generic-default simulation:** a large dark dashboard with fire/upgrade buttons and a motionless character would satisfy copy while missing the action. The design removes manual firing and dashboard navigation, concentrates UI in one contextual panel, makes gait and parallax mandatory, and separates gate durability from reward value.

| Hard check | Initial result | Evidence |
| --- | --- | --- |
| Three real alternatives and rejected reasons | Pass | Supplied A/B/C compare form, IA, input and distance |
| Core-task counterfactuals | Pass | Frame table; UI kept 2D where appropriate |
| Form/scale and attachment justification | Pass, physical assumption pending | A needs depth; one panel earns HUD/control role |
| Domain-swap | Pass | Lane routing, durability gates and toy gait would not suit a dashboard |
| Component task/data and native map | Pass | Map and all node bindings |
| Native roles | Pass | All 16 unchanged |
| Schema/references/bindings/acyclic tree | Pass | Isolated validation script |
| Surface discipline | Pass with justified type warning | Zero errors, no authored app surfaces |
| Repeated depth | Pass | Physical distance/motion only |
| Single focus | Pass | Route/runner/target, no competing panels |

## 5. Build Coverage Manifest

Coverage is rebuilt from JSON, not from what happens to appear. Stable selectors are `[data-surface-id]`, `[data-state]`, and each exact node ID. Automatic game events and actions share the state engine; data cases use non-visible review tooling.

| Kind | JSON ID/path | Implementation/evidence |
| --- | --- | --- |
| Surface | `stage` | [data-surface-id="stage"], declared default/min/max |
| Surface | `attachment` | [data-surface-id="attachment"], declared default/min/max |
| State | `loading` | #app[data-state="loading"], actual trigger path |
| State | `ready` | #app[data-state="ready"], actual trigger path |
| State | `running` | #app[data-state="running"], actual trigger path |
| State | `paused` | #app[data-state="paused"], actual trigger path |
| State | `failed` | #app[data-state="failed"], actual trigger path |
| State | `won` | #app[data-state="won"], actual trigger path |
| State | `complete` | #app[data-state="complete"], actual trigger path |
| State | `rules` | #app[data-state="rules"], actual trigger path |
| State | `load-error` | #app[data-state="load-error"], actual trigger path |
| State | `exited` | #app[data-state="exited"], actual trigger path |
| Transition | `asset-ready` | course: required-load-ready |
| Transition | `asset-fail` | course: required-load-failed |
| Transition | `start` | start: click-action |
| Transition | `pause` | pause: click-action |
| Transition | `suspend` | course: pause-request |
| Transition | `resume` | resume: click-action |
| Transition | `rules-open` | rules-open: click-action |
| Transition | `rules-paused` | rules-paused: click-action |
| Transition | `rules-back` | rules-back: click-action |
| Transition | `clear` | course: level-cleared |
| Transition | `fail` | course: runner-failed |
| Transition | `retry` | retry: click-action |
| Transition | `next` | next: click-action |
| Transition | `play-again` | play-again: click-action |
| Transition | `retry-assets` | retry-assets: click-action |
| Transition | `return` | return: click-action |
| Transition | `restart` | restart: click-action, native confirmation |
| Transition | `exit-ready` | exit-ready: click-action |
| Transition | `exit-paused` | exit-paused: click-action, native confirmation |
| Transition | `exit-failed` | exit-failed: click-action |
| Transition | `exit-won` | exit-won: click-action |
| Transition | `exit-complete` | exit-complete: click-action |
| Transition | `exit-load-error` | exit-load-error: click-action |
| Action | `drag` | custom: drag |
| Action | `confirm-cancel` | custom: cancel-confirmation |
| Action | `confirm-ok` | custom: confirm-transition |
| Action | `asset-ready` | transition: none |
| Action | `asset-fail` | transition: none |
| Action | `start` | transition: reset-level |
| Action | `pause` | transition: none |
| Action | `suspend` | transition: none |
| Action | `resume` | transition: none |
| Action | `rules-open` | transition: save-return-state |
| Action | `rules-paused` | transition: save-return-state |
| Action | `rules-back` | transition: restore-return-state |
| Action | `clear` | transition: none |
| Action | `fail` | transition: none |
| Action | `retry` | transition: reset-level |
| Action | `next` | transition: next-or-complete |
| Action | `play-again` | transition: reset-campaign |
| Action | `retry-assets` | transition: retry-assets |
| Action | `return` | transition: reset-campaign-and-load |
| Action | `restart` | transition: reset-level |
| Action | `exit-ready` | transition: stop-run |
| Action | `exit-paused` | transition: stop-run |
| Action | `exit-failed` | transition: stop-run |
| Action | `exit-won` | transition: stop-run |
| Action | `exit-complete` | transition: stop-run |
| Action | `exit-load-error` | transition: stop-run |
| Node | `course` | #course: toy-course |
| Binding | `course.level` | game.level: normal/fallback/error samples |
| Binding | `course.hp` | game.hp: normal/fallback/error samples |
| Binding | `course.upgrades` | game.upgrades: normal/fallback/error samples |
| Binding | `course.elapsed` | game.elapsed: normal/fallback/error samples |
| Binding | `course.models` | assets.required: normal/fallback/error samples |
| Binding | `course.audio` | assets.audio: normal/fallback/error samples |
| Node | `stage-root` | #stage-root: layout |
| Node | `ready` | #ready: layout |
| Node | `ready-title` | #ready-title: text |
| Node | `ready-copy` | #ready-copy: text |
| Node | `level-title` | #level-title: text |
| Binding | `level-title.content` | game.levelTitle: normal/fallback/error samples |
| Node | `optional-audio` | #optional-audio: text |
| Binding | `optional-audio.content` | assets.audioNotice: normal/fallback/error samples |
| Node | `ready-actions` | #ready-actions: layout |
| Node | `start` | #start: sui-button |
| Node | `rules-open` | #rules-open: sui-button |
| Node | `exit-ready` | #exit-ready: sui-button |
| Node | `hud` | #hud: layout |
| Node | `hud-top` | #hud-top: layout |
| Node | `hud-level` | #hud-level: text |
| Binding | `hud-level.content` | game.levelTitle: normal/fallback/error samples |
| Node | `hud-health` | #hud-health: text |
| Binding | `hud-health.content` | game.hpLabel: normal/fallback/error samples |
| Node | `hud-time` | #hud-time: text |
| Binding | `hud-time.content` | game.timeLabel: normal/fallback/error samples |
| Node | `pause` | #pause: sui-button |
| Node | `upgrade-line` | #upgrade-line: text |
| Binding | `upgrade-line.content` | game.upgradeLabel: normal/fallback/error samples |
| Node | `feedback` | #feedback: text |
| Binding | `feedback.content` | game.feedback: normal/fallback/error samples |
| Node | `drag-hint` | #drag-hint: text |
| Node | `paused` | #paused: layout |
| Node | `pause-title` | #pause-title: text |
| Node | `pause-copy` | #pause-copy: text |
| Node | `pause-actions` | #pause-actions: layout |
| Node | `resume` | #resume: sui-button |
| Node | `rules-paused` | #rules-paused: sui-button |
| Node | `restart` | #restart: sui-button |
| Node | `exit-paused` | #exit-paused: sui-button |
| Node | `failed` | #failed: layout |
| Node | `fail-title` | #fail-title: text |
| Binding | `fail-title.content` | game.failureTitle: normal/fallback/error samples |
| Node | `result-copy` | #result-copy: text |
| Binding | `result-copy.content` | game.resultLabel: normal/fallback/error samples |
| Node | `fail-actions` | #fail-actions: layout |
| Node | `retry` | #retry: sui-button |
| Node | `exit-failed` | #exit-failed: sui-button |
| Node | `won` | #won: layout |
| Node | `won-title` | #won-title: text |
| Node | `won-copy` | #won-copy: text |
| Binding | `won-copy.content` | game.resultLabel: normal/fallback/error samples |
| Node | `won-actions` | #won-actions: layout |
| Node | `next` | #next: sui-button |
| Node | `exit-won` | #exit-won: sui-button |
| Node | `complete` | #complete: layout |
| Node | `complete-title` | #complete-title: text |
| Node | `complete-copy` | #complete-copy: text |
| Node | `complete-actions` | #complete-actions: layout |
| Node | `play-again` | #play-again: sui-button |
| Node | `exit-complete` | #exit-complete: sui-button |
| Node | `rules` | #rules: layout |
| Node | `rules-title` | #rules-title: text |
| Node | `rules-copy` | #rules-copy: text |
| Node | `rules-back` | #rules-back: sui-button |
| Node | `loading` | #loading: layout |
| Node | `loading-text` | #loading-text: text |
| Node | `loading-progress` | #loading-progress: sui-circular-progress |
| Binding | `loading-progress.aria-label` | assets.loadingLabel: normal/fallback/error samples |
| Node | `load-error` | #load-error: layout |
| Node | `load-error-title` | #load-error-title: text |
| Node | `load-error-message` | #load-error-message: text |
| Binding | `load-error-message.content` | assets.errorMessage: normal/fallback/error samples |
| Node | `load-error-actions` | #load-error-actions: layout |
| Node | `retry-assets` | #retry-assets: sui-button |
| Node | `exit-load-error` | #exit-load-error: sui-button |
| Node | `exited` | #exited: layout |
| Node | `exit-text` | #exit-text: text |
| Node | `return` | #return: sui-button |
| Node | `confirm-dialog` | #confirm-dialog: sui-alert-dialog |
| Node | `confirm-title` | #confirm-title: text |
| Binding | `confirm-title.content` | ui.confirmation.title: normal/fallback/error samples |
| Node | `confirm-body` | #confirm-body: text |
| Binding | `confirm-body.content` | ui.confirmation.body: normal/fallback/error samples |
| Node | `confirm-buttons` | #confirm-buttons: layout |
| Node | `confirm-cancel` | #confirm-cancel: sui-button |
| Binding | `confirm-cancel.text` | ui.confirmation.cancelLabel: normal/fallback/error samples |
| Node | `confirm-ok` | #confirm-ok: sui-button |
| Binding | `confirm-ok.text` | ui.confirmation.confirmLabel: normal/fallback/error samples |
| Responsive | `attachment-compact` | Applied at minimum attachment |
| Asset | `runner-torso` | model: planned://spatial-editor/RunnerTorso; parent-owned acquisition, schematic preview only |
| Asset | `runner-leg` | model: planned://spatial-editor/RunnerLeg; parent-owned acquisition, schematic preview only |
| Asset | `blaster` | model: planned://spatial-editor/Blaster; parent-owned acquisition, schematic preview only |
| Asset | `gate-frame` | model: planned://spatial-editor/GateFrame; parent-owned acquisition, schematic preview only |
| Asset | `hover-enemy` | model: planned://spatial-editor/HoverEnemy; parent-owned acquisition, schematic preview only |
| Asset | `supply-crate` | model: planned://spatial-editor/SupplyCrate; parent-owned acquisition, schematic preview only |
| Asset | `lamp` | model: planned://spatial-editor/Lamp; parent-owned acquisition, schematic preview only |
| Asset | `optional-audio` | audio: planned://local-synth/soft-toy-fx; parent-owned acquisition, schematic preview only |

## 6. Critique Again

The final browser pass uses isolated Chrome and actual pointer/button interactions. Only the 60-second clock and lifecycle pause signal are accelerated/simulated through declared non-visible hooks; the document does not claim headset or real-time play-balance acceptance.

**Independent coverage rebuild:** 10/10 states, 68/68 nodes, 23/23 transitions, 26/26 actions, 22 bindings, all responsive rules. No missing item. All ten states were captured at default and minimum sizes. Default/minimum action, rules, error, failed and confirmation views were visually inspected; the Boss scene was visually inspected at default size.

| Quality check | Result | Evidence |
| --- | --- | --- |
| Clear task, justified form, alternatives | Pass | Frame/Explore and action-first course |
| Schema, references, bindings, acyclic ownership | Pass | `/private/tmp/gaterush-design/structural-check.json` |
| External/embedded JSON and complete bundle | Exact | Raw spec SHA-256 below; unchanged vendored JS inline |
| Native component registration and event paths | Pass | sui-text/button/progress/dialog; real public events |
| App-authored root/background/borders | Pass | No app surfaces; system window background; zero native duplicates |
| Environment | Pass | body = rgb(202, 202, 202); background-image = none |
| Offline | Pass | Zero external requests and zero browser page errors |
| Drag and retained lane | Pass | Actual pointer capture/drag/release; no later recenter |
| Gate reward semantics | Pass | Durability reached zero; one grant, no duplicate after extra ticks |
| Pause/rules/restart/exit | Pass | Frozen X/time; return scope preserved; confirm/cancel/Escape |
| Normal/fallback/required failure | Pass | Optional audio notice; error offers only retry/exit; failed assets stay failed after Return |
| Five levels | Pass, simulated clock only | All finish targets defeated at 60.0167s; runner HP outcomes 100/100/100/100/80 |
| Minimum composition | Pass | 600x480 schematic course; 600x264 AttachmentPanel; document width 648 at viewport 648 |
| Automatic lifecycle pause | Event branch passed | Declared hidden systemPause hook; natural headless tab visibility not verified |
| Typography ceiling | Reviewed warning | Only durability numbers explicitly use headlineLarge 24sp |
| Signature, hierarchy, readability | Pass for Web design | Rear-view gait, recoil, shatter/death particles, scenery parallax, distinct number/reward labels |
| User-facing language | Pass | Chinese title, instructions, level/reward names and native button labels |

**Patch log**

| Round | Problem | Target | Result | Revision |
| --- | --- | --- | --- | --- |
| 1 | Narrow preview overflow; front-reading avatar; too-short Boss exposure | JSON preview viewport/rear assembly and Boss HP | No document overflow; back-facing runner; visible telegraphed Boss phase | 10 |
| 2 | Coverage evidence and inspectable load/lifecycle branch | JSON review fields, load delay, hidden declared lifecycle hook | Every state/action/transition reachable and checked | 11 |
| 3 | User explicitly requested Chinese product labels | JSON labels, explanations, rewards, levels, buttons and sample data | Exact spec-driven Chinese preview, rechecked at both sizes | 12 |
| Renderer corrections | Frozen failure muzzle flash; minimum billboard text spacing | Generic declared course renderer only | Muzzle appears only while running; native-token line heights prevent label overlap | No independent JSON design changes |

**Verification tooling:** `/private/tmp/gaterush-design`. Browser checks/screenshot captures are local design-review evidence, not assets. `full-check.cjs` uses actual browser pointer/buttons and explicit clock/sample/lifecycle hooks. `validate.cjs` validates schema, references, bindings, tree ownership, exact embedded JSON and exact inline bundle. The installed surface checker reports zero errors and one retained typography warning.

**Known limitations:** natural visibility/suspension behavior in headless Chrome was not observed; PICO input/raycast, physical size/FOV/occlusion/comfort, real-time play balance, device performance, animation on generated meshes and model acceptance were not tested. The preview does not acquire the planned models or audio.

**Design receipt:** `design_escalation_receipt.json`, `status=designer_passed`, all four pre-gates true. Local design handoff only; no managed-workflow submission.

**Pinned spec:** package revision `12`; SHA-256 `f21721409fc9330484f024a96a5041a05033df02d4cd28463b57df294c9cde50`. Parent-managed revision `9` remains unchanged; parent must pin the actual package revision/hash when consuming it.

**Verdict:** `pass` for executable Web design handoff. No unresolved design-package blocker. Parent-owned model/runtime/device acceptance remains outstanding.


**Device validation:** `not_performed`. Web proves design logic/layout only, not physical comfort, model quality, actual PICO input, runtime animation, occlusion, audio or performance.

**Ownership:** parent owns managed run `run-d89e5dc4-9e28-47bf-a7b0-f39e873c5820`, managed design revision 9, Editor/model work and workflow submission. No main-workflow mutation or submission is performed by the designer.

## Native runtime revisions

The earlier revision 12 preview and hash above are historical design evidence. The native implementation now follows local executable spec revision 15, including the accepted 0.2 environment/audio and 0.3 proportion/density updates.

Revision 15 / APK 0.4 adds ICE, CHAIN and BURN rewards to all five retained courses. They stack independently with scatter, pierce and explosion. Enemy-only ice slows movement, chain visits the nearest distinct enemies once per bullet, and burn refresh keeps the next damage tick. Death clears active status; gates and crates keep their immediate, single reward semantics. Six projectile core styles share actual sweep dimensions with the simulation; electric trails and ice collars also show combined elements. Frost/flame markers remain outside imported body measurement. Enemy and arc pools are bounded at 28 and 24.

Three recurring combat WAVs (shot, impact, enemy) are replaced with noise-free midrange tones and lower playback gains. Other cues and focus/pause mute behavior are retained. Native tests, audio spectrum reports and device observations for this revision are stored in `/private/tmp/gaterush-04/`; the old browser preview is not evidence of these new native effects.
