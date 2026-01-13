#!/bin/bash

# Script for local/test deployment of Tagfolio
# This script is designed to be robust and self-contained.
set -euo pipefail

# Function to check if a command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Function to check for and offer to install a package using apt-get
install_package_with_apt() {
    local package_name=$1
    local purpose=$2

    if ! command_exists "$package_name"; then
        echo "❌ $package_name is not installed."
        read -p "Do you want to install $package_name now? (y/n) " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            echo "Installing $package_name..."
            if command_exists apt-get; then
                sudo apt-get update && sudo apt-get install -y "$package_name"
                echo "✅ $package_name installed successfully."
            else
                echo "❌ Automatic installation of $package_name is only supported on apt-based systems. Please install it manually."
                exit 1
            fi
        else
            echo "$package_name is required for $purpose. Exiting."
            exit 1
        fi
    fi
}

# Check for Git
install_package_with_apt "git" "pulling the latest changes"

# Check for .env file
if [ ! -f .env ]; then
    echo "❌ Error: '.env' file not found."
    echo "This file is required to set database credentials (DB_USER, DB_PASSWORD)."
    echo "Please create a '.env' file. You can use '.env.example' as a template if one exists."
    exit 1
elif [ ! -s .env ]; then
    echo "❌ Error: '.env' file is empty."
    echo "Please populate it with the required environment variables."
    exit 1
fi

# Check for Docker
if ! command_exists docker; then
    echo "❌ Docker is not installed."
    read -p "Do you want to install Docker now? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "Installing Docker..."
        install_package_with_apt "curl" "installing Docker"
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

# Check for Docker Compose V2 plugin
if ! docker compose version >/dev/null 2>&1; then
    echo "❌ 'docker compose' command not found (Docker Compose V2 plugin)."
    read -p "Do you want to install it now? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "Installing Docker Compose plugin..."
        if command_exists apt-get; then
            # The package name for the V2 plugin is docker-compose-plugin
            sudo apt-get update && sudo apt-get install -y docker-compose-plugin
            echo "✅ Docker Compose plugin installed successfully."
        else
            echo "❌ Automatic installation is only supported on apt-based systems. Please install the 'docker-compose-plugin' package manually."
            exit 1
        fi
    else
        echo "Docker Compose V2 is required to run this script. Exiting."
        exit 1
    fi
fi

echo "### 1. Stopping existing services... ###"
# Use --remove-orphans to clean up any containers not defined in the compose file.
# The '|| true' prevents the script from exiting if no containers are running.
docker compose down --remove-orphans || true

echo "### 2. Pulling latest changes from Git... ###"
if ! git diff --quiet HEAD; then
    echo "❌ Your local repository has uncommitted changes. Please commit or stash them before deploying."
    exit 1
fi
git pull
git log --all --oneline --graph  --max-count=5

echo "### 3. Building images and starting services... ###"
# --build forces a rebuild of the image, which triggers the multi-stage build
docker compose up --build -d

echo "### 4. Cleaning up old Docker images... ###"
docker image prune -f

echo ""
echo "✅ Deployment complete. Application is starting up."
echo "-------------------------------------------------"
echo "Current status of services:"
docker compose ps
echo "-------------------------------------------------"
echo "To view logs, run: docker compose logs -f"