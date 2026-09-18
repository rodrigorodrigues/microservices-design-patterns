# Claude AI GitHub Integration for microservices-design-patterns

This integration adds Claude-powered code review and an `@claude` assistant to this repository, via the official [`anthropics/claude-code-action`](https://github.com/anthropics/claude-code-action).

## 🚀 Features

### Automatic PR Review (`claude-code-review.yml`)
- Runs on every PR open, sync, reopen, and "ready for review"
- Uses the official `code-review` plugin (`/code-review:code-review`), not a hand-rolled prompt
- Read-only permissions — it reviews, it doesn't push commits or edit anything

### `@claude` Assistant (`claude.yml`)
- Responds anywhere you mention `@claude`: PR comments, inline PR review comments, submitted PR reviews, and issue comments
- Also picks up newly opened/assigned issues whose title or body mentions `@claude`
- Can read CI results on PRs (`additional_permissions: actions: read`) when asked about them

Both workflows were installed via `claude /install-github-app` and call the same official action — this repo doesn't call the Anthropic API directly anymore (an earlier hand-rolled `curl`-based setup was replaced; see git history on `claude-pr-review.yml`/`claude-issue-assistant.yml` if you need the old version for reference).

## 📋 Setup Instructions

### 1. Get Your Anthropic API Key

1. Go to https://console.anthropic.com/
2. Sign in or create an account
3. Navigate to **Settings** → **API Keys**
4. Click **Create Key** and copy it immediately

### 2. Add API Key to GitHub Secrets

1. Go to your repository: https://github.com/rodrigorodrigues/microservices-design-patterns
2. Click **Settings** → **Secrets and variables** → **Actions**
3. Click **New repository secret**
4. Name: `ANTHROPIC_API_KEY`
5. Value: Paste your API key
6. Click **Add secret**

Both `claude.yml` and `claude-code-review.yml` read this same secret via `anthropic_api_key: ${{ secrets.ANTHROPIC_API_KEY }}`. If the key is missing, expired, or revoked, the action fails with an API error surfaced in the Actions log (and sometimes as a posted comment) rather than silently doing nothing.

### 3. Workflow Files

Already present in this repo at:

```
.github/workflows/claude.yml
.github/workflows/claude-code-review.yml
```

If you ever need to reinstall or reconfigure them, run `claude /install-github-app` from a Claude Code session with access to this repo — it installs the official Claude GitHub App and can rewrite these files for you.

### 4. Enable Workflow Permissions

1. Go to **Settings** → **Actions** → **General**
2. Scroll to **Workflow permissions**
3. Select **Read and write permissions**
4. Check **Allow GitHub Actions to create and approve pull requests**
5. Click **Save**

## 🎯 Usage

### For Pull Requests

**Automatic Review**: every PR gets reviewed by the `code-review` plugin automatically — no `@claude` needed.

**Ask Questions**: mention `@claude` in a PR comment, an inline review comment, or a submitted review:
```
@claude what's the security impact of this change?
```

**What the automatic review checks**: whatever the `code-review` plugin covers (see [its docs](https://github.com/anthropics/claude-code)) — correctness, simplification/reuse opportunities, and efficiency, tuned by the prompt in `claude-code-review.yml`.

### For Issues

**Mention on open**: put `@claude` in the issue title or body when creating it.

**Ask Questions**: mention `@claude` in any issue comment:
```
@claude how do I add a new microservice in Python?
```

There is currently no label-based auto-trigger (e.g. `help-wanted`/`question`) — only `@claude` mentions trigger the assistant on issues. If you want that back, add an `issues: [opened, labeled]` condition to `claude.yml`'s `if:` block.

## 🔧 Customization

### Change what the automatic review does

Edit the `prompt`, `plugins`, or `plugin_marketplaces` inputs in `.github/workflows/claude-code-review.yml`. You can also restrict it to specific file paths (see the commented-out `paths:` block) or specific PR authors (see the commented-out author filter).

### Change what `@claude` can do

Edit `.github/workflows/claude.yml`:
- `prompt`: override with a fixed instruction instead of following whatever the comment that tagged it said
- `claude_args`: pass CLI flags, e.g. restrict tool access with `--allowed-tools Bash(gh pr *)`

See the [claude-code-action usage docs](https://github.com/anthropics/claude-code-action/blob/main/docs/usage.md) for the full set of options.

## 🔒 Security Notes

- ✅ API key is stored securely in GitHub Secrets
- ✅ Both workflows default to read-only `contents`/`pull-requests`/`issues` permissions plus `id-token: write` (required by the action) — they don't push code unless you explicitly grant write access and ask them to
- ✅ All analysis happens in your GitHub Actions runners

## 🐛 Troubleshooting

See [TROUBLESHOOTING.md](./TROUBLESHOOTING.md).

## 🆘 Need Help?

- Check [GitHub Actions logs](https://github.com/rodrigorodrigues/microservices-design-patterns/actions)
- Review the [claude-code-action repo](https://github.com/anthropics/claude-code-action) and [Anthropic API docs](https://docs.anthropic.com)
- Open an issue and mention `@claude`
