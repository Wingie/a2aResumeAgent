# this is an expensive command
docker-compose down && docker-compose build --no-cache a2awebagent 

# this is a cheap command to reload the code quickly
docker-compose build up -d 

# testing for example browser working
docker exec a2awebagent mvn test -Dtest=BrowserSystemHealthTest
