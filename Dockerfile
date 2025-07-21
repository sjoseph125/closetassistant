FROM sbtscala/scala-sbt:eclipse-temurin-jammy-21.0.1_12_1.9.7_3.3.1

WORKDIR /app

COPY . .

RUN sbt clean compile stage

# Set AWS environment variables
ENV AWS_REGION=us-east-1
ENV AWS_ACCESS_KEY_ID=your-access-key-here
ENV AWS_SECRET_ACCESS_KEY=your-secret-access-key-here

EXPOSE 8080

CMD ["target/universal/stage/bin/closetassistant"]
