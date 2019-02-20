#!/bin/sh

if [ "$#" -ne 1 ]; then
    >&2 echo "Usage: $0 host:port"
    exit -1
fi

ARGUMENT=$1
HOST="$(echo $ARGUMENT | cut -d ':' -f1)"
PORT="$(echo $ARGUMENT | cut -d ':' -f2)"
MAX_RETRY=7
SECONDS_SLEEP=20

echo "Testing connection to host $HOST and port $PORT."

count=0
while [ $count -lt $MAX_RETRY ]
do
    count=$((count+1))
    nc -z $HOST $PORT
    result=$?
    if [ $result -eq 0 ]; then
        echo "Connection is available after $(($count * $SECONDS_SLEEP)) second(s)."
        cmd="$JAVA_CMD"

        # run the command
        exec $cmd
        exit 0
    fi
    echo "Retrying for $SECONDS_SLEEP seconds... $(date +'%d/%m/%Y %H:%M:%S:%3N')"
    sleep $SECONDS_SLEEP
done

>&2 echo "Timeout occurred after waiting $MAX_RETRY seconds for $HOST:$PORT."
exit 1
