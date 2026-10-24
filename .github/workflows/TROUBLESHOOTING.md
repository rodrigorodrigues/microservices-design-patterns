# 🔧 Troubleshooting: Claude Not Responding

Covers both workflows: **Claude Code Review** (`claude-code-review.yml`, automatic PR review) and **Claude Code** (`claude.yml`, `@claude` mentions).

## Quick Checklist

### ✅ 1. Verify API Key is Set and Valid
Go to: `Settings → Secrets and variables → Actions`
- Check that `ANTHROPIC_API_KEY` exists
- Name must be **exactly** `ANTHROPIC_API_KEY` (case-sensitive)
- If the Actions log or a posted comment says something like "API key is invalid", the key itself is expired/revoked — regenerate one at https://console.anthropic.com/ (Settings → API Keys) and update the secret

### ✅ 2. Check Workflow Permissions
Go to: `Settings → Actions → General → Workflow permissions`
- Select: **Read and write permissions**
- Enable: **Allow GitHub Actions to create and approve pull requests**
- Click **Save**

### ✅ 3. Verify Workflows Are Enabled
Go to: `Actions` tab
- Check if workflows are showing in the left sidebar
- If you see a yellow banner saying "Workflows aren't being run on this repository", click **Enable workflows**

### ✅ 4. Check Workflow Files Are in Correct Location
Files must be in: `.github/workflows/`

```
.github/
  workflows/
    claude.yml
    claude-code-review.yml
```

### ✅ 5. How to Trigger Claude

**Automatic PR review** — no mention needed. Fires on PR opened, synchronize, reopened, or marked ready-for-review (`claude-code-review.yml`).

**`@claude` assistant** (`claude.yml`) fires on:
- A PR comment containing `@claude`
- An inline PR review comment containing `@claude`
- A submitted PR review whose body contains `@claude`
- A newly opened or assigned issue whose **title or body** contains `@claude`
- An issue comment containing `@claude`

There is **no** label-based trigger (e.g. `help-wanted`/`question`) — that existed in an earlier hand-rolled version of these workflows and was dropped when they were replaced by the official ones. If you want it back, add an `issues: [opened, labeled]` condition to `claude.yml`'s `if:` block.

## 🔍 Debug Steps

### Step 1: Check Workflow Runs
1. Go to **Actions** tab in your repo
2. Look for workflow runs named **"Claude Code Review"** or **"Claude Code"**
3. Click on a run to see details

**What to look for:**
- ❌ Red X = Failed (click to see error logs)
- ⚪ Gray circle = Skipped (workflow conditions not met)
- ✅ Green check = Success

### Step 2: Check the "if" Condition
If a run of `claude.yml` shows as skipped, its `if:` condition didn't match — the triggering comment/issue/review body didn't contain `@claude`, or the event type wasn't one of the ones it listens for. `claude-code-review.yml` has no `if:` gate — it should run on every matching `pull_request` event.

### Step 3: View Workflow Logs
1. Click on a workflow run
2. Click on the job (`claude-review` or `claude`)
3. Expand the **"Run Claude Code"** / **"Run Claude Code Review"** step
4. Look for errors in red

**Common errors:**
```
Error: ANTHROPIC_API_KEY not found / Process completed with exit code 1
→ Solution: Add the secret in Settings

API Error: "invalid x-api-key" / "API key is invalid"
→ Solution: The key was revoked/rotated — generate a new one and update the secret

Error: Resource not accessible by integration
→ Solution: Enable "Read and write permissions" under workflow permissions

Error: 401 Unauthorized
→ Solution: API key is invalid, regenerate it
```

## 🧪 Test the Workflow

### Test 1: PR Comment
1. Open any PR
2. Add a comment: `@claude are you working?`
3. Check the Actions tab for a "Claude Code" run

### Test 2: New Issue
1. Create an issue
2. Title or body: `@claude hello, are you working?`
3. Check the Actions tab for a "Claude Code" run

### Test 3: Automatic PR Review
1. Open (or push a new commit to) any PR
2. Check the Actions tab for a "Claude Code Review" run — should start without any `@claude` mention

## 🐛 Still Not Working?

### Check GitHub Actions Status
Sometimes GitHub Actions itself has issues: https://www.githubstatus.com/

### Enable Debug Logging
Add these secrets to get more detailed logs:
- `ACTIONS_RUNNER_DEBUG` = `true`
- `ACTIONS_STEP_DEBUG` = `true`

### Validate YAML Syntax
```
python3 -c "import yaml; yaml.safe_load(open('.github/workflows/claude.yml'))"
python3 -c "import yaml; yaml.safe_load(open('.github/workflows/claude-code-review.yml'))"
```

### Check Repository Settings
Go to `Settings → Actions → General`
- "Actions permissions" should be: **Allow all actions and reusable workflows**
- "Fork pull request workflows" settings may affect behavior on PRs from forks

## 📋 Manual Verification Checklist

```
Repository: rodrigorodrigues/microservices-design-patterns

□ ANTHROPIC_API_KEY exists in Secrets and is a currently-valid key
□ Workflow permissions set to "Read and write"
□ "Allow GitHub Actions to create and approve pull requests" enabled
□ Workflows are in .github/workflows/ as claude.yml and claude-code-review.yml
□ GitHub Actions are enabled for the repository
□ Comment/issue/review actually contains the literal text @claude (case-sensitive)
□ No syntax errors in YAML files
```

## 💡 Common Gotchas

1. **`@claude` vs `@Claude`**: matching is case-sensitive, use lowercase `@claude`
2. **First time setup**: the first workflow run after installing may take 1-2 minutes to start
3. **Workflow file changes**: after updating `.yml` files, it may take ~30 seconds to reflect
4. **Two workflows, two purposes**: `claude-code-review.yml` never needs `@claude` — if you're expecting a response to a mention and nothing happens, check `claude.yml`'s run instead

## 🆘 Get Help

If still stuck, open an issue with:
1. Screenshot of the Actions tab showing the workflow run (or its absence)
2. Screenshot of Settings → Actions → General
3. Copy of error logs from the failed run
4. Confirmation you completed all checklist items above

---

**Quick Test Command:**
Comment `@claude hello` on any open PR or issue and wait 1-2 minutes. If no response, check the Actions tab for the "Claude Code" run and its logs.
