# AI agent transcripts

The project was built in a single Claude Code session, exported here in full as Markdown. Screenshots that Claude took to check the UI are in [images/](images/).

| Session | Dates | Covered |
|---|---|---|
| [2026-11-03-connect-four-build.md](2026-11-03-connect-four-build.md) | 2026-11-03 to 2026-11-05 | The whole project, from the initial commit to adding these transcripts |

The session ran long enough that Claude Code compacted its context once (marked in the transcript). It continued in the same session. In order, it covered:

1. **Initial commit:** `.gitignore`, README, and the design plan in `DECISIONS.md`, with the assignment PDF kept out of git (Nov 3)
2. **Backend scaffold:** Spring Boot 3.5, then switched to 4.1 (Nov 4)
3. **Frontend scaffold:** React with Vite
4. **Docker Compose:** setup for a local run, plus the nginx SPA fallback
5. **Game rules:** tests written first and reviewed, then the implementation
6. **Persistence and API:** includes a deadlock found under simultaneous moves and fixed with a row lock
7. **Game UI:** checked end to end in headless Chrome
8. **Fixes, Kubernetes, and CI:** the column-full message fix, manifests tested on kind, and the CI workflow
9. **Docs:** README, final design notes, and a fresh-clone check
10. **This export** (Nov 5). The transcript ends while this step was in progress, so the final commit, push, and CI check aren't in it.
