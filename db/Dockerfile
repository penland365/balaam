FROM postgres:9.6
MAINTAINER Jeffrey.N.Davis@gmail.com

COPY ./balaam.sql /tmp/
RUN mkdir -p /docker-entrypoint-initdb.d
COPY ./initdb.sh /docker-entrypoint-initdb.d/initdb.sh
