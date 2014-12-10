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
if [[ ! -z "${WILDFLY_ADMIN_USER}" ]] && [[ ! -z "${WILDFLY_ADMIN_PASSWORD}" ]]
then
  ${JBOSS_HOME}/bin/add-user.sh --silent -e -u ${WILDFLY_ADMIN_USER} -p ${WILDFLY_ADMIN_PASSWORD}
  unset ${WILDFLY_ADMIN_USER} ${WILDFLY_ADMIN_PASSWORD}
fi

if [[ ! -z "${HAWTIO_USER}" ]] && [[ ! -z "${HAWTIO_PASSWORD}" ]]
then
  ${JBOSS_HOME}/bin/add-user.sh --silent -e -a -u ${HAWTIO_USER} -p ${HAWTIO_PASSWORD}
  unset ${HAWTIO_USER} ${HAWTIO_PASSWORD}
fi

# Run startup script for chosen operating mode
exec ${JBOSS_HOME}/bin/${WILDFLY_OPERATING_MODE}.sh "$@"
