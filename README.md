# hardsmartpi
Умные железяки на базе RaspberryPy (код проекта)

# REST API test queries via httpie

## Current state

http http://hardsmartpi:4242/state

while true; do http http://hardsmartpi:4242/state; sleep 2; done

## Humidity level

http http://hardsmartpi:4242/humidity/low

http http://hardsmartpi:4242/humidity/high

http POST http://hardsmartpi:4242/humidity/low level=40

http POST http://hardsmartpi:4242/humidity/high level==45


## Votes check
http http://hardsmartpi:4242/votes/h

http http://hardsmartpi:4242/votes/f
