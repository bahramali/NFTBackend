# NFTBackend

This project is a simple Spring Boot application that listens to Kafka topics and stores the received data in a PostgreSQL database.

## Building and running with Docker

1. Build the Docker image:

```bash
docker build -t nft-backend .
```

2. Run the container (adjust environment variables as needed for your database and Kafka configuration):

```bash
docker run -p 8080:8080 nft-backend
```

The resulting image can be deployed to any Docker-compatible environment such as AWS EC2.
