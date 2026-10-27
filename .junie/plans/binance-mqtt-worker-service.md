---
sessionId: session-261026-223814-xnp0
---

# Requirements

### Overview & Goals
Visually indicate unsaved runtime-update changes by highlighting only the checkbox control that was changed until `Apply` succeeds.

### Scope
**In scope**
- Highlight only the changed `systemLog` or `mqtt` control.
- Keep each control's highlight during polling and failed Apply requests.
- Restore each control's normal appearance after successful Apply.
- Preserve the existing runtime-only endpoint and checkbox behavior.

**Out of scope**
- API, YAML, worker, persistence, endpoint, or server changes.

### Functional Requirements
- Changing `systemLog` immediately highlights only the `systemLog` control; changing `mqtt` does the same for `mqtt`.
- Polling must not remove pending highlights or overwrite unsaved values.
- Successful Apply clears both pending states and restores normal appearance.
- Failed Apply keeps the selected values and highlights for the controls with pending changes.

# Technical Design

### Current Implementation
`src/main/resources/dashboard.html` contains the Runtime updates section around lines 30–34. A shared `updatesDirty` flag and section-level pending styling currently cover both controls; checkbox listeners are around lines 58–61, polling synchronization is in `refresh()`, and Apply clears the shared flag only after success.

### Key Decisions
- Wrap or target each checkbox as its own visual control and apply a pending class independently.
- Track pending state per setting, while preserving independent protection from polling overwrites.
- Remove each pending class only after a successful Apply; failed requests retain the relevant pending states.

### Proposed Changes
In `dashboard.html`, give the `systemLog` and `mqtt` controls individual highlight targets, replace the shared dirty flag with per-control pending state, and update polling so only controls without pending edits synchronize from the server. Apply both values through the existing endpoint, clearing pending styling only after success; no Java server changes are required.

### Risks
Independent pending state must remain consistent when one checkbox is applied together with another unchanged checkbox; the endpoint still receives both values, while the UI clears both only after the request succeeds.

# Testing

### Validation Approach
Build the project and manually verify the dashboard in a browser across polling intervals and Apply outcomes.

### Key Scenarios
- Initial load shows both controls normally.
- Changing only `systemLog` highlights only `systemLog`; changing only `mqtt` highlights only `mqtt`.
- Each highlight survives multiple refresh intervals without overwriting its selection.
- Successful Apply restores both controls' normal appearance.
- Failed Apply retains the changed values and corresponding highlights.

# Delivery Steps

### ✓ Stage 1: Add independent checkbox pending states
Only the runtime-update checkbox changed by the user receives the pending highlight.
- Update `src/main/resources/dashboard.html` so each checkbox has an individual visual wrapper or target.
- Replace the shared dirty styling with per-control pending flags and a helper that toggles each control's class.
- Preserve per-control values during polling when that control has unsaved edits.

### ✓ Stage 2: Synchronize independent states through Apply
The dashboard clears pending highlights only after the existing runtime update request succeeds.
- Continue submitting both checkbox values to `/live-updates`.
- Clear both pending flags and styles after successful Apply; preserve them after failure.
- Build and manually verify initial load, single-control edits, simultaneous edits, polling, and failed Apply scenarios.
