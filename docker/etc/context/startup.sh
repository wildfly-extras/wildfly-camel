#!/bin/sh

# Work out our operating mode
WILDFLY_OPERATING_MODE=standalone

for ARG in "$@"
do
    # Assume domain mode if domain config args have been given
    if [[ "${ARG}" =~ jboss\.domain|domain-config|host-config ]]
    then
        WILDFLY_OPERATING_MODE=domain
    fi
done

# Create default app server users
if [[ ! -z "${WILDFLY_MANAGEMENT_USER}" ]] && [[ ! -z "${WILDFLY_MANAGEMENT_PASSWORD}" ]]
then
  ${JBOSS_HOME}/bin/add-user.sh --silent -e -u ${WILDFLY_MANAGEMENT_USER} -p ${WILDFLY_MANAGEMENT_PASSWORD}
  sed -i "s/@WILDFLY_MANAGEMENT_USER@/${WILDFLY_MANAGEMENT_USER}/" ${JBOSS_HOME}/domain/configuration/host-camel-slave.xml
  sed -i "s/@WILDFLY_MANAGEMENT_PASSWORD@/`echo ${WILDFLY_MANAGEMENT_PASSWORD} | base64`/" ${JBOSS_HOME}/domain/configuration/host-camel-slave.xml
  unset ${WILDFLY_MANAGEMENT_USER} ${WILDFLY_MANAGEMENT_PASSWORD}
fi

if [[ ! -z "${WILDFLY_APPLICATION_USER}" ]] && [[ ! -z "${WILDFLY_APPLICATION_PASSWORD}" ]]
then
  ${JBOSS_HOME}/bin/add-user.sh --silent -e -a -u ${WILDFLY_APPLICATION_USER} -p ${WILDFLY_APPLICATION_PASSWORD}
  unset ${WILDFLY_APPLICATION_USER} ${WILDFLY_APPLICATION_PASSWORD}
fi

# Run startup script for chosen operating mode
exec ${JBOSS_HOME}/bin/${WILDFLY_OPERATING_MODE}.sh "$@"
