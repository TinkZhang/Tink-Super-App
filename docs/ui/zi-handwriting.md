# 识字 · Handwriting

**Design status** `ready-for-review` · [Android handwriting draft](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=26-2)

The 识字 dashboard shows separate counts for 掌握汉字 and 会写汉字. The 会写汉字 card opens a dedicated screen through 添加会写的字. Recording a written character leaves its existing proficiency intact.

- Write one character at a time in the grid. 撤销一笔 removes the latest stroke and 清空 clears the board.
- Show candidate characters after a brief pause in writing. Save only after the user selects a candidate and confirms. Invalidate the selection if the user changes the strokes.
- Count each confirmed character once. If it already exists in the written set, indicate that it has been recorded and leave the total unchanged.
- Show when the first-use Chinese handwriting model is downloading or needs a retry. After download, recognition runs on the device. Store the confirmed character only.
- If recognition produces no candidate, keep the strokes visible so the user can continue writing or start over. Keep the current selection available for retry if saving fails.

The Figma draft includes the dashboard, the empty board at 320 × 640, a selected candidate, and an unrecognized character. It uses sample counts and a light Material 3 palette. The implementation will use the app's dynamic color scheme.
