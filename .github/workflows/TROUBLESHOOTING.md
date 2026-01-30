# 🔧 Troubleshooting: Claude Not Responding to @claude

## Quick Checklist

### ✅ 1. Verify API Key is Set
Go to: `Settings → Secrets and variables → Actions`
- Check that `ANTHROPIC_API_KEY` exists
- Name must be **exactly** `ANTHROPIC_API_KEY` (case-sensitive)

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
Files must be in: `.github/workflows/` directory

```bash
# Correct structure:
.github/
  workflows/
    claude-pr-review.yml
    claude-issue-assistant.yml
```

### ✅ 5. For Issues - How to Trigger Claude

**Option 1: Mention @claude in issue body when creating**
```
Title: How do I add a new service?
Body: @claude Can you help me understand the architecture?
```

**Option 2: Add a label**
Create issue with label: `help-wanted` or `question`

**Option 3: Comment with @claude**
```
@claude What's the best way to do this?
```

## 🔍 Debug Steps

### Step 1: Check Workflow Runs
1. Go to **Actions** tab in your repo
2. Look for workflow runs named "Claude Issue Assistant"
3. Click on a run to see details

**What to look for:**
- ❌ Red X = Failed (click to see error logs)
- ⚪ Gray circle = Skipped (workflow conditions not met)
- ✅ Green check = Success

### Step 2: Check the "if" Condition
If workflow shows as "skipped", the `if` condition didn't pass.

**For Issues**, check if ANY of these are true:
- Issue has `help-wanted` label
- Issue has `question` label  
- Issue body contains `@claude`
- Comment contains `@claude`

### Step 3: View Workflow Logs
1. Click on a workflow run
2. Click on the job "claude-issue-help"
3. Expand each step to see detailed logs
4. Look for errors in red

**Common errors:**
```
Error: ANTHROPIC_API_KEY not found
→ Solution: Add the secret in Settings

Error: Resource not accessible by integration
→ Solution: Enable workflow permissions

Error: 401 Unauthorized
→ Solution: API key is invalid, regenerate it
```

## 🧪 Test the Workflow

### Test 1: Simple Issue with @claude
1. Create a new issue
2. Title: "Test Claude Integration"
3. Body: "@claude Hello, are you working?"
4. Submit
5. Go to Actions tab and watch for workflow run

### Test 2: Issue Comment
1. Open any existing issue
2. Add a comment: "@claude Can you help?"
3. Submit
4. Check Actions tab

### Test 3: Issue with Label
1. Create issue without @claude
2. Add label `question`
3. Check Actions tab

## 🐛 Still Not Working?

### Check GitHub Actions Status
Sometimes GitHub Actions itself has issues: https://www.githubstatus.com/

### Enable Debug Logging
Add these secrets to get more detailed logs:
- `ACTIONS_RUNNER_DEBUG` = `true`
- `ACTIONS_STEP_DEBUG` = `true`

### Validate YAML Syntax
Copy your workflow file and check at: https://www.yamllint.com/

### Check Repository Settings
Go to `Settings → Actions → General`
- "Actions permissions" should be: **Allow all actions and reusable workflows**
- "Fork pull request workflows" settings may affect behavior

## 📋 Manual Verification Checklist

Copy this checklist and verify each item:

```
Repository: rodrigorodrigues/microservices-design-patterns

□ ANTHROPIC_API_KEY exists in Secrets
□ Workflow permissions set to "Read and write"
□ "Allow GitHub Actions to create and approve pull requests" enabled
□ Workflows are in .github/workflows/ directory
□ Files are named exactly: claude-issue-assistant.yml
□ GitHub Actions are enabled for the repository
□ Issue contains @claude OR has help-wanted/question label
□ No syntax errors in YAML files
□ API key is valid (test at console.anthropic.com)
```

## 💡 Common Gotchas

1. **@claude vs @Claude**: Matching is case-sensitive, use lowercase `@claude`

2. **Label names**: Must be exactly `help-wanted` or `question` (with dash, not space)

3. **First time setup**: First workflow run may take 1-2 minutes to start

4. **Workflow file changes**: After updating .yml files, it may take 30 seconds to reflect

5. **Multiple workflows**: If you have multiple workflow files, make sure you're looking at the right one

## 🎯 Expected Behavior

**When working correctly:**

1. You mention @claude in an issue or comment
2. Within 30-60 seconds, workflow appears in Actions tab
3. Workflow runs (shows as yellow/in progress)
4. After 30-90 seconds, Claude posts a comment on the issue
5. Comment starts with "🤖 Claude AI Assistant"

**Timing:**
- Workflow trigger: ~10-30 seconds
- Workflow execution: ~30-60 seconds  
- Total: ~1-2 minutes from @claude to response

## 🆘 Get Help

If still stuck, create an issue with:
1. Screenshot of Actions tab showing workflow runs
2. Screenshot of Settings → Actions → General
3. Copy of error logs from failed workflow run
4. Confirm you completed all checklist items above

---

**Quick Test Command:**
Create issue titled "Test" with body "@claude hello" and wait 2 minutes. If no response, check Actions tab for errors.
