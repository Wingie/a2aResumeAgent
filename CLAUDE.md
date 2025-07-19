# CLAUDE.md

This file provides guidance to Claude Code when working with the a2aTravelAgent repository.

## Project Overview

**a2aTravelAgent** - AI-Powered Travel Research Agent with MCP (Model Context Protocol) support

## Repository Structure

```
a2aTravelAgent/
├── a2ajava/              # 📁 UNUSED (legacy reference only)
└── a2awebagent/          # 🚀 ACTIVE PROJECT
    ├── a2acore/          # Internal MCP framework
    └── a2awebapp/        # Spring Boot application
```

**Key Facts:**
- **Active Project**: `/a2awebagent/` 
- **Working Directory**: `/Users/wingston/code/a2aTravelAgent/a2awebagent`
- **Architecture**: Multi-module Maven project
- **Dependencies**: a2awebapp uses internal a2acore (NOT external a2ajava)

## Quick Commands

```bash
# Navigate to working directory
cd /Users/wingston/code/a2aTravelAgent/a2awebagent

# Build and run
mvn spring-boot:run -pl a2awebapp

# Docker (preferred)
docker-compose down a2awebagent && docker-compose up --build a2awebagent -d

# Sanity test
./sanity_test.sh
```

## Development Workflow

1. **All development happens in `/a2awebagent/`**
2. **Main app**: http://localhost:7860
3. **Detailed docs**: See `a2awebagent/CLAUDE.md` for comprehensive development guide

## Code Modification Pattern

Always use Task() agents for analysis and changes:
```bash
Task(
  description="Debug issue",
  prompt="Use read_file and grep to analyze problem, then provide solution"
)
```

## Important Notes

- **Working directory**: Always use `/a2awebagent/` as base
- **Project structure**: Multi-module Maven (a2acore + a2awebapp)
- **External dependencies**: None - all functionality is internal
- **Docker**: Preferred deployment method
- **Testing**: Use sanity_test.sh for health checks
- **Creating New Files**: If creating a file it has to be the result of a plan and todo after finding and user validates the need for the file, always try and find existing files and only create new files if absolutely necessary. 
- **Rebuild**: docker-compose up --build -d && docker-compose restart a2awebagent