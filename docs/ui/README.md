# Android UI catalog

Use this source index and the native release gallery to understand the implemented interface. The catalog covers app-owned screens, nested pages, and selected dialogs. System pickers, protected website content, and launcher widgets are verified separately when affected.

## Local UI review

1. Draft UI and navigation changes in `docs/ui/drafts/<feature>/index.html`. Keep assets relative and use sample data. Open the file in a browser and get approval before coding unless asked to proceed directly.
2. Link the approved HTML in the feature notes. Verify implemented states with Compose previews or the running app, including light, dark, compact layouts and dynamic color when relevant.
3. When a user pushes `release/vX.Y.Z`, Actions validates native screenshots and packages a gallery from Compose previews rendered at that same commit. Download `android-ui-gallery.zip` from the [GitHub release](https://github.com/TinkZhang/Tink-Super-App/releases), extract it, and open `index.html`. Before the first release with a gallery, use the [checked-in screenshot references](../../app/src/screenshotTestDebug/reference) and source below.
4. Review the affected screens and capture missing native states with sample data. Record the release and commit, appearance, viewport, and source in any supplemental gallery. Keep the reviewed draft and the release gallery available for later comparisons.

## Screens

### Home, navigation & settings

| Screen | Compose source |
| --- | --- |
| Home dashboard | [HomeScreen.kt](../../app/src/main/java/app/tinks/tink/home/HomeScreen.kt) |
| Navigation drawer | [MyApp.kt](../../app/src/main/java/app/tinks/tink/MyApp.kt) |
| Home · reading progress | [HomeScreen.kt](../../app/src/main/java/app/tinks/tink/home/HomeScreen.kt) |
| Settings | [SettingsScreen.kt](../../app/src/main/java/app/tinks/tink/settings/SettingsScreen.kt) |
| Settings · story length | [StorySettingsDialog.kt](../../app/src/main/java/app/tinks/tink/zi/StorySettingsDialog.kt) |

### Weight & haircut

| Screen | Compose source |
| --- | --- |
| Weight overview | [WeightScreen.kt](../../app/src/main/java/app/tinks/tink/weight/WeightScreen.kt) |
| Weight · history | [WeightScreen.kt](../../app/src/main/java/app/tinks/tink/weight/WeightScreen.kt) |
| Weight · empty history | [WeightScreen.kt](../../app/src/main/java/app/tinks/tink/weight/WeightScreen.kt) |
| Haircut dashboard | [HaircutScreen.kt](../../app/src/main/java/app/tinks/tink/haircut/HaircutScreen.kt) |
| Haircut · add record | [HaircutScreen.kt](../../app/src/main/java/app/tinks/tink/haircut/HaircutScreen.kt) |

### LeetKeeper

| Screen | Compose source |
| --- | --- |
| LeetKeeper · ongoing plan | [LeetKeeperScreen.kt](../../app/src/main/java/app/tinks/tink/leetkeeper/LeetKeeperScreen.kt) |
| LeetKeeper · no active plan | [LeetKeeperScreen.kt](../../app/src/main/java/app/tinks/tink/leetkeeper/LeetKeeperScreen.kt) |
| LeetKeeper · popular plans | [LeetKeeperScreen.kt](../../app/src/main/java/app/tinks/tink/leetkeeper/LeetKeeperScreen.kt) |
| LeetKeeper · done plans | [LeetKeeperScreen.kt](../../app/src/main/java/app/tinks/tink/leetkeeper/LeetKeeperScreen.kt) |
| LeetKeeper · public plan detail | [LeetKeeperScreen.kt](../../app/src/main/java/app/tinks/tink/leetkeeper/LeetKeeperScreen.kt) |
| LeetKeeper · problem detail | [LeetKeeperScreen.kt](../../app/src/main/java/app/tinks/tink/leetkeeper/LeetKeeperScreen.kt) |
| LeetKeeper · mark done sheet | [LeetKeeperScreen.kt](../../app/src/main/java/app/tinks/tink/leetkeeper/LeetKeeperScreen.kt) |

### 识字 & stories

| Screen | Compose source |
| --- | --- |
| 识字 dashboard | [ZiScreen.kt](../../app/src/main/java/app/tinks/tink/zi/ZiScreen.kt) |
| 识字 · add characters | [AddZiDialog.kt](../../app/src/main/java/app/tinks/tink/zi/AddZiDialog.kt) |
| Learned characters | [LearntZiListScreen.kt](../../app/src/main/java/app/tinks/tink/zi/zilist/LearntZiListScreen.kt) |
| Story list | [StoryListScreen.kt](../../app/src/main/java/app/tinks/tink/story/StoryListScreen.kt) |
| Story detail | [StoryDetailScreen.kt](../../app/src/main/java/app/tinks/tink/story/StoryDetailScreen.kt) |

### M-W Builder

| Screen | Compose source |
| --- | --- |
| M-W Builder | [MerriamScreen.kt](../../app/src/main/java/app/tinks/tink/merriam/MerriamScreen.kt) |

### Time

| Screen | Compose source |
| --- | --- |
| Time dashboard | [TimeScreen.kt](../../app/src/main/java/app/tinks/tink/time/TimeScreen.kt) |
| Time · add entry | [TimeScreen.kt](../../app/src/main/java/app/tinks/tink/time/TimeScreen.kt) |
| Time · edit entry | [TimeScreen.kt](../../app/src/main/java/app/tinks/tink/time/TimeScreen.kt) |
| Time · label manager | [TimeScreen.kt](../../app/src/main/java/app/tinks/tink/time/TimeScreen.kt) |
| Time · edit label | [TimeScreen.kt](../../app/src/main/java/app/tinks/tink/time/TimeScreen.kt) |

### ReadKeeper · home, shelves & search

| Screen | Compose source |
| --- | --- |
| ReadKeeper dashboard | [BookScreen.kt](../../app/src/main/java/app/tinks/tink/book/BookScreen.kt) |
| ReadKeeper · reading shelf | [BookScreen.kt](../../app/src/main/java/app/tinks/tink/book/BookScreen.kt) |
| ReadKeeper · wishlist | [BookScreen.kt](../../app/src/main/java/app/tinks/tink/book/BookScreen.kt) |
| ReadKeeper · abandoned shelf | [BookScreen.kt](../../app/src/main/java/app/tinks/tink/book/BookScreen.kt) |
| ReadKeeper · done shelf | [BookScreen.kt](../../app/src/main/java/app/tinks/tink/book/BookScreen.kt) |
| ReadKeeper · find a book | [BookScreen.kt](../../app/src/main/java/app/tinks/tink/book/BookScreen.kt) |
| ReadKeeper · search results | [BookScreen.kt](../../app/src/main/java/app/tinks/tink/book/BookScreen.kt) |

### ReadKeeper · book detail & notes

| Screen | Compose source |
| --- | --- |
| ReadKeeper · reading detail | [BookScreen.kt](../../app/src/main/java/app/tinks/tink/book/BookScreen.kt) |
| ReadKeeper · completed detail | [BookScreen.kt](../../app/src/main/java/app/tinks/tink/book/BookScreen.kt) |
| ReadKeeper · reading notes | [BookScreen.kt](../../app/src/main/java/app/tinks/tink/book/BookScreen.kt) |
| ReadKeeper · edit book | [BookScreen.kt](../../app/src/main/java/app/tinks/tink/book/BookScreen.kt) |
| ReadKeeper · update progress | [BookScreen.kt](../../app/src/main/java/app/tinks/tink/book/BookScreen.kt) |
| ReadKeeper · note editor | [BookScreen.kt](../../app/src/main/java/app/tinks/tink/book/BookScreen.kt) |
| ReadKeeper · delete confirmation | [BookScreen.kt](../../app/src/main/java/app/tinks/tink/book/BookScreen.kt) |

### ReadKeeper · yearly summary

| Screen | Compose source |
| --- | --- |
| ReadKeeper · yearly summary | [BookScreen.kt](../../app/src/main/java/app/tinks/tink/book/BookScreen.kt) |
| ReadKeeper · share image | [BookScreen.kt](../../app/src/main/java/app/tinks/tink/book/BookScreen.kt) |

### DiaryLoom

| Screen | Compose source |
| --- | --- |
| DiaryLoom dashboard | [DiaryScreen.kt](../../app/src/main/java/app/tinks/tink/diary/DiaryScreen.kt) |
| DiaryLoom · diaries and search | [DiaryScreen.kt](../../app/src/main/java/app/tinks/tink/diary/DiaryScreen.kt) |
| DiaryLoom · diary detail | [DiaryScreen.kt](../../app/src/main/java/app/tinks/tink/diary/DiaryScreen.kt) |
| DiaryLoom · new diary | [DiaryScreen.kt](../../app/src/main/java/app/tinks/tink/diary/DiaryScreen.kt) |
| DiaryLoom · edit diary | [DiaryScreen.kt](../../app/src/main/java/app/tinks/tink/diary/DiaryScreen.kt) |
| DiaryLoom · delete confirmation | [DiaryScreen.kt](../../app/src/main/java/app/tinks/tink/diary/DiaryScreen.kt) |

### Lottery

| Screen | Compose source |
| --- | --- |
| Lottery dashboard | [LotteryScreen.kt](../../app/src/main/java/app/tinks/tink/lottery/LotteryScreen.kt) |
| Lottery · history and stats | [LotteryScreen.kt](../../app/src/main/java/app/tinks/tink/lottery/LotteryScreen.kt) |
| Lottery · add ticket | [LotteryScreen.kt](../../app/src/main/java/app/tinks/tink/lottery/LotteryScreen.kt) |
| Lottery · reveal result | [LotteryScreen.kt](../../app/src/main/java/app/tinks/tink/lottery/LotteryScreen.kt) |

### Salesforce

| Screen | Compose source |
| --- | --- |
| Salesforce dashboard | [SalesforceScreen.kt](../../app/src/main/java/app/tinks/tink/salesforce/SalesforceScreen.kt) |
| Salesforce · practice | [SalesforceScreen.kt](../../app/src/main/java/app/tinks/tink/salesforce/SalesforceScreen.kt) |
| Salesforce · exam | [SalesforceScreen.kt](../../app/src/main/java/app/tinks/tink/salesforce/SalesforceScreen.kt) |
| Salesforce · exam result | [SalesforceScreen.kt](../../app/src/main/java/app/tinks/tink/salesforce/SalesforceScreen.kt) |
| Salesforce · answer review | [SalesforceScreen.kt](../../app/src/main/java/app/tinks/tink/salesforce/SalesforceScreen.kt) |

### Protected web & downloads

| Screen | Compose source |
| --- | --- |
| Baidu · fingerprint lock | [SecureWebScreen.kt](../../app/src/main/java/app/tinks/tink/secureweb/SecureWebScreen.kt) |
| Baidu · unlocked web container | [SecureWebScreen.kt](../../app/src/main/java/app/tinks/tink/secureweb/SecureWebScreen.kt) |
| YT · offline downloads | [YtScreen.kt](../../app/src/main/java/app/tinks/tink/yt/YtScreen.kt) |

### Random · settings & editor

| Screen | Compose source |
| --- | --- |
| Random · dice settings | [RandomSettingsScreen.kt](../../app/src/main/java/app/tinks/tink/random/RandomSettingsScreen.kt) |
| Random · wheel settings | [RandomSettingsScreen.kt](../../app/src/main/java/app/tinks/tink/random/RandomSettingsScreen.kt) |
| Random · coin settings | [RandomSettingsScreen.kt](../../app/src/main/java/app/tinks/tink/random/RandomSettingsScreen.kt) |
| Random · wheel editor | [RandomSettingsScreen.kt](../../app/src/main/java/app/tinks/tink/random/RandomSettingsScreen.kt) |
| Random · discard wheel edits | [RandomSettingsScreen.kt](../../app/src/main/java/app/tinks/tink/random/RandomSettingsScreen.kt) |

### Random · full-screen play

| Screen | Compose source |
| --- | --- |
| Random · dice play | [RandomPlayScreen.kt](../../app/src/main/java/app/tinks/tink/random/RandomPlayScreen.kt) |
| Random · wheel play | [RandomPlayScreen.kt](../../app/src/main/java/app/tinks/tink/random/RandomPlayScreen.kt) |
| Random · coin play | [RandomPlayScreen.kt](../../app/src/main/java/app/tinks/tink/random/RandomPlayScreen.kt) |

See [Weight](weight.md) for the original pilot details and Compose screenshot link.
