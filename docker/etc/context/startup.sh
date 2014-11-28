#!/bin/sh

if [[ ${WILDFLY_OPERATING_MODE} == "domain" ]] || [[ ${WILDFLY_OPERATING_MODE} == "standalone" ]]
then
  readonly BASE_DIR=${WILDFLY_BASE_DIR:-${JBOSS_HOME}/${WILDFLY_OPERATING_MODE}}
  readonly OPERATING_MODE=$(echo ${WILDFLY_OPERATING_MODE} | tr '[:lower:]' '[:upper:]')
  readonly PROPERTY_PREFIX=$([ "$WILDFLY_OPERATING_MODE" == "standalone" ] && echo "server" || echo "domain")
  readonly WILDFLY_EXTRAS=${JBOSS_HOME}/extras
  readonly WILDFLY_PROPERTIES=${WILDFLY_EXTRAS}/wildfly.properties

  # Setup app server properties from env vars (if present) or use defaults
  cat << EOF > ${WILDFLY_PROPERTIES}
jboss.${PROPERTY_PREFIX}.base.dir=${BASE_DIR:-${JBOSS_HOME}/${WILDFLY_OPERATING_MODE}}
jboss.${PROPERTY_PREFIX}.config.dir=${WILDFLY_CONFIG_DIR:-${BASE_DIR}/configuration}
jboss.${PROPERTY_PREFIX}.data.dir=${WILDFLY_DATA_DIR:-${BASE_DIR}/data}
jboss.${PROPERTY_PREFIX}.log.dir=${WILDFLY_LOG_DIR:-${BASE_DIR}/log}
jboss.${PROPERTY_PREFIX}.temp.dir=${WILDFLY_TEMP_DIR:-${BASE_DIR}/tmp}
jboss.${PROPERTY_PREFIX}.deploy.dir=${WILDFLY_DEPLOYMENT_DIR:-${BASE_DIR}/content}
EOF

  # Add servers.dir property if running domain mode
  if [[ ${WILDFLY_OPERATING_MODE} == "domain" ]]
  then
    echo "jboss.${WILDFLY_OPERATING_MODE}.servers.dir=${WILDFLY_DOMAIN_SERVERS_DIR:-${BASE_DIR}/servers}" >> ${WILDFLY_PROPERTIES}
  fi

  # Create default app server users
  if [[ ! -z "${WILDFLY_ADMIN_USER}" ]] && [[ ! -z "${WILDFLY_ADMIN_PASSWORD}" ]]
  then
      ${JBOSS_HOME}/bin/add-user.sh --silent -e -u ${WILDFLY_ADMIN_USER} -p ${WILDFLY_ADMIN_PASSWORD}
  fi

  if [[ ! -z "${HAWTIO_USER}" ]] && [[ ! -z "${HAWTIO_PASSWORD}" ]]
  then
      ${JBOSS_HOME}/bin/add-user.sh --silent -e -a -u ${HAWTIO_USER} -p ${HAWTIO_PASSWORD}
  fi

  # Run startup script for chosen operating mode
 exec ${JBOSS_HOME}/bin/${WILDFLY_OPERATING_MODE}.sh -c ${WILDFLY_CONFIG_FILE:-${WILDFLY_OPERATING_MODE}-camel.xml} \
                                                     -b ${WILDFLY_BIND_ADDRESS:-"0.0.0.0"} \
                                                     -bmanagement ${WILDFLY_MANAGEMENT_BIND_ADDRESS:-"0.0.0.0"} \
                                                     -P=${WILDFLY_EXTRAS}/wildfly.properties
else
  echo "Unknown operating environment '${WILDFLY_OPERATING_MODE}'. Valid options are 'standalone' or 'domain'"
  exit 1
fi
