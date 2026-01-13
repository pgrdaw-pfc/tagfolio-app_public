# Local Production Deployment Guide

This guide provides comprehensive instructions for deploying the application to a local production server using Docker.

## Part 1: Server Setup

This part covers the initial setup of the server that will run the Docker containers.

### 1. Prerequisites

*   A server or local machine (Linux recommended).
*   **Note**: Git, Docker, Docker Compose, and curl will be installed automatically by the deployment script if they are not present (on apt-based systems).
*   Java is **not** required on the host machine as the build process runs inside a container.

### 2. Clone the Repository

Clone the project repository to your server.

```bash
git clone https://github.com/pgrdaw-pfc/tagfolio-app_public.git
cd tagfolio-app_public
```

### 3. Configure Environment Variables

You must create a `.env` file in the project root to store your database credentials. You can use the provided example file as a template.

1.  Copy the example file:
    ```bash
    cp .env.example .env
    ```

2.  Edit the file with your actual credentials:
    ```bash
    nano .env
    ```

3.  Save the file and exit (press **Ctrl+X**, then **Y**, then **Enter**).

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
        *   **Check for prerequisites**: Verifies Git, Docker, and Docker Compose (V2) are installed. Prompts to install them if missing.
        *   **Check configuration**: Verifies that `.env` exists and is not empty.
        *   **Check if the current user has Docker permissions**. If not, it will offer to add the user to the `docker` group and automatically restart the script with the new permissions (so you don't need to log out).
        *   **Check git status**: Ensures there are no uncommitted changes before pulling.
        *   Stop any existing services (and remove orphan containers).
        *   Pull the latest changes from `git`.
        *   **Build the application `jar` file inside a Docker container** (Multi-stage build).
        *   Build and start the `app` and `db` Docker containers.
        *   Display the status of the running services.

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
