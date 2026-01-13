FROM gvenzl/oracle-xe:latest

# Switch to root user to set permissions
USER root

COPY healthcheck.sh /healthcheck.sh

RUN chmod +x /healthcheck.sh

# Switch back to the default oracle user
USER oracle
