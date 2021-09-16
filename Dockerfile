FROM java:8-jdk-alpine
COPY ./out/artifacts/DiscordCleverBot_jar/DiscordCleverBot.jar /usr/app/
COPY jslibs.js /usr/app/

WORKDIR /usr/app
ENTRYPOINT ["java", "-jar", "DiscordCleverBot.jar"]