FROM fedora:25
MAINTAINER Jeffrey.N.Davis@gmail.com

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
CMD PORT=$HTTP_PORT GOOGLE_API_KEY=$GOOGLE_API_KEY DARKSKY_API_KEY=$DARKSKY_API_KEY DATABASE_HOSTNAME=$DATABASE_HOSTNAME DATABASE_PORT=$DATABASE_PORT DATABASE=$DATABASE DATABASE_USERNAME=$DATABASE_USERNAME DATABASE_PASSWORD=$DATABASE_PASSWORD SLACK_CLIENT_ID=$SLACK_CLIENT_ID SLACK_CLIENT_SECRET=$SLACK_CLIENT_SECRET SLACK_REDIRECT_URI=$SLACK_RECIRECT_URI java -jar /usr/local/src/balaam/balaam-$BALAAM_VERSION-standalone.jar
