# Local Production Deployment Guide

This guide provides comprehensive instructions for deploying the application to a local production server using Docker.

## Part 1: Server Setup

This part covers the initial setup of the server that will run the Docker containers.

### 1. Prerequisites

*   A server or local machine (Linux recommended).
*   Install Git on the server.
```bash
sudo apt update -y && sudo apt install git -y
```
*   **Note**: Docker (and curl) will be installed automatically by the deployment script if they are not present. Java is **not** required on the host machine as the build process runs inside a container.

### 2. Clone the Repository

Clone the project repository to your server.

```bash
git clone https://github.com/pgrdaw-pfc/tagfolio-app_public.git
cd tagfolio-app_public
```

### 3. Configure Environment Variables

You must create a `.env` file in the project root to store your database credentials. This file is ignored by Git and keeps your secrets out of version control.

1.  Create the file:
    ```bash
    nano .env
    ```

2.  Add the following content, replacing the placeholder values with your actual database credentials:
    ```
    DB_USER=your_db_user
    DB_PASSWORD=your_strong_password
    ```

3.  Save the file and exit `nano` (press **Ctrl+X**, then **Y**, then **Enter**).

---

## Part 2: Application Deployment

After setting up the server and environment, you can deploy the application.

### Deployment Steps

1.  **Run the Local Deployment Script**:
    *   From the project root, make the script executable:
        ```bash
        chmod +x deploy.sh
        ```
    *   Run the script:
        ```bash
        ./deploy.sh
        ```
    *   The script will:
        *   Check if `docker` is installed (and prompt to install it if missing).
        *   Check if `curl` is installed (and prompt to install it if missing, as it is required to install Docker).
        *   **Check if the current user has Docker permissions**. If not, it will offer to add the user to the `docker` group and automatically restart the script with the new permissions (so you don't need to log out).
        *   Stop any existing services.
        *   Pull the latest changes from `git`.
        *   **Build the application `jar` file inside a Docker container** (Multi-stage build).
        *   Build and start the `app` and `db` Docker containers.

2.  **Verify the Deployment**:
    *   After the script completes, the application will be accessible at `http://localhost/` or at `http://<your_server_ip>/`.
    *   You can view the logs for the services by running:
        ```bash
        docker compose logs -f
        ```

---

## Part 3: Create Initial Admin User

After the first successful deployment, if you need to create the initial administrator account, proceed as follows:

1.  **Run the Admin Creation Script**:
    *   The `CreateAdminUser.java` tool is designed to be run with the `create-admin` profile. The easiest way to do this is to temporarily modify your `docker-compose.yml` file.
    *   Open `docker-compose.yml` and find the `environment` section for the `app` service.
    *   Change `SPRING_PROFILES_ACTIVE=prod` to `SPRING_PROFILES_ACTIVE=prod,create-admin`.
    *   Run `docker compose up -d --build app`.
    *   The application will start and prompt you in the console logs to enter the admin email and password. You can view this with `docker compose logs -f app`.
    *   **IMPORTANT**: After creating the admin user, remember to change the profile back to `SPRING_PROFILES_ACTIVE=prod` and run `docker compose up -d --build app` again to return the application to its normal state.
