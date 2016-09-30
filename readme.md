

# To start server with docker-compose

## Prepare

The config files for the docker backend are placed outside of the git directory and are a shared directory with docker. To setup this locally copy the `konfetti` folder from the git folder `docu` to your main hdd root directory. Then open your Docker `Preferences` and add under the `File Sharing` the entry `/konfetti`.

Default username and password is `dev` ... but please make sure to change this for production ;)

## Build and Start

(if mvnw is not working for you, install maven and replace mvnw with mvn)

Build package 
`./mvnw clean package`
 
Build docker image
`docker build --tag konfetti/api .`

Run service with docker-compose in console
`docker-compose up`

## Troubleshooting

maybe you need to stop & delete old containers before compose-up is working (`docker ps -a` and then `docker stop [ID]` then `docker rm [ID]`) ... if docker-compose up worked you sould see the logs from both containers (backend and DB)

To check if konfetti docker server is running call in your browser: http://localhost:9000/actuator/info

On the first time making a docker-compose up it could be that be startup fails and you dont get a response on localhost:9000 - then stop docker containers with CTRL+C and make again a `docker-compose up` - this time it should work.

## More info on managing docker

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

# Swagger Api Documentation
For dev profile the swagger api documentation is build, accessable by 

Json:
http://localhost:9000/v2/api-docs

UI:
http://localhost:9000/swagger-ui.html