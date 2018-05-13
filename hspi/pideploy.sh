#/usr/bin/env sh
cd "$(dirname "$0")"

RPI_IP="192.168.11.11"
if [ ! -z "$1" ]; then
    RPI_IP="$1"
fi

./gradlew jar
echo "Copy build to $RPI_IP via scp"
sshpass -v -p raspberry  scp ./build/libs/hspik.jar  "pi@${RPI_IP}:~/hspi.jar"