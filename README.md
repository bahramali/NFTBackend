# NFTBackend

This project is a Spring Boot application that connects to an MQTT broker and stores incoming messages in a PostgreSQL database. It also forwards the data to WebSocket subscribers.

## Building and running with Docker

1. Build the Docker image:

```bash
docker build -t nft-backend .
```

2. Run the container (adjust environment variables as needed for your database and MQTT configuration):

```bash
docker run -p 8080:8080 nft-backend
```

The resulting image can be deployed to any Docker-compatible environment such as AWS EC2.
