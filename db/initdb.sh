#!/bin/sh

set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    CREATE USER moses WITH PASSWORD 'opensesame';
    CREATE DATABASE numbers;
    GRANT ALL PRIVILEGES ON DATABASE numbers TO moses;
EOSQL

# create balaam schema
psql -d numbers -U moses -f /tmp/balaam.sql
