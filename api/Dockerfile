FROM fedora:26
MAINTAINER Jeffrey.N.Davis@gmail.com

RUN set -ex && \
  yum upgrade -y && \
  yum install -y java-1.8.0-openjdk && \
  yum install -y xz && \
  yum install -y xz-devel

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
EXPOSE 7171
CMD /usr/local/bin/balaam -- -log.level=$LOG_LEVEL -darksky.api-key=$DARKSKY_API_KEY -http.port=$HTTP_PORT -db.host=$DATABASE_HOST -db.port=$DATABASE_PORT
