#!/bin/bash

# Farben für Output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Konfiguration
OLD_EMAIL="vatankeskin47@gmail.com"
NEW_NAME="GreenCone"
NEW_EMAIL="noreply@github.com"
OWNER="GreenCoder0930"

# Array mit allen Repos
REPOS=(
  "appvault:main"
  "badusb-keylogger:master"
  "codynn:main"
  "Englisch-film:main"
  "Englisch-film-stop-motion:main"
  "Github-login:main"
  "MM2Script:main"
  "Port-scanner:main"
  "Shhh:main"
  "Snake:main"
  "thc-hydra:master"
  "usbrubberducky-payloads:master"
  "Verif-bot-:main"
)

# Arbeitsverzeichnis
WORK_DIR="$HOME/repos-rewrite"
mkdir -p "$WORK_DIR"

echo -e "${YELLOW}======================================${NC}"
echo -e "${YELLOW}Starting Commit Rewrite Process${NC}"
echo -e "${YELLOW}======================================${NC}"
echo -e "${YELLOW}Old Email: $OLD_EMAIL${NC}"
echo -e "${YELLOW}New Name: $NEW_NAME${NC}"
echo -e "${YELLOW}New Email: $NEW_EMAIL${NC}"
echo -e "${YELLOW}======================================${NC}\n"

# Counter
SUCCESS=0
FAILED=0

# Für jedes Repo
for REPO_INFO in "${REPOS[@]}"; do
  REPO_NAME="${REPO_INFO%:*}"
  DEFAULT_BRANCH="${REPO_INFO#*:}"
  
  echo -e "${YELLOW}[$(date '+%H:%M:%S')] Processing: $REPO_NAME (Branch: $DEFAULT_BRANCH)${NC}"
  
  REPO_PATH="$WORK_DIR/$REPO_NAME"
  
  # Repo klonen oder aktualisieren
  if [ -d "$REPO_PATH" ]; then
    echo -e "  ${YELLOW}→ Repository already exists, pulling latest...${NC}"
    cd "$REPO_PATH" || continue
    git pull origin "$DEFAULT_BRANCH" 2>/dev/null || git fetch origin
  else
    echo -e "  ${YELLOW}→ Cloning repository...${NC}"
    git clone --quiet "https://github.com/$OWNER/$REPO_NAME.git" "$REPO_PATH" 2>/dev/null
    if [ $? -ne 0 ]; then
      echo -e "  ${RED}✗ Failed to clone $REPO_NAME${NC}"
      FAILED=$((FAILED + 1))
      continue
    fi
    cd "$REPO_PATH" || continue
  fi
  
  # Checkout zum Default Branch
  git checkout "$DEFAULT_BRANCH" 2>/dev/null
  
  # Prüfe ob Commits mit alter Email existieren
  COMMIT_COUNT=$(git log --all --format="%ae" | grep -c "$OLD_EMAIL")
  
  if [ "$COMMIT_COUNT" -eq 0 ]; then
    echo -e "  ${GREEN}✓ No commits with old email found${NC}\n"
    SUCCESS=$((SUCCESS + 1))
    continue
  fi
  
  echo -e "  ${YELLOW}→ Found $COMMIT_COUNT commits with old email${NC}"
  echo -e "  ${YELLOW}→ Running git filter-branch...${NC}"
  
  # Git filter-branch ausführen
  git filter-branch --env-filter '
    if [ "$GIT_COMMITTER_EMAIL" = "'"$OLD_EMAIL"'" ]
    then
      export GIT_COMMITTER_NAME="'"$NEW_NAME"'"
      export GIT_COMMITTER_EMAIL="'"$NEW_EMAIL"'"
    fi
    if [ "$GIT_AUTHOR_EMAIL" = "'"$OLD_EMAIL"'" ]
    then
      export GIT_AUTHOR_NAME="'"$NEW_NAME"'"
      export GIT_AUTHOR_EMAIL="'"$NEW_EMAIL"'"
    fi
  ' --tag-name-filter=cat -- --all 2>/dev/null
  
  if [ $? -ne 0 ]; then
    echo -e "  ${RED}✗ filter-branch failed${NC}"
    FAILED=$((FAILED + 1))
    cd "$WORK_DIR" || continue
    continue
  fi
  
  # Force push zu GitHub
  echo -e "  ${YELLOW}→ Force pushing to GitHub...${NC}"
  
  # Alle Branches pushen
  git push origin --all --force-with-lease 2>/dev/null
  PUSH_RESULT=$?
  
  # Tags pushen
  git push origin --tags --force-with-lease 2>/dev/null
  
  if [ $PUSH_RESULT -eq 0 ]; then
    echo -e "  ${GREEN}✓ Successfully processed $REPO_NAME${NC}\n"
    SUCCESS=$((SUCCESS + 1))
  else
    echo -e "  ${RED}✗ Failed to push $REPO_NAME${NC}\n"
    FAILED=$((FAILED + 1))
  fi
  
  cd "$WORK_DIR" || continue
done

# Zusammenfassung
echo -e "${YELLOW}======================================${NC}"
echo -e "${YELLOW}SUMMARY${NC}"
echo -e "${YELLOW}======================================${NC}"
echo -e "${GREEN}✓ Successful: $SUCCESS${NC}"
echo -e "${RED}✗ Failed: $FAILED${NC}"
echo -e "${YELLOW}======================================${NC}"

if [ $FAILED -eq 0 ]; then
  echo -e "${GREEN}All repositories processed successfully!${NC}"
else
  echo -e "${RED}Some repositories failed. Check the output above.${NC}"
fi

echo -e "\n${YELLOW}Files are in: $WORK_DIR${NC}"
echo -e "${YELLOW}You can delete this folder after verification: rm -rf $WORK_DIR${NC}\n"
