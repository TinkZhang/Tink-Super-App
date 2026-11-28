# 识字 · 汉字掉落测试游戏

**Design status** `approved`

HTML prototype: [docs/ui/drafts/zi-falling-game/index.html](drafts/zi-falling-game/index.html)

## Product Summary

- Adds a prominent pixel-accented **「汉字掉落测试」** entry button on the 识字 dashboard right below the 会读汉字 and 会写汉字 cards.
- Tapping it opens a fullscreen portrait 8-bit pixel-art voice literacy game tailored for 1st-grade Mandarin learners.
- **Solar System Stage Progression**:
  - Stages 1–8 follow the Solar System planets in order: 水星、金星、地球、火星、木星、土星、天王星、海王星.
  - Stages 9–20 continue with famous Solar System dwarf planets and asteroids: 冥王星、谷神星、灶神星、智神星、婚神星、龙宫星、贝努星、爱神星、灵神星、阋神星、妊神星、鸟神星 (with endless cycling beyond stage 20).
  - Completing a stage unlocks the next celestial body and saves best stars and score locally.
- **Gameplay & Offline Voice Recognition**:
  - Randomly draws 20 distinct characters from the readable pool (`proficiency == 5`; or the entire pool if fewer than 20).
  - Defaults to slow falling speed (~11 seconds per character), forgiving toneless Pinyin matching (covering polyphones and homophones), and phrase reading support (e.g. saying "木头" for 「木」).
  - Uses on-device streaming Sherpa-ONNX (`streaming-zipformer-zh-14M` INT8, ~25 MB, downloaded on first use with progress and retry) so it works completely offline without depending on Google Speech Services.
  - Correct pronunciation triggers multi-color pixel fireworks, score & combo bonuses, and 8-bit arcade audio.
- **Missed Character Review Sync (错字联动)**:
  - Characters that reach the ground defense wall are collected into the stage result review list with tone-marked Pinyin.
  - All missed characters are pre-selected (tappable to uncheck accidental noise misses) and synced back to the existing `PUT /zi/` endpoint with `proficiency = 3` so they appear immediately under 复习汉字.
