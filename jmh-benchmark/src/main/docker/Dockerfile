FROM openjdk:11

ARG  VERSION

ENV  VERSION $VERSION
ENV  JAVA_OPTS "-Dfile.encoding=UTF-8"

WORKDIR /jmh/

USER root

RUN  mkdir -p /jmh

ADD  entry-point.sh /jmh/
ADD  maven/*.jar /jmh/

RUN   chmod +x /jmh/entry-point.sh \
      && echo "Europe/Moscow" > /etc/timezone

RUN  useradd -m -d /jmh  -s /bin/bash jmh \
     && chown -R jmh /jmh \
     && chmod -R "g+rwX" /jmh

USER jmh

WORKDIR "/jmh"
ENTRYPOINT  ["sh","./entry-point.sh"]
