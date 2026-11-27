# ReadKeeper · Update progress

[Approved Android design](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=18-141)

The book-detail dialog opens with **Log reading time** off. Saving updates the reading position and preserves the recorded reading duration. Enabling the switch reveals separate date and time pickers for the start and end. A timed save uses the existing reading-session and Google Calendar flow.

The centered progress field uses Material 3 secondary container and on-secondary-container colors from the app theme.

## Compact Continue reading and shared progress

**Design status** `approved` in the local interactive review. The existing progress dialog is referenced above.

- The Android ReadKeeper dashboard shows up to five reading books, ordered by their latest update. A saved progress update refreshes the visible order immediately, including while the write is queued offline. All opens the full Reading shelf.
- A compact row contains the cover, title, and current progress. Its separate book-and-arrow button has a 32 dp visual background and 48 dp touch target.
- Home, Continue reading, and book details open the same progress dialog with the selected book's current page or percentage. Reading time is off on each open, and all three entry points use the same progress and optional session logic.
