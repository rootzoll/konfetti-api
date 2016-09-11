mvn clean package -DskipTests && docker build --tag konfetti/backend . && docker-compose up
