FROM jboss/base-jdk:8

LABEL maintainer="ppalaga@redhat.com"

ENV DRILL_VERSION 1.10.0
ENV DRILL_SHA1 d2eb15be1b7774d631efd07b2c6ac67db4a3821e
ENV DRILL_HOME /opt/jboss/apache-drill

RUN cd $HOME \
    && curl -Lo apache-drill-${DRILL_VERSION}.tar.gz "http://www.apache.org/dyn/closer.lua?filename=drill/drill-${DRILL_VERSION}/apache-drill-${DRILL_VERSION}.tar.gz&action=download" \
    && sha1sum apache-drill-$DRILL_VERSION.tar.gz | grep $DRILL_SHA1 \
    && tar xf apache-drill-$DRILL_VERSION.tar.gz \
    && mv $HOME/apache-drill-$DRILL_VERSION $DRILL_HOME \
    && rm apache-drill-$DRILL_VERSION.tar.gz

WORKDIR $DRILL_HOME

EXPOSE 8047 31010

ENTRYPOINT ["/opt/jboss/apache-drill/bin/drill-embedded"]