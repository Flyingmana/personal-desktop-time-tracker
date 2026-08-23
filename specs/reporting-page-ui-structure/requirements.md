# Reporting Page UI Structure

## User Story

As a user, I want the reporting page to live in a clear, dedicated tab in the main app UI, so that
I can move between my timer list and report views without confusion.

## Acceptance Criteria

- **Given** the app is open, **when** I look at the main navigation, **then** I see a dedicated tab
  for the reporting page.
- **Given** the reporting page tab is selected, **when** the app is displayed, **then** the reporting
  page content is shown and the timer list is not the active primary view.
- **Given** the timer list tab is selected, **when** I switch to the reporting tab, **then** the app
  updates to show the reporting page without losing the underlying timer data.
- **Given** the reporting page is available, **when** I navigate between tabs, **then** the active tab
  is clearly marked as selected.

## Out of scope

- The detailed statistics shown on the reporting page itself (see reporting page story)
- Any reporting tab-specific filtering logic beyond the page selection itself
