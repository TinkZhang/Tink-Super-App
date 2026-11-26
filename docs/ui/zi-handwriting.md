# 识字 · Handwriting

**Design status** `approved` · [Android handwriting draft](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=27-2)

The 识字 dashboard shows separate counts for 会读汉字 and 会写汉字. Tapping a card opens its list. A plus icon in each card opens its add flow. The old add FAB and the separate list arrow are removed. The add-reading dialog uses larger input text and labels. Reading uses the existing proficiency-five records. Recording a written character leaves its existing proficiency intact.

- Write one character at a time in the grid. 撤销一笔 removes the latest stroke and 清空 clears the board.
- Show candidate characters after a brief pause in writing. Save only after the user selects a candidate and confirms. Invalidate the selection if the user changes the strokes.
- Count each confirmed character once. If it already exists in the written set, indicate that it has been recorded and leave the total unchanged.
- Show when the first-use Chinese handwriting model is downloading or needs a retry. After download, recognition runs on the device. Store the confirmed character only.
- If recognition produces no candidate, keep the strokes visible so the user can continue writing or start over. Keep the current selection available for retry if saving fails.

The Figma draft includes the dashboard, both lists, the add-reading dialog, and the empty, recognized, and unrecognized board states. The empty board is shown at 320 × 640. It uses sample counts and a light Material 3 palette. The implementation will use the app's dynamic color scheme.
