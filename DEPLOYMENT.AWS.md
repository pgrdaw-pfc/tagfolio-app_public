# AWS Deployment Guide

This guide provides comprehensive instructions for deploying the application to AWS, including setting up EC2 instances and running the deployment script.

## Part 1: EC2 Instance and Security Group Setup

This part covers the creation of the necessary AWS infrastructure.

### 1. Create a Key Pair

A key pair is required to securely connect to your EC2 instances via SSH.

1.  Navigate to the **EC2 Dashboard** in the AWS Management Console.
2.  In the left navigation pane, under **Network & Security**, click on **Key Pairs**.
3.  Click **Create key pair**.
4.  Enter a name (e.g., `tagfolio-key`).
5.  Choose **.pem** for OpenSSH on macOS/Linux or **.ppk** for PuTTY on Windows.
6.  Click **Create key pair**. The key file will be downloaded automatically.
7.  Move the downloaded key file to a secure location (e.g., `~/.ssh/`) and set its permissions:
    ```bash
    chmod 400 /path/to/your/tagfolio-key.pem
    ```

### 2. Create Security Groups

Security groups act as virtual firewalls. We will create two: one for the `app` instance and one for the `db` instance.

#### a. Create the `app-sg` Security Group

1.  In the EC2 Dashboard, under **Network & Security**, click on **Security Groups**.
2.  Click **Create security group**.
3.  **Name**: `app-sg`.
4.  **Description**: `Security group for the Tagfolio application instance`.
5.  **Inbound rules**:
    *   Click **Add rule**.
        *   **Type**: `SSH` (TCP, Port 22).
        *   **Source**: `Anywhere-IPv4` (`0.0.0.0/0`). This is safe as access is protected by your key file.
    *   Click **Add rule**.
        *   **Type**: `HTTP` (TCP, Port 80).
        *   **Source**: `Anywhere-IPv4` (`0.0.0.0/0`).
6.  Click **Create security group**.

#### b. Create the `db-sg` Security Group

1.  Follow the same steps to create another security group.
2.  **Name**: `db-sg`.
3.  **Description**: `Security group for the Tagfolio database instance`.
4.  **Inbound rules**:
    *   Click **Add rule**.
        *   **Type**: `SSH` (TCP, Port 22).
        *   **Source**: `Anywhere-IPv4` (`0.0.0.0/0`).
    *   Click **Add rule**.
        *   **Type**: `Custom TCP`.
        *   **Port range**: `1521`.
        *   **Source**: Select the `app-sg` security group you just created. This ensures that only the `app` instance can communicate with the database.
5.  Click **Create security group**.

### 3. Launch EC2 Instances

Now, launch two EC2 instances.

1.  In the EC2 Dashboard, click **Launch instances**.
2.  **Name and tags**:
    *   For the first instance, name it `tagfolio-app`.
    *   For the second instance, name it `tagfolio-db`.
3.  **Application and OS Images (AMI)**:
    *   Select **Ubuntu**.
    *   Choose **Ubuntu Server 22.04 LTS** (or a recent LTS version).
4.  **Instance type**:
    *   **`tagfolio-app`**: `t3.small` (2 GB RAM) is recommended.
    *   **`tagfolio-db`**: **CRITICAL!** You must select an instance with at least 4 GB of RAM. `t3.medium` is the recommended minimum. An instance with less RAM will cause the database to crash or become unresponsive.
5.  **Key pair (login)**:
    *   Select the `tagfolio-key` you created earlier.
6.  **Network settings**:
    *   Click **Edit**.
    *   **For the `tagfolio-app` instance**: Under **Firewall (security groups)**, choose **Select existing security group** and select `app-sg`.
    *   **For the `tagfolio-db` instance**: Under **Firewall (security groups)**, choose **Select existing security group** and select `db-sg`.
7.  **Configure storage**:
    *   **For both instances**: Change the size from 8 to **30 GiB**. This provides sufficient space for the OS, Docker images, and user data.
8.  **Launch two instances**: In the **Summary** panel, change the **Number of instances** to `2` and click **Launch instance**.
9.  Go to your instances list. Note down the **Public IPv4 address** and **Private IPv4 address** for each instance.

### 4. Configure Instances

For each new instance, perform the following steps.

1.  **Connect via SSH**:
    ```bash
    ssh -i /path/to/your/tagfolio-key.pem ubuntu@<INSTANCE_PUBLIC_IP>
    ```

2.  **Run the Setup Script**: Once connected, run the following commands to install Docker and set up the environment variables. **Remember to replace `your_db_user` and `your_db_password` with your actual database credentials.**

    ```bash
    #!/bin/bash
    
    # --- Update and Install Dependencies ---
    sudo apt-get update -y
    sudo apt-get install -y ca-certificates curl gnupg
    
    # --- Install Docker ---
    sudo install -m 0755 -d /etc/apt/keyrings
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
    sudo chmod a+r /etc/apt/keyrings/docker.gpg
    echo \
      "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
      $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
      sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
    sudo apt-get update -y
    sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
    
    # Add ubuntu user to the docker group to run docker without sudo
    sudo usermod -aG docker ubuntu
    
    # --- Set Environment Variables ---
    echo "export DB_USER=tagfolio" >> ~/.bashrc
    echo "export DB_PASSWORD=tagfolio" >> ~/.bashrc
    
    echo "--------------------------------------------------"
    echo "✅ Setup complete."
    echo "Please log out and log back in for all changes to take effect."
    echo "--------------------------------------------------"
    ```

3.  **Log out and log back in** to apply the group and environment variable changes.

---

## Part 2: Application Deployment

After setting up the EC2 instances, you can deploy the application from your local machine.

### Deployment Steps

1.  **Run the AWS Deployment Script**:
    *   From the project root on your local machine, make the script executable:
        ```bash
        chmod +x deploy.aws.sh
        ```
    *   Run the script:
        ```bash
        ./deploy.aws.sh
        ```
    *   The script will prompt you for the public and private IP addresses of your new `app` and `db` EC2 instances.
    *   **Note**: The script will copy the source code to the AWS instance and build the application there using Docker. No local Java installation is required.

2.  **Verify the Deployment**:
    *   After the script completes, wait 1-2 minutes for the database and application to initialize.
    *   Access the application by navigating to the public IP address of your `app` EC2 instance.
    *   You can view the logs for each service using the `ssh` commands provided in the script's output.

---

## Part 3: Create Initial Admin User

After the first successful deployment, you need to create the initial administrator account.

1.  **Run the Admin Creation Script**:
    *   From the project root on your local machine, make the script executable:
        ```bash
        chmod +x create-admin.aws.sh
        ```
    *   Run the script:
        ```bash
        ./create-admin.aws.sh
        ```
    *   The script will connect to the `app` instance and prompt you to enter the email and password for the new admin user.

2.  **Login**: Once the admin user is created, you can log in to the application.
