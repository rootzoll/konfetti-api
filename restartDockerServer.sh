./mvnw clean package -DskipTests && docker build --tag konfetti/api . && docker-compose up
