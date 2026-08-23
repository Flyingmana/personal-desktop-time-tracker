# Design: Timer Labels

## Approach

- Build on the existing `TimerLabel`, `TaskTimerEntry.labelIds`, `createLabel`, `assignLabels`,
  and `labelsFor` domain APIs. Labels remain global catalog records; task timers retain only their
  label IDs.
- Add a `Labels` app tab for catalog management. It shows a hierarchical label tree and an inline
  create form. Existing labels are display-only in this story; renaming and moving labels remain
  out of scope.
- Let users select zero or more labels while starting a timer and while editing an existing timer.
  A compact label picker uses checkbox rows grouped by hierarchy. The selected labels display as
  colored chips below the task text within the Task column of each timer row.
- Use a fixed set of accessible color swatches rather than free-form color entry. A label stores
  the selected swatch's hex color code; no selection stores `null`. Uncolored labels use the
  standard neutral chip style.
- Prevent label deletion when the label has child labels or is assigned to any task timer. The UI
  disables deletion and states the blocking reason. This avoids orphaned hierarchy references and
  timer-label links.

## Data Model And Operations

The current model remains the source of truth:

```kotlin
data class TimerLabel(
    val id: UUID,
    val name: String,
    val colorCode: String? = null,
    val parentId: UUID? = null,
)

data class TaskTimerEntry(
    // existing fields
    val labelIds: Set<UUID> = emptySet(),
)
```

- Extend `startTaskTimer` with an optional `labelIds: Set<UUID> = emptySet()` parameter. It must
  validate that every ID exists before creating the timer, so starting a labelled timer is one
  atomic `TrackerData` transition.
- Keep `assignLabels(data, taskEntryId, labelIds)` as the replacement operation for edits. It
  supports empty sets and validates every requested ID.
- Add `deleteLabel(data, labelId)`. It returns `LabelNotFound`, `LabelHasChildren`, or
  `LabelIsAssigned` failures where appropriate; otherwise it removes the label. The operation
  never reparents children or silently removes assignments.
- Add `updateLabelColor(data, labelId, colorCode)`. It validates the optional color code and
  replaces only that field, so existing labels can gain, change, or clear their color without
  renaming or reorganizing them.
- Add `isValidLabelColorCode(colorCode)` and reject malformed non-null values in `createLabel`
  with `InvalidLabelColorCode`. Although the UI emits only known swatches, this preserves the
  domain boundary for storage and future callers.
- Retain the existing three-level limit: roots are level 1, their children level 2, and their
  grandchildren level 3. The create form offers only labels at levels 1 or 2 as eligible parents.

## UI Flow

### Label Management

1. The user opens the `Labels` tab.
2. The tab renders root labels and their descendants indented one level at a time, with a color
   swatch and full hierarchy path available in semantics.
3. The create form contains a name input, an optional parent dropdown limited to eligible parents,
   color swatches plus a `No color` option, and a Create button.
4. A successful creation persists through `App`'s existing `onDataChanged` callback and appears
   immediately in the tree and label pickers.
5. Each label exposes an Edit color action. Its swatch dialog can select a palette color or clear
  the color; the update persists immediately.
6. Each label exposes a Delete action. It is disabled for labels with children or timer
   assignments, with the specific reason rendered next to the action. Eligible labels are deleted
   after a confirmation dialog.

### Assigning Labels

1. The new-timer row shows a Labels button beside Start. Selecting it opens the multi-select label
   picker; no selection is valid.
2. Starting a timer passes the chosen IDs to `startTaskTimer` and clears the draft selection after
   success.
3. Each task timer row has a Labels button in the Task column. Its picker initializes from that
   row's `labelIds`; applying replaces them through `assignLabels`.
4. Selected labels render below the timer text as small chips. A colored label uses its stored
   color as the chip accent; an uncolored label uses neutral outlined styling. Chip text always
   remains high contrast and includes the hierarchy path in its content description.
5. Attendance entries do not show label controls because they cannot carry task labels.

## Layout And Test Tags

- Preserve the existing `Task | Time | Action` timer-list columns. Chips stay in the Task column,
  so time and action alignment is unchanged.
- The Labels tab has `labelsTab`, `labelManager`, `labelNameInput`, `labelParentSelector`,
  `labelColorSelector`, `createLabelButton`, `labelTree`, and `labelDeleteButton` tags.
- Timer assignment controls use `newTimerLabelsButton`, `timerLabelsButton`, `labelPicker`,
  `labelPickerOption`, `applyLabelsButton`, and `timerLabelChip` tags. Row-specific UI tests use
  `onAllNodesWithTag` or semantic label text rather than relying on duplicate tag uniqueness.
- The picker presents each hierarchy path such as `Client > Project > Task` to distinguish labels
  with the same leaf name.

## Persistence

- The existing `labels` and `entry_labels` schema already persists label properties and task
  associations. Existing save behavior writes changed labels and replaces links for changed task
  entries.
- Add SQLDelight delete queries for an eligible label: remove its `entry_labels` rows first and
  then its `labels` row, within the global-label database transaction. The domain operation makes
  the link deletion a defensive no-op in normal use because assigned labels cannot be deleted.
- Update `SqlDelightTrackerStorage.save` to detect labels removed from the prior snapshot and
  execute those deletion queries. Entries remain in their weekly partitions; no unrelated entry
  data is rewritten.

## Tests

- Add Kotest coverage for starting a timer with zero/multiple labels, replacing assignments,
  invalid label IDs, valid hierarchy depth, invalid color codes, and every deletion rejection.
- Add Compose UI coverage for creating a root and nested label, color selection, the level-three
  parent constraint, selecting labels when starting/editing a timer, and colored/neutral chip
  rendering.
- Add persistence tests covering label deletion synchronization and label associations surviving a
  save/load round trip.

## Edge Cases

- No labels exist: both pickers show an empty state with a direct route to label management; timer
  creation and editing remain usable with no labels.
- A label's hierarchy depth is calculated from its parent chain, not its display order.
- Duplicate label IDs and missing parents continue to use existing domain validation.
- A label assignment update does not alter the timer's text, start, end, running state, or list
  ordering.
- Stored labels with a valid but currently unavailable color still render using that hex value;
  invalid stored values fall back to neutral styling when displayed.

## Out Of Scope

- Filtering, reporting, or search by label
- Renaming labels or reorganizing their parent after creation
- Bulk assignment, import/export, and custom free-form color entry