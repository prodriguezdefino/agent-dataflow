kill $(lsof -t -i:8080) > /dev/null 2>&1 || :
kill $(lsof -t -i:8081) > /dev/null 2>&1 || :
