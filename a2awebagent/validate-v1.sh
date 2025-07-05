#!/bin/bash

echo "🎯 A2A Travel Agent v1.0 Validation Script"
echo "==========================================="

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

success_count=0
total_checks=0

check_result() {
    total_checks=$((total_checks + 1))
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✅ $2${NC}"
        success_count=$((success_count + 1))
    else
        echo -e "${RED}❌ $2${NC}"
    fi
}

echo -e "\n📁 Documentation Structure Validation"
echo "--------------------------------------"

# Check main documentation files
[ -f "specs/README.md" ] && check_result 0 "Main specs README exists" || check_result 1 "Main specs README missing"
[ -d "specs/getting-started" ] && check_result 0 "Getting started directory exists" || check_result 1 "Getting started directory missing"
[ -d "specs/architecture" ] && check_result 0 "Architecture directory exists" || check_result 1 "Architecture directory missing"
[ -d "specs/api-reference" ] && check_result 0 "API reference directory exists" || check_result 1 "API reference directory missing"
[ -d "specs/guides" ] && check_result 0 "Guides directory exists" || check_result 1 "Guides directory missing"
[ -d "specs/reference" ] && check_result 0 "Reference directory exists" || check_result 1 "Reference directory missing"

# Check getting started files
[ -f "specs/getting-started/01-project-overview.md" ] && check_result 0 "Project overview tutorial exists" || check_result 1 "Project overview tutorial missing"
[ -f "specs/getting-started/02-docker-setup.md" ] && check_result 0 "Docker setup tutorial exists" || check_result 1 "Docker setup tutorial missing"
[ -f "specs/getting-started/03-mcp-protocol.md" ] && check_result 0 "MCP protocol tutorial exists" || check_result 1 "MCP protocol tutorial missing"
[ -f "specs/getting-started/04-phase1-success.md" ] && check_result 0 "Phase 1 success story exists" || check_result 1 "Phase 1 success story missing"

# Check architecture files
[ -f "specs/architecture/agentic-patterns.md" ] && check_result 0 "Agentic patterns doc exists" || check_result 1 "Agentic patterns doc missing"
[ -f "specs/architecture/superintelligence-platform.md" ] && check_result 0 "Superintelligence platform doc exists" || check_result 1 "Superintelligence platform doc missing"
[ -f "specs/architecture/system-overview.md" ] && check_result 0 "System overview doc exists" || check_result 1 "System overview doc missing"

# Check API reference files
[ -f "specs/api-reference/model-management.md" ] && check_result 0 "Model management API doc exists" || check_result 1 "Model management API doc missing"
[ -f "specs/api-reference/screenshot-gallery.md" ] && check_result 0 "Screenshot gallery API doc exists" || check_result 1 "Screenshot gallery API doc missing"
[ -f "specs/api-reference/tool-testing.md" ] && check_result 0 "Tool testing API doc exists" || check_result 1 "Tool testing API doc missing"

# Check deployment guides
[ -f "specs/guides/deployment/production-deployment.md" ] && check_result 0 "Production deployment guide exists" || check_result 1 "Production deployment guide missing"
[ -f "specs/guides/troubleshooting/common-issues.md" ] && check_result 0 "Troubleshooting guide exists" || check_result 1 "Troubleshooting guide missing"

# Check v2 planning
[ -f "specs/reference/v2-planning/v2-roadmap.md" ] && check_result 0 "v2 roadmap exists" || check_result 1 "v2 roadmap missing"

# Check that research is archived
[ -d "specs/reference/research-archive" ] && check_result 0 "Research archive directory exists" || check_result 1 "Research archive directory missing"

# Check that no WIP files remain in main areas
! [ -f "specs/userplan.md" ] && check_result 0 "Old userplan.md removed" || check_result 1 "Old userplan.md still exists"

echo -e "\n🔧 Project Structure Validation"
echo "-------------------------------"

# Check core project files
[ -f "README.MD" ] && check_result 0 "Main README exists" || check_result 1 "Main README missing"
[ -f "docker-compose.yml" ] && check_result 0 "Docker compose file exists" || check_result 1 "Docker compose file missing"
[ -f "pom.xml" ] && check_result 0 "Maven POM exists" || check_result 1 "Maven POM missing"

# Check module structure
[ -d "a2acore" ] && check_result 0 "a2acore module exists" || check_result 1 "a2acore module missing"
[ -d "a2awebapp" ] && check_result 0 "a2awebapp module exists" || check_result 1 "a2awebapp module missing"

echo -e "\n📊 Content Quality Validation"
echo "-----------------------------"

# Check for version badges in main README
if grep -q "version-1.0.0-brightgreen" README.MD; then
    check_result 0 "Version badge shows v1.0.0"
else
    check_result 1 "Version badge missing or incorrect"
fi

# Check for production ready status
if grep -q "production ready" README.MD; then
    check_result 0 "Production ready status indicated"
else
    check_result 1 "Production ready status missing"
fi

# Check for protocol documentation
if grep -q "Agent2Agent (A2A)" README.MD && grep -q "Model Context Protocol (MCP)" README.MD; then
    check_result 0 "Both A2A and MCP protocols documented"
else
    check_result 1 "Protocol documentation incomplete"
fi

# Check for Docker section
if grep -q "Docker Deployment" README.MD; then
    check_result 0 "Docker deployment section exists"
else
    check_result 1 "Docker deployment section missing"
fi

echo -e "\n🎯 Final Results"
echo "=================="
echo -e "Passed: ${GREEN}$success_count${NC}/$total_checks checks"

if [ $success_count -eq $total_checks ]; then
    echo -e "\n${GREEN}🎉 V1.0 VALIDATION SUCCESSFUL!${NC}"
    echo -e "${GREEN}✅ All documentation is properly organized${NC}"
    echo -e "${GREEN}✅ Project structure is complete${NC}"
    echo -e "${GREEN}✅ Content quality meets v1.0 standards${NC}"
    echo -e "\n🚀 ${YELLOW}Project is ready for v1.0 release!${NC}"
    exit 0
else
    echo -e "\n${RED}❌ V1.0 VALIDATION FAILED${NC}"
    echo -e "${RED}Some checks failed. Please review the output above.${NC}"
    exit 1
fi