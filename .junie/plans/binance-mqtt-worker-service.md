---
sessionId: session-261026-223814-xnp0
---

# Requirements

### Overview & Goals
Reduce the remaining bottom whitespace beneath all dashboard panels while preserving the existing compact layout, controls, and runtime behavior.

### Scope
**In scope**
- Reduce the shared vertical margin applied to `section` panels in `src/main/resources/dashboard.html`.
- Preserve the current side-by-side top layout, responsive stacking, panel content, and JavaScript behavior.

**Out of scope**
- HTTP endpoints, server code, configuration, worker, MQTT, and runtime update behavior.


# Technical Design

### Current Implementation
`src/main/resources/dashboard.html` uses one shared `section` CSS rule at line 12 for all panels. That rule currently applies `margin: .5rem 0`, leaving visible bottom spacing beneath the Controls, Runtime updates, and Status panels. The existing responsive layout and JavaScript behavior are already correct.

### Proposed Changes
Reduce the vertical margin in the shared `section` rule to a smaller value, such as `.25rem 0`, so all panels have less bottom whitespace. Do not change panel structure, element IDs, pending highlighting, polling, or endpoint interactions.

### Risks
The smaller margin must not cause panel content to overlap or make controls inaccessible at narrow viewport sizes.

# Testing

### Validation Approach
Inspect the dashboard at desktop and narrow viewport sizes and confirm the reduced panel spacing does not cause overlap or affect existing interactions.

### Key Scenarios
- All panels have visibly reduced bottom whitespace.
- Controls and Runtime updates remain adjacent on wide screens.
- Status remains below the top row and panels stack cleanly on narrow screens.
- Lifecycle controls, polling, pending highlights, and Apply behavior remain functional.

# Delivery Steps

### ✓ Stage 1: Reduce dashboard panel bottom spacing
The dashboard panels use a smaller shared vertical margin.
- Update the `section` rule in `src/main/resources/dashboard.html` from `.5rem 0` to a smaller margin such as `.25rem 0`.
- Preserve panel padding, layout structure, element IDs, and all JavaScript behavior.

### ✓ Stage 2: Verify compact layout and existing interactions
The dashboard is shorter without visual overlap or regressions.
- Inspect Controls, Runtime updates, and Status panel spacing at desktop and narrow widths.
- Confirm lifecycle buttons, checkbox synchronization, pending highlighting, polling, and Apply behavior remain unchanged.
