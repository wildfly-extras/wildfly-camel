#!/bin/sh
#
# publish-docker-images.sh
#
# Pushes selected WildFly Camel Docker images to the Docker Hub
#
trap cleanup SIGHUP SIGINT SIGTERM

if ! $(which docker > /dev/null 2>&1)
then
  echo "Docker does not appear to be installed... Exiting"
  exit 1
fi

DOCKER_IMAGES=$(docker images | grep -E '^wildflyext/(wildfly|example|s2i-wildfly)-camel(-rest)?' | sort)
IMAGE_COUNT=0
OLDIFS=${IFS}
IFS=$'\n'

for IMAGE in ${DOCKER_IMAGES}
do
  IMAGE_COUNT=$((IMAGE_COUNT+1))
  IMAGE_NAME=$(echo ${IMAGE} | awk '{print $1}')
  IMAGE_TAG=$(echo ${IMAGE} | awk '{print $2}')
  IMAGE_ID=$(echo ${IMAGE} | awk '{print $3}')
  IMAGE_RAW=${IMAGE}

  declare "IDX${IMAGE_COUNT}_NAME=${IMAGE_NAME}"
  declare "IDX${IMAGE_COUNT}_TAG=${IMAGE_TAG}"
  declare "IDX${IMAGE_COUNT}_REPO=${IMAGE_NAME}:${IMAGE_TAG}"
  declare "IDX${IMAGE_COUNT}_ID=${IMAGE_ID}"
  declare "IDX${IMAGE_COUNT}_RAW=${IMAGE_RAW}"
done

function cleanup() {
  IFS=${OLDIFS}
  docker logout > /dev/null 2>&1
  exit $?
}

function hashGet() {
  local HASH=$1
  local KEY=$2
  local VALUE="${HASH}_${KEY}"

  printf '%s' "${!VALUE}"
}

function displayImages() {
  clear
  printf "   %-40s\t%-20s%-20s\n" "IMAGE" "TAG" "ID"

  for ((IMAGE_INDEX=1; IMAGE_INDEX <= IMAGE_COUNT ; IMAGE_INDEX++))
  do
    IMAGE_NAME=$(hashGet IDX${IMAGE_INDEX} 'NAME')
    IMAGE_TAG=$(hashGet IDX${IMAGE_INDEX} 'TAG')
    IMAGE_ID=$(hashGet IDX${IMAGE_INDEX} 'ID')
    printf "%d) %-40s\t%-20s%s\n" ${IMAGE_INDEX} "${IMAGE_NAME}" "${IMAGE_TAG}" "${IMAGE_ID}"
  done

  printf "%d) %s\n" $((IMAGE_COUNT+1)) "Exit this script"
}

function pushImages() {
  local IMAGE_SELECTION=$1
  local IMAGE_PUSH_SUCCESS=()
  local IMAGE_PUSH_ERRORS=()

  printf '\nEnter Docker Hub username and password...\n\n'
  docker login 2> /dev/null

  if [[ $? -ne 0 ]]
  then
    printf '\n\nDocker login failed. Cannot push images'
    sleep 5
    break
  else
    printf '\n\nPushing images...\n\n'

    for INDEX in ${SELECTION[@]}
    do
      local REPO=$(hashGet IDX${INDEX} 'REPO')

      if [[ ! -z ${REPO} ]]
      then
        docker push ${REPO}

        if [[ $? -ne 0 ]]
        then
          IMAGE_ERRORS+=(${REPO})
        else
          IMAGE_PUSH_SUCCESS+=(${REPO})
        fi
      fi
    done

    if [[ ${#IMAGE_PUSH_SUCCESS[@]} -gt 0 ]]
    then
      printf '\n\nThe following images were pushed successfully\n\n'
      for IMAGE in ${IMAGE_PUSH_SUCCESS[@]}
      do
        echo ${IMAGE}
      done
    fi

    if [[ ${#IMAGE_PUSH_ERRORS[@]} -gt 0 ]]
    then
      printf '\n\nThe following images were NOT pushed successfully\n\n'
      for IMAGE in ${IMAGE_PUSH_ERRORS[@]}
      do
          echo ${IMAGE}
      done
    fi

    printf '\n\nPress enter to continue: '
    read
  fi
}
function getImageSelection() {
  while [[ ! ${IMAGE_SELECTION} =~ ^[0-9]+(,[0-9]+)?+$ ]]
  do
    displayImages

    printf "\nChoose image(s) to push to Docker Hub\n\n"
    printf "Choose multiple images by separating choices with ','. E.g 2,4\n\n"
    echo -n "Enter image number(s): "

    read IMAGE_SELECTION
  done
}

function processImageSelection() {
  local SELECTION=($(echo $1 | tr ',' '\n'))
  local IMAGES=()

  if [[ ${SELECTION[0]} == $((IMAGE_COUNT + 1)) ]]
  then
    exit 0
  fi

  for INDEX in ${SELECTION[@]}
  do
    if [[ ${INDEX} =~ [1-9]$ ]] && [[ ${INDEX} -le ${IMAGE_COUNT} ]]
    then
      IMAGES+=($(hashGet IDX${INDEX} 'RAW'))
    fi
  done

  if [[ ${#IMAGES[@]} -gt 0 ]]
  then
    printf "You have chosen to push the following images:\n\n"

    for SELECTED_IMAGE in ${IMAGES[@]}
    do
      printf '%s\n' ${SELECTED_IMAGE}
    done

    while [[ true ]]
    do
      printf "\nIs this correct (y/n): "
      read IS_CORRECT

      case ${IS_CORRECT} in
        [yY])
          pushImages ${SELECTION}
          break
          ;;
        [nN])
          break
          ;;
      esac
    done

    unset IS_CORRECT
  fi
}

if [[ ! -z  "${DOCKER_IMAGES}" ]]
then
  while true
  do
    getImageSelection
    processImageSelection ${IMAGE_SELECTION}
    unset IMAGE_SELECTION
  done
else
  echo "No images tagged with the wildflyext namespace were found"
  exit 1
fi
