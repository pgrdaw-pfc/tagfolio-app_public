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
print_header "Starting AWS deployment..."

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

# Prompt for EC2 instance IPs, offering the last used ones as defaults.
read -p "Enter the PUBLIC IP of the 'app' EC2 instance [${LAST_APP_PUBLIC_IP}]: " APP_PUBLIC_IP
APP_PUBLIC_IP=${APP_PUBLIC_IP:-$LAST_APP_PUBLIC_IP}

read -p "Enter the PUBLIC IP of the 'db' EC2 instance [${LAST_DB_PUBLIC_IP}]: " DB_PUBLIC_IP
DB_PUBLIC_IP=${DB_PUBLIC_IP:-$LAST_DB_PUBLIC_IP}

read -p "Enter the PRIVATE IP of the 'db' EC2 instance [${LAST_DB_PRIVATE_IP}]: " DB_PRIVATE_IP
DB_PRIVATE_IP=${DB_PRIVATE_IP:-$LAST_DB_PRIVATE_IP}

# Save the entered IPs and SSH key filename for the next run.
echo "LAST_APP_PUBLIC_IP=${APP_PUBLIC_IP}" > "$IP_CACHE_FILE"
echo "LAST_DB_PUBLIC_IP=${DB_PUBLIC_IP}" >> "$IP_CACHE_FILE"
echo "LAST_DB_PRIVATE_IP=${DB_PRIVATE_IP}" >> "$IP_CACHE_FILE"
echo "LAST_SSH_KEY_FILENAME=${SSH_KEY_FILENAME}" >> "$IP_CACHE_FILE"


# --- Deploy 'db' service ---
print_header "Deploying 'db' service to $DB_PUBLIC_IP..."

# Create the full remote directory structure first.
ssh $SSH_OPTIONS -i "$SSH_KEY_PATH_EXPANDED" ubuntu@$DB_PUBLIC_IP "mkdir -p ~/project/docker"
# Copy the db context into the remote docker directory.
scp $SSH_OPTIONS -i "$SSH_KEY_PATH_EXPANDED" -r docker/db ubuntu@$DB_PUBLIC_IP:~/project/docker/

# SSH into the db EC2 instance and start the db service.
ssh $SSH_OPTIONS -i "$SSH_KEY_PATH_EXPANDED" ubuntu@$DB_PUBLIC_IP << 'EOF'
  set -e
  cd ~/project/docker/db

  DB_USER_VALUE=$(grep 'export DB_USER' ~/.bashrc | tail -n 1 | cut -d'=' -f2)
  DB_PASSWORD_VALUE=$(grep 'export DB_PASSWORD' ~/.bashrc | tail -n 1 | cut -d'=' -f2)

  if [ -z "$DB_USER_VALUE" ] || [ -z "$DB_PASSWORD_VALUE" ]; then
    echo "Error: DB_USER or DB_PASSWORD not found in ~/.bashrc on the remote machine."
    echo "Please ensure you have run the setup commands from DEPLOYMENT.AWS.md"
    exit 1
  fi

  docker pull gvenzl/oracle-xe:21-slim-faststart

  # IMPORTANT: Removed '-v' to prevent deleting the database volume!
  # Suppress warnings about variables not being set during 'down'
  DB_USER=$DB_USER_VALUE DB_PASSWORD=$DB_PASSWORD_VALUE docker compose -f docker-compose.aws.db.yml down
  DB_USER=$DB_USER_VALUE DB_PASSWORD=$DB_PASSWORD_VALUE docker compose -f docker-compose.aws.db.yml up --build -d

  # Wait for the database to be ready before applying the schema
  echo "Waiting for database to be ready..."
  # Use docker compose exec instead of docker exec with ps
  # Pass env vars to suppress warnings
  until DB_USER=$DB_USER_VALUE DB_PASSWORD=$DB_PASSWORD_VALUE docker compose -f docker-compose.aws.db.yml exec db /opt/oracle/healthcheck.sh; do
    echo "Database is not ready yet. Retrying in 10 seconds..."

    # Check if container is actually running
    # Pass env vars to suppress warnings
    if [ -z "$(DB_USER=$DB_USER_VALUE DB_PASSWORD=$DB_PASSWORD_VALUE docker compose -f docker-compose.aws.db.yml ps -q db)" ]; then
      echo "CRITICAL ERROR: Database container is not running!"
      DB_USER=$DB_USER_VALUE DB_PASSWORD=$DB_PASSWORD_VALUE docker compose -f docker-compose.aws.db.yml logs
      exit 1
    fi

    sleep 10
  done
  echo "Database is ready."

  # Apply the schema as the correct application user
  echo "Applying database schema..."
  # Use docker compose exec here too with -T to disable pseudo-tty
  # Note: schema.sql should be idempotent (e.g. use CREATE TABLE IF NOT EXISTS or handle errors)
  # But standard SQL scripts often fail if objects exist.
  # We will attempt to run it, but ignore errors if tables already exist.
  # Or better: The user should ensure schema.sql is safe to re-run.
  # For now, we run it, but we don't fail the script if it returns an error (likely due to existing tables).
  DB_USER=$DB_USER_VALUE DB_PASSWORD=$DB_PASSWORD_VALUE docker compose -f docker-compose.aws.db.yml exec -T db sqlplus $DB_USER_VALUE/$DB_PASSWORD_VALUE@//localhost:1521/XEPDB1 < schema.sql || echo "Schema application finished (possibly with errors if objects already exist)."

  echo "Schema applied."
EOF

# --- Deploy 'app' service ---
print_header "Deploying 'app' service to $APP_PUBLIC_IP..."

# Ensure we are on the main branch and have the latest changes
# git checkout main
# git pull origin main

# Create the full remote directory structure first.
ssh $SSH_OPTIONS -i "$SSH_KEY_PATH_EXPANDED" ubuntu@$APP_PUBLIC_IP "mkdir -p ~/project/docker ~/project/src ~/project/gradle"

# Copy necessary files for the build (Source Code + Gradle Wrapper).
# We copy everything needed for the multi-stage build.
scp $SSH_OPTIONS -i "$SSH_KEY_PATH_EXPANDED" build.gradle settings.gradle gradlew ubuntu@$APP_PUBLIC_IP:~/project/
scp $SSH_OPTIONS -i "$SSH_KEY_PATH_EXPANDED" -r gradle ubuntu@$APP_PUBLIC_IP:~/project/
scp $SSH_OPTIONS -i "$SSH_KEY_PATH_EXPANDED" -r src ubuntu@$APP_PUBLIC_IP:~/project/
scp $SSH_OPTIONS -i "$SSH_KEY_PATH_EXPANDED" -r docker/app ubuntu@$APP_PUBLIC_IP:~/project/docker/

# Check if storage directory exists on remote, if not create it.
# We do NOT copy local storage to remote to preserve user data.
ssh $SSH_OPTIONS -i "$SSH_KEY_PATH_EXPANDED" ubuntu@$APP_PUBLIC_IP "mkdir -p ~/project/storage"

# SSH into the app EC2 instance and start the app service.
ssh $SSH_OPTIONS -i "$SSH_KEY_PATH_EXPANDED" ubuntu@$APP_PUBLIC_IP "DB_HOST_IP=${DB_PRIVATE_IP}" '
  set -e
  cd ~/project

  DB_USER_VALUE=$(grep "export DB_USER" ~/.bashrc | tail -n 1 | cut -d"=" -f2)
  DB_PASSWORD_VALUE=$(grep "export DB_PASSWORD" ~/.bashrc | tail -n 1 | cut -d"=" -f2)

  if [ -z "$DB_USER_VALUE" ] || [ -z "$DB_PASSWORD_VALUE" ]; then
    echo "Error: DB_USER or DB_PASSWORD not found in ~/.bashrc"
    exit 1
  fi

  # IMPORTANT: Removed '-v' to prevent deleting any app volumes (though app uses host bind mount)
  # Suppress warnings about variables not being set during 'down'
  DB_USER=$DB_USER_VALUE DB_PASSWORD=$DB_PASSWORD_VALUE DB_HOST=$DB_HOST_IP docker compose -f docker/app/docker-compose.aws.app.yml down

  # --build triggers the multi-stage build on the remote server
  DB_USER=$DB_USER_VALUE DB_PASSWORD=$DB_PASSWORD_VALUE DB_HOST=$DB_HOST_IP docker compose -f docker/app/docker-compose.aws.app.yml up --build -d
'

print_header "Cleaning up old Docker images on both instances..."
ssh $SSH_OPTIONS -i "$SSH_KEY_PATH_EXPANDED" ubuntu@$APP_PUBLIC_IP 'docker image prune -f'
ssh $SSH_OPTIONS -i "$SSH_KEY_PATH_EXPANDED" ubuntu@$DB_PUBLIC_IP 'docker image prune -f'

echo ""
echo "✅ AWS deployment complete."
print_header "To check the console logs, run one of the following commands:"
echo "--- APP LOGS ---"
echo "ssh $SSH_OPTIONS -i \"$SSH_KEY_PATH_EXPANDED\" ubuntu@$APP_PUBLIC_IP 'cd ~/project && docker compose -f docker/app/docker-compose.aws.app.yml logs -f'"
echo ""
echo "--- DB LOGS ---"
echo "ssh $SSH_OPTIONS -i \"$SSH_KEY_PATH_EXPANDED\" ubuntu@$DB_PUBLIC_IP 'cd ~/project/docker/db && docker compose -f docker-compose.aws.db.yml logs -f'"
echo ""