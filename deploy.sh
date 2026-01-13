#!/bin/bash

# Exit immediately if a command exits with a non-zero status.
set -e

# Function to check if a command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Check for Docker
if ! command_exists docker; then
    echo "❌ Docker is not installed."
    read -p "Do you want to install Docker now? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "Installing Docker..."
        # Check for curl
        if ! command_exists curl; then
            echo "❌ curl is not installed."
            read -p "Do you want to install curl now? (y/n) " -n 1 -r
               echo
               if [[ $REPLY =~ ^[Yy]$ ]]; then
                   echo "Installing Curl..."
                   sudo apt-get update && sudo apt-get install -y curl
                   echo "Curl installed successfully."
               else
                   echo "Curl is required to install Docker. Exiting."
                   exit 1
               fi
        fi
        curl -fsSL https://get.docker.com -o get-docker.sh
        sudo sh get-docker.sh
        rm get-docker.sh
        echo "Docker installed successfully."
    else
        echo "Docker is required to run this deployment script. Exiting."
        exit 1
    fi
fi

# Check if current user can run docker commands
if ! docker info >/dev/null 2>&1; then
    echo "❌ Error: Unable to connect to the Docker daemon."
    echo "This is likely because your user '$USER' is not in the 'docker' group."

    read -p "Do you want to add '$USER' to the 'docker' group now? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "Adding user '$USER' to 'docker' group..."
        sudo usermod -aG docker "$USER"
        echo "✅ User added to 'docker' group."

        echo "🔄 Restarting script with new group permissions..."
        # Use 'sg' to execute the script again with the 'docker' group active.
        # "$0" is the script name, "$@" passes any arguments.
        exec sg docker -c "/bin/bash $0 $@"
    else
        echo "You must have Docker permissions to run this script. Exiting."
        exit 1
    fi
fi

echo "### 1. Stopping existing services... ###"
docker compose down

echo "### 2. Pulling latest changes from Git... ###"
git pull
git log --all --oneline --graph  --max-count=5

echo "### 3. Building images and starting services... ###"
# --build forces a rebuild of the image, which triggers the multi-stage build
docker compose up --build -d

echo "### 4. Cleaning up old Docker images... ###"
docker image prune -f

echo ""
echo "✅ Deployment complete. Application is starting up."
echo "To view logs, run: docker compose logs -f"