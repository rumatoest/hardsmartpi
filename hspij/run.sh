#/usr/bin/env sh
cd "$(dirname "$0")"
./gradlew jar
echo "Execute application with args: $@"
java -jar ./build/libs/hspij.jar "$@"