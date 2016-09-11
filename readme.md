# To start server with docker-compose

(if mvnw is not working for you, install maven and replace mvnw with mvn)

Build package 
./mvnw clean package
 
Build docker image
docker build --tag konfetti/backend .

Run service with docker-compose in console
docker-compose up

Run service with docker-compose as a daemon
docker-compose up -d

Show running containers
docker ps

Show all containers (also stopped ones)
docker ps -a

Show logs from conatiner
docker logs (-f) {containerName | containerId}

Log into container 
docker exec -it {containerName | containerId} bash

Log out from container
exit

# To start server locally without docker (for experts)

Change into main directory and run command
"./mvnw spring-boot:run"

- service should be running on port 9000

To stop the server:
- just press CTRL-C in console, or kill the process

To change the Profile, change the value "spring.profiles.active" in file "api/src/main/resources/application.properties"

existing Profiles at the moment:
dev
    -> using mysql for persistenc, adjust values for your mysql server accordingly in file application-dev.properties (spring.datasource.user and spring.datasource.password)
test
    -> using H2 inMemory Database

to test if the server is running correctly, call the URL http://localhost:9000/konfetti/api/account

should return something like this "{"clientId":"1","secret":"3915478b-f51d-4306-ab3b-fa7762f4c6bc","userId":"1"}"
