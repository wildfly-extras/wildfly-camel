#!/bin/bash

readonly WILDFLY_MODULE_DIR=$1
readonly WILDFLY_CAMEL_MODULE_DIR=target/wildfly-patch/modules
readonly WILDFLY_MODULE_NAMES=wildfly-modules.txt

# Pre-req checks
[ ! -d ${WILDFLY_MODULE_DIR} ] && echo "${WILDFLY_MODULE_DIR} does not exist - aborting" && exit 1
[ ! -d ${WILDFLY_CAMEL_MODULE_DIR} ] && echo "${WILDFLY_CAMEL_MODULE_DIR} does not exist - aborting" && exit 1

# Dump out the WildFly module names to file
for MODULE in $(find ${WILDFLY_MODULE_DIR} -name module.xml)
do
  sed -n "s/.* name=\"\(.*\)\".*/\1/p" ${MODULE} | head -n1 >> target/${WILDFLY_MODULE_NAMES}
done

# Get module names added to WildFly camel and see if they're duplicates of existing modules
for MODULE in $(find ${WILDFLY_CAMEL_MODULE_DIR} -name module.xml)
do
  MODULE_NAME=$(sed -n "s/.* name=\"\(.*\)\".*/\1/p" $MODULE | head -n1)

  if grep "${MODULE_NAME}" target/${WILDFLY_MODULE_NAMES} > /dev/null
  then
    echo
    echo -e "\e[31mDEPENDENCY ERRORS DETECTED!!\e[0m"
    echo
    echo "Module ${MODULE_NAME} of WildFly Camel conflicts with an existing WildFly module."
    echo
    echo "Please fix your module dependencies."
    echo
    exit 1
  fi
done
