#!/bin/sh

if [ "x$DB_URI" = "x" ]; then
    DB_URI=jdbc:h2:mem:LOGSTORE;DB_CLOSE_ON_EXIT=TRUE
fi

exec java -Djava.security.egd=file:/dev/.urandom -Dspring.datasource.url="$DB_URI" -Dspring.datasource.username="$DB_USER" -Dspring.datasource.password="$DB_PASSWORD" -jar /app.jar
