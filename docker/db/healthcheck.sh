#!/bin/bash
# Exit with a non-zero status if the database is not yet ready.
sqlplus -S / as sysdba <<-EOF
  whenever sqlerror exit 1;
  select 1 from v\$database;
  exit;
EOF
