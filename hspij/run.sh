#/usr/bin/env sh
cd "$(dirname "$0")"
./gradlew jar
echo "Execute application in DEV mode with args: $@"
java -jar ./build/libs/hspij.jar --config=./dev.yml "$@"