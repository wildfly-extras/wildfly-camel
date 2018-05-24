FROM openjdk:8-jre-slim

ENV NARAYANA_VERSION=5.8.2.Final

RUN apt update && \
    apt install curl -y && \
    apt clean && \
    curl -L http://downloads.jboss.org/jbosstm/${NARAYANA_VERSION}/binary/narayana-full-${NARAYANA_VERSION}-bin.zip > /tmp/narayana.zip && \
    mkdir /opt/lra && \
    unzip /tmp/narayana.zip && \
    cp narayana-full-${NARAYANA_VERSION}/rts/lra/lra-coordinator-swarm.jar /opt/lra/ && \
    rm -rf narayana-full-${NARAYANA_VERSION} /tmp/narayana.zip

CMD [ "java", "-jar", "/opt/lra/lra-coordinator-swarm.jar", "-Djava.net.preferIPv4Stack=true", "-Dswarm.http.port=46000" ]
