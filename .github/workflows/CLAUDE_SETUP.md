# Claude AI GitHub Integration for microservices-design-patterns

This integration adds Claude AI-powered code reviews and issue assistance to your repository.

## 🚀 Features

### PR Reviews (`claude-pr-review.yml`)
- **Automatic reviews** on every PR
- **Architecture analysis** aligned with microservice patterns
- **Multi-language support** (Java, Kotlin, Node.js, Python, Go)
- **Security checks** with automatic labeling
- **Interactive Q&A** - mention `@claude` in PR comments to ask questions

### Issue Assistant (`claude-issue-assistant.yml`)
- **Auto-responds** to issues labeled `help-wanted` or `question`
- **Context-aware** - understands your microservices architecture
- **Smart labeling** - automatically suggests relevant labels
- **Interactive help** - mention `@claude` in issue comments for follow-up

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

### 3. Add Workflow Files

Copy the workflow files to your repository:

```bash
# Create workflows directory if it doesn't exist
mkdir -p .github/workflows

# Copy the workflow files
cp claude-pr-review.yml .github/workflows/
cp claude-issue-assistant.yml .github/workflows/

# Commit and push
git add .github/workflows/claude-*.yml
git commit -m "Add Claude AI integration for PR reviews and issue assistance"
git push
```

### 4. Enable Workflow Permissions

1. Go to **Settings** → **Actions** → **General**
2. Scroll to **Workflow permissions**
3. Select **Read and write permissions**
4. Check **Allow GitHub Actions to create and approve pull requests**
5. Click **Save**

## 🎯 Usage

### For Pull Requests

**Automatic Review**: Every PR gets automatically reviewed by Claude

**Ask Questions**: Mention `@claude` in any PR comment:
```
@claude what's the security impact of this change?
```

**What Claude Checks**:
- ✅ Architecture alignment with microservice patterns
- ✅ Code quality and best practices
- ✅ Security concerns (auth, secrets, injections)
- ✅ Test coverage
- ✅ Documentation needs
- ✅ Cross-language integration issues

### For Issues

**Automatic Help**: Add `help-wanted` or `question` label to trigger Claude

**Ask Questions**: Mention `@claude` in any issue comment:
```
@claude how do I add a new microservice in Python?
```

**What Claude Helps With**:
- 🔍 Understanding the issue
- 📂 Identifying relevant services/files
- 💡 Suggesting solutions
- 📚 Providing resources and docs

## 🔧 Customization

### Adjust Review Depth

Edit `.github/workflows/claude-pr-review.yml`:

```yaml
# For more detailed reviews, increase max_tokens
"max_tokens": 4000,  # Change from 2000 to 4000
```

### Change Trigger Conditions

Edit when Claude responds:

```yaml
# Only review PRs with specific labels
on:
  pull_request:
    types: [opened, labeled]
    
jobs:
  claude-review:
    if: contains(github.event.pull_request.labels.*.name, 'needs-review')
```

### Add Custom Instructions

Modify the prompt in the workflow to focus on specific aspects:

```yaml
PROMPT="Please review this PR focusing on:
1. Spring Boot best practices
2. MongoDB query optimization
3. Docker image size
..."
```

## 📊 Cost Estimation

Claude Sonnet 4 pricing (as of Jan 2025):
- Input: $3 per million tokens
- Output: $15 per million tokens

Typical usage per PR:
- ~5,000 input tokens (diff + context)
- ~1,000 output tokens (review)
- Cost: ~$0.03 per PR

For 100 PRs/month: ~$3/month

## 🔒 Security Notes

- ✅ API key is stored securely in GitHub Secrets
- ✅ Claude only receives PR diffs, not full repository access
- ✅ No code is stored or trained on by Claude
- ✅ All analysis happens in your GitHub Actions runners

## 🐛 Troubleshooting

### "ANTHROPIC_API_KEY not found"
- Make sure you added the secret exactly as `ANTHROPIC_API_KEY`
- Check spelling and case sensitivity

### Claude isn't responding
- Check workflow runs in **Actions** tab
- Verify workflow permissions are set correctly
- Ensure the PR/issue meets trigger conditions

### Review is too generic
- Reduce diff size by making smaller PRs
- Add more context in PR description
- Customize the prompt for your needs

## 🆘 Need Help?

- Check [GitHub Actions logs](https://github.com/rodrigorodrigues/microservices-design-patterns/actions)
- Review [Anthropic API docs](https://docs.anthropic.com)
- Open an issue with the `question` label and mention `@claude`

## 📝 Example Interactions

### PR Review Example

**PR**: Add new authentication method
**Claude's Response**:
```markdown
## 🤖 Claude AI Review

### Architecture Review
✅ Good: Follows OAuth2 pattern consistent with existing auth-service
⚠️  Consider: Should this be in authentication-service or a separate endpoint?

### Code Quality  
✅ Clean separation of concerns
⚠️  Missing input validation on line 45

### Security
🔴 CRITICAL: Hardcoded JWT secret on line 67 - use environment variable
✅ Proper password hashing with BCrypt

### Testing
⚠️  Add integration tests for the new auth flow

### Documentation
📝 Update README with new authentication method
```

### Issue Assistant Example

**Issue**: How do I add a new microservice in Ruby?
**Claude's Response**:
```markdown
## 🤖 Claude AI Assistant

### Understanding
You want to add a Ruby microservice to this polyglot architecture.

### Relevant Files
- Check `/go-service` and `/nodejs-service` as examples
- Review `/docker/docker-compose.yml` for service registration
- See main `/README.md` for architecture patterns

### Solution Steps
1. Create new folder: `/ruby-service`
2. Implement REST API with JWT validation
3. Add MongoDB connection
4. Register with Consul
5. Create Dockerfile
6. Update docker-compose.yml

### Resources
- [Ruby Sinatra + MongoDB](https://docs.mongodb.com/ruby-driver/)
- [Consul Ruby Client](https://github.com/WeAreFarmGeek/diplomat)
```

## 🎉 Benefits

- 🚀 **Faster reviews** - Get instant feedback on every PR
- 🎯 **Consistent quality** - Apply architectural patterns uniformly
- 🔍 **Catch issues early** - Security and code quality checks
- 📚 **Learning tool** - Understand microservice best practices
- 🤝 **Team support** - Help with issues and questions 24/7

---

**Ready to try it?** Open a test PR or issue with the `question` label! 🎈
