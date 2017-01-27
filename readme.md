
![alt tag](https://api.travis-ci.org/rootzoll/konfetti-api.svg?branch=master)

# To start server with docker-compose

## Prepare

The config files for the docker backend are placed outside of the git directory and are a shared directory with docker. To setup this locally copy the `konfetti` folder from the git folder `docu` to your main hdd root directory. Then open your Docker `Preferences` and add under the `File Sharing` the entry `/konfetti`.

Default username and password is `dev` ... but please make sure to change this for production ;)

## Build and Start

Install docker: https://www.digitalocean.com/community/tutorials/how-to-install-and-use-docker-on-ubuntu-16-04

Install docker-compose: https://www.digitalocean.com/community/tutorials/how-to-install-docker-compose-on-ubuntu-16-04

Install letsencrypt using `apt-get i letsencrypt`

You will need the konfetti-api repository for now, to bring up the services.

Run service with docker-compose in console
`docker-compose up`

## Troubleshooting

maybe you need to stop & delete old containers before compose-up is working (`docker ps -a` and then `docker stop [ID]` then `docker rm [ID]`) ... if docker-compose up worked you sould see the logs from both containers (backend and DB)

To check if konfetti docker server is running call in your browser: http://localhost:8280/actuator/info

On the first time making a docker-compose up it could be that be startup fails and you dont get a response on localhost:8280 - then stop docker containers with CTRL+C and make again a `docker-compose up` - this time it should work.

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

**if using locally and graylog is not running, remove the lines in logback-spring.xml loading the gelfAppender!!**

# To start server locally without docker (for experts)

Change into main directory and run command
"./mvnw spring-boot:run"

- service should be running on port 8280

To stop the server:
- just press CTRL-C in console, or kill the process

To run with a different profile, start with environment variable spring.profiles.active, e.g. to start dev profile and skip Tests:
./mvnw spring-boot:run -Dspring.profiles.active=dev -DskipTets

existing Profiles at the moment:
dev
    -> using mysql for persistence, adjust values for your mysql server accordingly in file application-dev.properties (spring.datasource.user and spring.datasource.password)
test
    -> using H2 inMemory Database

If you need to change parameters, modify application-dev.properties, but make sure you dont commit the changes, except they are general adjustments, not just
for your local environment

to test if the server is running correctly, call the URL http://localhost:8280/konfetti/api/account

should return something like this "{"clientId":"1","secret":"3915478b-f51d-4306-ab3b-fa7762f4c6bc","userId":"1"}"

# Application routes
NGINX is configured to bundle the various administration interfaces to be accessible via https, when deployed, the services are not reachable individually. Instead use these routes to access them ([:port/outside_route]->[docker-internal_address]):

`:80,:443/ -> http://konfettiHomepage:80/`

`:80,:443/app/ -> http://konfettiApp:80/`

`:80,:443/admin/ -> http://konfettiAdmin:80/`

`:80,:443/coupongenerator/ -> http://konfettiCouponGenerator:2342/`

`:80,:443/bootadmin/ -> http://konfettiBootAdmin:8180/`

`:80,:443,:8280/konfetti/api/ -> http://konfettiApi:8280/konfetti/api/`

# SSL
ssl is done via letsencrypt.org and certbot (ubuntu package https://certbot.eff.org/#ubuntuxenial-nginx).
Inital certificate aquisition: Adapt and run the following command on the host system (everything is mounted into docker - the ngninx container will not start, if defined mounts or cert-files are not available. Uncomment if necessary)
`$ letsencrypt certonly --webroot -w {pathToProjectFolder}/nginx/letsencrypt -d konfettiapp.de -d www.konfettiapp.de -d test.konfettiapp.de`
This should generate the certificates.
Afterwards run `letsencrypt renew --dry-run --agree-tos` to prepare automatic renewal.
Then place this into root's cron (renews the certificates every 2 month and makes nginx reload the files):
`* * * */2 * letsencrypt renew && pkill -HUP nginx`

# Swagger Api Documentation
For dev profile the swagger api documentation is build, accessable by

Json:
http://localhost:8280/v2/api-docs

UI:
http://localhost:8280/swagger-ui.html

# Eclipse IDE Setup with Lombok

Open Eclipse (tested with NEON). "File > New > Other" .. then select "Maven > Maven Project". Choose the path were you did the check-out from GIT.

If you see Errors about missing log and getter/setters: The Java code uses the Lombok Lib (see pom.xml for version) https://projectlombok.org/download.html - you need to download the lombok.jar, close eclipse and start the JAR (double click) or "java -jar lombok.jar" - make sure to set the correct eclipse path with lombok install dialog.

#this is a test
