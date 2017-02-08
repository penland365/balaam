FROM fedora:25
MAINTAINER Jeffrey.Davis@sabre.com

RUN dnf upgrade --setopt=deltarpm=false -y && \
  dnf install --setopt=deltarpm=false -y tar xz java-1.8.0-openjdk && \
  dnf clean all --setopt=deltarpm=false -y

ARG BALAAM_VERSION
ENV BALAAM_VERSION=$BALAAM_VERSION

# Create App directory
RUN mkdir /usr/local/src/balaam

# Copy jar
COPY ./target/balaam-$BALAAM_VERSION-standalone.jar /usr/local/src/balaam

# It's showtime
CMD PORT=$HTTP_PORT GOOGLE_API_KEY=$GOOGLE_API_KEY DARKSKY_API_KEY=$DARKSKY_API_KEY java -jar /usr/local/src/balaam/balaam-$BALAAM_VERSION-standalone.jar
