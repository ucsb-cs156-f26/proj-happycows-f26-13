#!/bin/bash
if [ -n "$DOKKU_POSTGRES_AQUA_URL" ]; then
DATABASE_URL="$DOKKU_POSTGRES_AQUA_URL"
fi

export JDBC_DATABASE_PASSWORD=$(printf '%s\n' "$DATABASE_URL" | cut -d ':' -f3 | cut -d '@' -f1)

export JDBC_DATABASE_URL=jdbc:postgresql://$(printf '%s\n' "$DATABASE_URL" | cut -d '@' -f2)

export JDBC_DATABASE_USERNAME=postgres

exec java -jar "$1"
