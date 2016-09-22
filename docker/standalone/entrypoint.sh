#!/bin/sh

# Create default app server users
if [[ ! -z "${WILDFLY_MANAGEMENT_USER}" ]] && [[ ! -z "${WILDFLY_MANAGEMENT_PASSWORD}" ]]
then
  ${JBOSS_HOME}/bin/add-user.sh --silent -e -u ${WILDFLY_MANAGEMENT_USER} -p ${WILDFLY_MANAGEMENT_PASSWORD}
fi

if [[ ! -z "${WILDFLY_APPLICATION_USER}" ]] && [[ ! -z "${WILDFLY_APPLICATION_PASSWORD}" ]]
then
  ${JBOSS_HOME}/bin/add-user.sh --silent -e -a -u ${WILDFLY_APPLICATION_USER} -p ${WILDFLY_APPLICATION_PASSWORD}
fi

# Unset the temporary env variables
unset ${WILDFLY_MANAGEMENT_USER} ${WILDFLY_MANAGEMENT_PASSWORD}
unset ${WILDFLY_APPLICATION_USER} ${WILDFLY_APPLICATION_PASSWORD}

# Run startup script for chosen operating mode
exec ${JBOSS_HOME}/bin/standalone.sh "$@"
