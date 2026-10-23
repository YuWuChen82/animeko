# Repository Guidelines

This is the repository for the app. For the server, you can navigate to ../ani-api-server

Read docs/contributing for project guidelines. Before modifying a subsystem, check docs/contributing/code/ for its documentation (e.g. the media framework docs cover terminology, class-level code maps, and the playback flow) and read the relevant docs first.

Additional requirements:

- You should add imports, instead of using fully qualified names in code.
- For Android Instrumented tests, you can just use `@Test`, no need to write `@RunWith` to the class.
- Backtick test names in `commonTest` and Android test source sets use only English letters, digits, spaces, `-` and `_`, so D8 can dex them for Android device tests.
- 注释和文档应直接描述当前设计、职责、行为与约束，不要用“不再…”“改为…”“新设计…”等措辞叙述开发过程，也不要记录未上线方案、被纠正的错误假设或对话历史。只有在解释兼容性或迁移逻辑确有必要时，才说明已发布版本的历史行为。

## UI Verification

- **Prefer reusable interactive UI tests** over driving a real window: use `runAniComposeUiTest` (`utils/ui-testing`) with synthetic input (`performClick`, `performTextInput`, `sendKeyEvent`) and assert on semantics — focus, text, state, bounds. They run without OS input — no focus stealing, no real mouse — and stay in the repo as regression tests. When you verify a UI change manually, consider leaving such a test behind.
- `assertScreenshot` compares against golden images only on desktop; on Android it is a no-op. Android and TV device tests do not capture or compare pixels: expose visual state that semantics cannot express (blur, dim, glow, whether an animation runs) through `TvVisualSemantics` in TV code, and test color or geometry calculations as pure functions in host tests. Check the rendered look manually with the skills below.
- Reserve the skills below for what headless tests cannot cover: JCEF, VLC/mpv playback, native libraries, packaging, window chrome, emulator behavior.
- Screenshots and recordings taken as verification evidence never go into the repository: git history keeps every binary forever, even after the file or branch is deleted. Upload them as GitHub attachments (the `github-image-upload` skill, when available) and embed the `https://github.com/user-attachments/...` URLs in the PR description or comment. If the upload fails, post no images and describe what you verified in text. Do not commit files, push branches, or create refs just to get an image URL. App resources and `assertScreenshot` baselines are not evidence and belong in the repository as usual.

## Agent Skills

- For interactive Android UI verification (start emulator, install the app, then tap/swipe/type and verify via screenshots, UI-hierarchy dumps, logcat, and Figma design comparison), use the repo-local skill at `.agents/skills/android-ui-verify/SKILL.md`. Its toolbox script is `.agents/skills/android-ui-verify/scripts/droid.sh` (run `droid.sh help`). `.claude/skills/android-ui-verify` is a symlink to it for Claude Code auto-discovery.
- For desktop/PC executable validation (Compose Desktop, JCEF, VLC/native libraries, packaging, macOS window screenshots), use the repo-local skill at `.agents/skills/desktop-ui-verify/SKILL.md`.

## Generating Client

If you change server API, you can then use `./gradlew generateOpenApiForAnimeko` to automatically re-generate the client. Don't manually write http client.

<!-- project-wiki:start -->
## Ani 项目 Markdown Wiki

已安装原版 llm-wiki skill。项目 ID 为 ani，总入口为 `E:/知识库/README.md`，项目 Wiki 位于 `E:/知识库/projects/ani/`，原始资料位于 `E:/知识库/raw/ani/`。

开始 Ani 讨论或开发前，先读本工作区 `.codex/knowledge.json`，再读该 Wiki 的 SCHEMA.md 和 index.md，按 llm-wiki 的 ingest/query/lint 工作流维护。默认只检索 Ani；明确切换项目时再选择独立目录，禁止混写。

本机直接搜索 Markdown：`rg -n -F --glob '*.md' -- '<关键词>' 'E:/知识库/projects/ani'`。包含索引、规范与日志，必要时由 agent 扩展同义词。不执行默认 init_wiki.py/setup_wiki.py 的向量 runtime 安装，不使用默认 hybrid search，不创建向量数据库；上游代码保持原样。

讨论形成有后续价值的新方案、明确决定，或开发产生进展、阻塞和验证观察时，主动保存来源、状态与摘要，维护相关引用、索引及祖先摘要。每个独立决定单独成页；同一决定的补充证据可更新原页，替代决定另建并互链。没有新知识时无需新增页面。

每次新增、修改或修复用户可见功能时，必须同步更新仓库根目录 `PERSONAL_FEATURES.md`，让个人版相对官方版的功能清单与当前代码一致；新增个人版功能还必须在根目录 `README.md` 中保留指向该清单的引用。发版前核对这两份文档与实际实现及发布版本。

相关任务收尾运行原版检查脚本：`python -X utf8 .agents/skills/llm-wiki/scripts/wiki_lint.py E:/知识库/projects/ani --required-fm type,title,tags,created,updated,project,status,summary,sources --json`，阅读结果并修正结构问题。语义粒度和来源真实性由 agent 判断，没有后台监听或强制收尾 hook。

源码位于 `E:/Ani/animeko-personal`。其 AGENTS.md 和 docs/contributing/ 的开发约束继续有效；涉及子系统时先读对应文档。主 agent 汇总子 agent 的证据写入知识库，避免同时修改同一页面。普通已授权的知识维护无需逐次询问。
<!-- project-wiki:end -->
