FROM ubuntu:17.10
MAINTAINER Jeffrey.N.Davis@gmail.com

RUN set -ex                                                       && \
  apt-get update && apt-get dist-upgrade -y && apt-get upgrade -y && \
  apt-get -y install bash \
    vim                   \
    curl                  \
    openjdk-8-jre

ARG VERSION
ENV VERSION=$VERSION

# Create App base directory
RUN mkdir -p /usr/local/src/balaam

RUN mkdir -p /usr/local/bin

#Install App
COPY ./target/universal/balaam-$VERSION.txz /tmp
RUN tar -Jxvf /tmp/balaam-$VERSION.txz -C /usr/local/src/balaam && \
  ln -s /usr/local/src/balaam/balaam-$VERSION/bin/balaam /usr/local/bin/balaam && \
  rm /tmp/balaam-$VERSION.txz

# It's Showtime
CMD /usr/local/bin/balaam -- -log.level=$LOG_LEVEL -darksky.api-key=$DARKSKY_API_KEY -http.port=$HTTP_PORT -db.host=$DB_HOST -db.port=$DB_PORT -db.name=$DB_NAME -db.user=$DB_USER -db.passwd=$DB_PASSWD
