# Android UI catalog

**Reference revision** `9cb1de0` · [Android · Current UI](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=6-6)

The catalog covers every app navigation destination, nested feature page, and selected dialogs or states. It uses sample data, the light Material 3 palette, and a representative phone layout. The frames are editable references based on Compose source and the checked-in screenshot previews where available. Use the native app or Compose Preview for pixel-level checks, dynamic color, dark mode, and other window sizes.

Protected web, downloads, and Random are grouped on the [editable import board](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=9-2).

The two Weight drawer entries open the same overview. The unlocked Baidu reference represents the app’s WebView container. The website, Android system pickers, and launcher widgets are maintained separately from the app’s pages.

## When to update Figma

1. **Before coding**, draft the proposed UI on a separate Figma page or frame and have the user approve the exact frames. Record the approved link in the feature notes under `docs/ui/`. Use Current UI as the reference for the comparison.
2. **On the release branch**, pushing `release/vX.Y.Z` starts release verification and the **Prepare Figma refresh** job. The job compares the release commit with this catalog’s reference revision and puts candidate frames, changed sources, and design notes in the Actions summary and `figma-refresh` artifact. Further pushes to the release branch regenerate the handoff.
3. **After release visual review**, use a signed-in Figma session to update only the affected Current UI frames from the verified release build. Update this catalog’s frame links, source paths, and representative states, then record the verified release commit as the reference revision. If the app UI is identical to the reference, record the result without editing the canvas.

## Screens

### Home, navigation & settings

| Screen | Figma | Compose source |
| --- | --- | --- |
| Home dashboard | [Current frame](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=7-171) | [HomeScreen.kt](../../app/src/main/java/app/tinks/tink/home/HomeScreen.kt) |
| Navigation drawer | [Current frame](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=7-214) | [MyApp.kt](../../app/src/main/java/app/tinks/tink/MyApp.kt) |
| Home · reading progress | [Current frame](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=7-251) | [HomeScreen.kt](../../app/src/main/java/app/tinks/tink/home/HomeScreen.kt) |
| Settings | [Current frame](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=7-310) | [SettingsScreen.kt](../../app/src/main/java/app/tinks/tink/settings/SettingsScreen.kt) |
| Settings · story length | [Current frame](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=7-333) | [StorySettingsDialog.kt](../../app/src/main/java/app/tinks/tink/zi/StorySettingsDialog.kt) |

### Weight & haircut

| Screen | Figma | Compose source |
| --- | --- | --- |
| Weight overview | [Current frame](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=3-321) | [WeightScreen.kt](../../app/src/main/java/app/tinks/tink/weight/WeightScreen.kt) |
| Weight · history | [Current frame](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=7-372) | [WeightScreen.kt](../../app/src/main/java/app/tinks/tink/weight/WeightScreen.kt) |
| Weight · empty history | [Current frame](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=7-392) | [WeightScreen.kt](../../app/src/main/java/app/tinks/tink/weight/WeightScreen.kt) |
| Haircut dashboard | [Current frame](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=7-402) | [HaircutScreen.kt](../../app/src/main/java/app/tinks/tink/haircut/HaircutScreen.kt) |
| Haircut · add record | [Current frame](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=7-432) | [HaircutScreen.kt](../../app/src/main/java/app/tinks/tink/haircut/HaircutScreen.kt) |

### LeetKeeper

| Screen | Figma | Compose source |
| --- | --- | --- |
| LeetKeeper · ongoing plan | [Current frame](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=8-26) | [LeetKeeperScreen.kt](../../app/src/main/java/app/tinks/tink/leetkeeper/LeetKeeperScreen.kt) |
| LeetKeeper · no active plan | [Current frame](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=8-62) | [LeetKeeperScreen.kt](../../app/src/main/java/app/tinks/tink/leetkeeper/LeetKeeperScreen.kt) |
| LeetKeeper · popular plans | [Current frame](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=8-77) | [LeetKeeperScreen.kt](../../app/src/main/java/app/tinks/tink/leetkeeper/LeetKeeperScreen.kt) |
| LeetKeeper · done plans | [Current frame](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=8-98) | [LeetKeeperScreen.kt](../../app/src/main/java/app/tinks/tink/leetkeeper/LeetKeeperScreen.kt) |
| LeetKeeper · public plan detail | [Current frame](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=8-113) | [LeetKeeperScreen.kt](../../app/src/main/java/app/tinks/tink/leetkeeper/LeetKeeperScreen.kt) |
| LeetKeeper · problem detail | [Current frame](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=8-150) | [LeetKeeperScreen.kt](../../app/src/main/java/app/tinks/tink/leetkeeper/LeetKeeperScreen.kt) |
| LeetKeeper · mark done sheet | [Current frame](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=8-178) | [LeetKeeperScreen.kt](../../app/src/main/java/app/tinks/tink/leetkeeper/LeetKeeperScreen.kt) |

### 识字 & stories

| Screen | Figma | Compose source |
| --- | --- | --- |
| 识字 dashboard | [Current frame](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=8-240) | [ZiScreen.kt](../../app/src/main/java/app/tinks/tink/zi/ZiScreen.kt) |
| 识字 · add characters | [Current frame](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=8-286) | [AddZiDialog.kt](../../app/src/main/java/app/tinks/tink/zi/AddZiDialog.kt) |
| Learned characters | [Current frame](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=8-331) | [LearntZiListScreen.kt](../../app/src/main/java/app/tinks/tink/zi/zilist/LearntZiListScreen.kt) |
| Story list | [Current frame](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=8-352) | [StoryListScreen.kt](../../app/src/main/java/app/tinks/tink/story/StoryListScreen.kt) |
| Story detail | [Current frame](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=8-368) | [StoryDetailScreen.kt](../../app/src/main/java/app/tinks/tink/story/StoryDetailScreen.kt) |

### M-W Builder

| Screen | Figma | Compose source |
| --- | --- | --- |
| M-W Builder | [Current frame](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=8-381) | [MerriamScreen.kt](../../app/src/main/java/app/tinks/tink/merriam/MerriamScreen.kt) |

### Time

| Screen | Figma | Compose source |
| --- | --- | --- |
| Time dashboard | [Current frame](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=8-427) | [TimeScreen.kt](../../app/src/main/java/app/tinks/tink/time/TimeScreen.kt) |
| Time · add entry | [Current frame](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=8-479) | [TimeScreen.kt](../../app/src/main/java/app/tinks/tink/time/TimeScreen.kt) |
| Time · edit entry | [Current frame](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=8-569) | [TimeScreen.kt](../../app/src/main/java/app/tinks/tink/time/TimeScreen.kt) |
| Time · label manager | [Current frame](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=8-659) | [TimeScreen.kt](../../app/src/main/java/app/tinks/tink/time/TimeScreen.kt) |
| Time · edit label | [Current frame](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=8-727) | [TimeScreen.kt](../../app/src/main/java/app/tinks/tink/time/TimeScreen.kt) |

### ReadKeeper · home, shelves & search

| Screen | Figma | Compose source |
| --- | --- | --- |
| ReadKeeper dashboard | [Current frame](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=8-795) | [BookScreen.kt](../../app/src/main/java/app/tinks/tink/book/BookScreen.kt) |
| ReadKeeper · reading shelf | [Current frame](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=8-1108) | [BookScreen.kt](../../app/src/main/java/app/tinks/tink/book/BookScreen.kt) |
| ReadKeeper · wishlist | [Current frame](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=8-1134) | [BookScreen.kt](../../app/src/main/java/app/tinks/tink/book/BookScreen.kt) |
| ReadKeeper · abandoned shelf | [Current frame](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=8-1163) | [BookScreen.kt](../../app/src/main/java/app/tinks/tink/book/BookScreen.kt) |
| ReadKeeper · done shelf | [Current frame](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=8-1187) | [BookScreen.kt](../../app/src/main/java/app/tinks/tink/book/BookScreen.kt) |
| ReadKeeper · find a book | [Current frame](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=8-1222) | [BookScreen.kt](../../app/src/main/java/app/tinks/tink/book/BookScreen.kt) |
| ReadKeeper · search results | [Current frame](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=8-1235) | [BookScreen.kt](../../app/src/main/java/app/tinks/tink/book/BookScreen.kt) |

### ReadKeeper · book detail & notes

| Screen | Figma | Compose source |
| --- | --- | --- |
| ReadKeeper · reading detail | [Current frame](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=8-1264) | [BookScreen.kt](../../app/src/main/java/app/tinks/tink/book/BookScreen.kt) |
| ReadKeeper · completed detail | [Current frame](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=8-1314) | [BookScreen.kt](../../app/src/main/java/app/tinks/tink/book/BookScreen.kt) |
| ReadKeeper · reading notes | [Current frame](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=8-1352) | [BookScreen.kt](../../app/src/main/java/app/tinks/tink/book/BookScreen.kt) |
| ReadKeeper · edit book | [Current frame](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=8-1369) | [BookScreen.kt](../../app/src/main/java/app/tinks/tink/book/BookScreen.kt) |
| ReadKeeper · update progress | [Current frame](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=8-1456) | [BookScreen.kt](../../app/src/main/java/app/tinks/tink/book/BookScreen.kt) |
| ReadKeeper · note editor | [Current frame](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=8-1531) | [BookScreen.kt](../../app/src/main/java/app/tinks/tink/book/BookScreen.kt) |
| ReadKeeper · delete confirmation | [Current frame](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=8-1567) | [BookScreen.kt](../../app/src/main/java/app/tinks/tink/book/BookScreen.kt) |

### ReadKeeper · yearly summary

| Screen | Figma | Compose source |
| --- | --- | --- |
| ReadKeeper · yearly summary | [Current frame](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=8-1630) | [BookScreen.kt](../../app/src/main/java/app/tinks/tink/book/BookScreen.kt) |
| ReadKeeper · share image | [Current frame](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=8-1677) | [BookScreen.kt](../../app/src/main/java/app/tinks/tink/book/BookScreen.kt) |

### DiaryLoom

| Screen | Figma | Compose source |
| --- | --- | --- |
| DiaryLoom dashboard | [Current frame](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=8-1720) | [DiaryScreen.kt](../../app/src/main/java/app/tinks/tink/diary/DiaryScreen.kt) |
| DiaryLoom · diaries and search | [Current frame](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=8-2040) | [DiaryScreen.kt](../../app/src/main/java/app/tinks/tink/diary/DiaryScreen.kt) |
| DiaryLoom · diary detail | [Current frame](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=8-2066) | [DiaryScreen.kt](../../app/src/main/java/app/tinks/tink/diary/DiaryScreen.kt) |
| DiaryLoom · new diary | [Current frame](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=8-2101) | [DiaryScreen.kt](../../app/src/main/java/app/tinks/tink/diary/DiaryScreen.kt) |
| DiaryLoom · edit diary | [Current frame](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=8-2147) | [DiaryScreen.kt](../../app/src/main/java/app/tinks/tink/diary/DiaryScreen.kt) |
| DiaryLoom · delete confirmation | [Current frame](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=8-2193) | [DiaryScreen.kt](../../app/src/main/java/app/tinks/tink/diary/DiaryScreen.kt) |

### Lottery

| Screen | Figma | Compose source |
| --- | --- | --- |
| Lottery dashboard | [Current frame](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=8-2241) | [LotteryScreen.kt](../../app/src/main/java/app/tinks/tink/lottery/LotteryScreen.kt) |
| Lottery · history and stats | [Current frame](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=8-2277) | [LotteryScreen.kt](../../app/src/main/java/app/tinks/tink/lottery/LotteryScreen.kt) |
| Lottery · add ticket | [Current frame](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=8-2320) | [LotteryScreen.kt](../../app/src/main/java/app/tinks/tink/lottery/LotteryScreen.kt) |
| Lottery · reveal result | [Current frame](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=8-2354) | [LotteryScreen.kt](../../app/src/main/java/app/tinks/tink/lottery/LotteryScreen.kt) |

### Salesforce

| Screen | Figma | Compose source |
| --- | --- | --- |
| Salesforce dashboard | [Current frame](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=8-2387) | [SalesforceScreen.kt](../../app/src/main/java/app/tinks/tink/salesforce/SalesforceScreen.kt) |
| Salesforce · practice | [Current frame](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=8-2417) | [SalesforceScreen.kt](../../app/src/main/java/app/tinks/tink/salesforce/SalesforceScreen.kt) |
| Salesforce · exam | [Current frame](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=8-2456) | [SalesforceScreen.kt](../../app/src/main/java/app/tinks/tink/salesforce/SalesforceScreen.kt) |
| Salesforce · exam result | [Current frame](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=8-2492) | [SalesforceScreen.kt](../../app/src/main/java/app/tinks/tink/salesforce/SalesforceScreen.kt) |
| Salesforce · answer review | [Current frame](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=8-2519) | [SalesforceScreen.kt](../../app/src/main/java/app/tinks/tink/salesforce/SalesforceScreen.kt) |

### Protected web & downloads

| Screen | Figma | Compose source |
| --- | --- | --- |
| Baidu · fingerprint lock | [Imported board](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=9-2) | [SecureWebScreen.kt](../../app/src/main/java/app/tinks/tink/secureweb/SecureWebScreen.kt) |
| Baidu · unlocked web container | [Imported board](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=9-2) | [SecureWebScreen.kt](../../app/src/main/java/app/tinks/tink/secureweb/SecureWebScreen.kt) |
| YT · offline downloads | [Imported board](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=9-2) | [YtScreen.kt](../../app/src/main/java/app/tinks/tink/yt/YtScreen.kt) |

### Random · settings & editor

| Screen | Figma | Compose source |
| --- | --- | --- |
| Random · dice settings | [Imported board](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=9-2) | [RandomSettingsScreen.kt](../../app/src/main/java/app/tinks/tink/random/RandomSettingsScreen.kt) |
| Random · wheel settings | [Imported board](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=9-2) | [RandomSettingsScreen.kt](../../app/src/main/java/app/tinks/tink/random/RandomSettingsScreen.kt) |
| Random · coin settings | [Imported board](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=9-2) | [RandomSettingsScreen.kt](../../app/src/main/java/app/tinks/tink/random/RandomSettingsScreen.kt) |
| Random · wheel editor | [Imported board](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=9-2) | [RandomSettingsScreen.kt](../../app/src/main/java/app/tinks/tink/random/RandomSettingsScreen.kt) |
| Random · discard wheel edits | [Imported board](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=9-2) | [RandomSettingsScreen.kt](../../app/src/main/java/app/tinks/tink/random/RandomSettingsScreen.kt) |

### Random · full-screen play

| Screen | Figma | Compose source |
| --- | --- | --- |
| Random · dice play | [Imported board](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=9-2) | [RandomPlayScreen.kt](../../app/src/main/java/app/tinks/tink/random/RandomPlayScreen.kt) |
| Random · wheel play | [Imported board](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=9-2) | [RandomPlayScreen.kt](../../app/src/main/java/app/tinks/tink/random/RandomPlayScreen.kt) |
| Random · coin play | [Imported board](https://www.figma.com/design/bGT7KPRSoMJ1iYkmHNmAnf?node-id=9-2) | [RandomPlayScreen.kt](../../app/src/main/java/app/tinks/tink/random/RandomPlayScreen.kt) |

See [Weight](weight.md) for the original pilot details and Compose screenshot link.
