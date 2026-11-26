# 识字 · Handwriting

**Design status** `approved` · [Android handwriting draft](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=27-2)

The 识字 dashboard shows separate counts for 会读汉字 and 会写汉字. Tapping a card opens its list. A plus icon in each card opens its add flow. The old add FAB and the separate list arrow are removed. The add-reading dialog uses larger input text and labels. Reading uses the existing proficiency-five records. Recording a written character leaves its existing proficiency intact.

- Write one character at a time in the grid. 撤销一笔 removes the latest stroke and 清空 clears the board.
- Show candidate characters after a brief pause in writing. Changing the strokes clears stale candidates. The batch flow below determines what is saved.
- Count each confirmed character once. If it already exists in the written set, indicate that it has been recorded and leave the total unchanged.
- Show when the first-use Chinese handwriting model is downloading or needs a retry. After download, recognition runs on the device. Store the confirmed character only.
- If recognition produces no candidate, keep the strokes visible so the user can continue writing or start over. Keep unconfirmed characters available for retry if saving fails.

The Figma draft includes the dashboard, both lists, the add-reading dialog, and the empty, recognized, and unrecognized board states. The empty board is shown at 320 × 640. It uses sample counts and a light Material 3 palette. The implementation will use the app's dynamic color scheme.

## Batch handwriting

**Design status** `approved` · [Batch handwriting design](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=32-2)

- Tapping a candidate appends it to 已选汉字 in selection order and clears the strokes and candidates so the user can write the next character.
- Show the queue below the candidates as bordered, tinted square tiles. Each tile is about one fifth as wide as the writing board. When the row fills, wrap the tiles onto the next line and keep 确认添加 accessible.
- Long-pressing a tile removes only that character. The tile shrinks and fades out, and the remaining tiles shift into place. Keep a character only once in the queue.
- Keep the primary button label 确认添加. Enable it when the queue has at least one character. One confirmation saves the queue through the existing idempotent single-character endpoint. If a request fails, keep that character and the remaining ones for retry. Characters already saved do not increase the total.

## Dashboard and handwriting polish

**Design status** `approved` · [Dashboard and handwriting design](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=43-2)

- Stack full-width Material 3 tonal cards for 会读汉字 and 会写汉字, with different semantic colors, a clear count and a plus action. Tap the card for its list. Keep the existing review characters. Group the two stat cards at the top with a fixed 16 dp gap. Use the full available width with 16 dp on either side. Place the two labeled story actions at the bottom with 32 dp below them, above the system navigation inset. Center the review card in the remaining height and retain its original row height. Let the page scroll when the content needs more height.
- Reserve the same vertical space for the candidate row when waiting for ink, recognizing, and showing candidates. Leave the empty row blank and keep retry controls available when recognition fails. Leave the selected-character area free of empty-state instructions, with room for the first row of tiles.
- The full-height dashboards were checked at 360 × 760 and 320 × 640 with the 32 dp story-action bottom inset. The draft also includes two compact handwriting states. Both handwriting states fit at 320 × 640 and position 已选汉字 at the same height.
