#!/bin/bash

# Exit immediately if a command exits with a non-zero status.
set -e

# --- Configuration ---
SSH_OPTIONS="-o StrictHostKeyChecking=no"
IP_CACHE_FILE=".last_aws_ips"

# --- Helper Functions ---
# Function to print a formatted header.
print_header() {
  echo ""
  echo "### $1 ###"
  echo ""
}

# --- Main Script ---
print_header "AWS Admin User Creation Tool"

# Load last used IPs and SSH key if the cache file exists
if [ -f "$IP_CACHE_FILE" ]; then
    source "$IP_CACHE_FILE"
fi

# Prompt for the SSH key filename.
read -p "Enter the filename of your AWS EC2 key pair in ~/.ssh/ [${LAST_SSH_KEY_FILENAME}]: " SSH_KEY_FILENAME
SSH_KEY_FILENAME=${SSH_KEY_FILENAME:-$LAST_SSH_KEY_FILENAME}

# Construct the full path and check if it exists
SSH_KEY_PATH="~/.ssh/${SSH_KEY_FILENAME}"
eval SSH_KEY_PATH_EXPANDED=$SSH_KEY_PATH
if [ ! -f "$SSH_KEY_PATH_EXPANDED" ]; then
    echo "Error: SSH key not found at '$SSH_KEY_PATH_EXPANDED'"
    exit 1
fi

# Prompt for the app EC2 instance IP.
read -p "Enter the PUBLIC IP of the 'app' EC2 instance [${LAST_APP_PUBLIC_IP}]: " APP_PUBLIC_IP
APP_PUBLIC_IP=${APP_PUBLIC_IP:-$LAST_APP_PUBLIC_IP}

# Save the entered IP and SSH key filename for the next run.
if [ ! -f "$IP_CACHE_FILE" ]; then
    touch "$IP_CACHE_FILE"
fi
grep -v "LAST_APP_PUBLIC_IP" "$IP_CACHE_FILE" | grep -v "LAST_SSH_KEY_FILENAME" > "${IP_CACHE_FILE}.tmp"
echo "LAST_APP_PUBLIC_IP=${APP_PUBLIC_IP}" >> "${IP_CACHE_FILE}.tmp"
echo "LAST_SSH_KEY_FILENAME=${SSH_KEY_FILENAME}" >> "${IP_CACHE_FILE}.tmp"
mv "${IP_CACHE_FILE}.tmp" "$IP_CACHE_FILE"

print_header "Connecting to the 'app' instance to create admin user..."

# SSH into the app EC2 instance and use 'docker exec' to run the admin creation tool
# inside the already running application container.
# The -t flag allocates a pseudo-terminal, which is required for the interactive Java Scanner.
ssh -t $SSH_OPTIONS -i "$SSH_KEY_PATH_EXPANDED" ubuntu@$APP_PUBLIC_IP '
  set -e

  # Find the running application container by name
  CONTAINER_NAME="tagfolio-app"
  CONTAINER_ID=$(docker ps -q -f "name=^/${CONTAINER_NAME}$")

  if [ -z "$CONTAINER_ID" ]; then
    echo "Error: The main application container ($CONTAINER_NAME) is not running."
    echo "Please ensure the application is deployed and running before creating an admin."
    exit 1
  fi

  echo "--- Found running application container: $CONTAINER_NAME ($CONTAINER_ID) ---"
  echo "--- Running admin creation tool inside the container ---"

  # Use docker exec to run the java command inside the container
  # -Dspring.main.web-application-type=none PREVENTS the web server from starting, avoiding port conflicts.
  # The -it flags are crucial for the interactive prompt to work.
  docker exec -it $CONTAINER_ID \
    java -Dspring.main.web-application-type=none -Dspring.profiles.active=prod,create-admin -jar app.jar

  echo "--- Admin creation tool finished ---"
'

echo ""
echo "✅ Admin creation process complete."
echo ""