#!/usr/bin/env sh

cd "$(dirname "$0")"

RPI_HOST="hardsmartpi"
if [ ! -z "$1" ]; then
    RPI_HOST="$1"
fi

./gradlew jar
echo "Copy build to $RPI_HOST via scp"
sshpass -v -p raspberry  scp ./build/libs/hspik.jar  "pi@${RPI_HOST}:~/hspi.jar"