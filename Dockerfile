FROM openjdk@sha256:0ba030330f545cf9697892423b1d8720be46e38f85045a483cd2b12126f10d12
#24-jdk-bullseye
WORKDIR /segmentation_app
COPY build/libs/segmentation_app-1.0.0.jar .
EXPOSE 8080
ENTRYPOINT ["java", "-Xms1024m", "-Xmx4g", "--enable-native-access=ALL-UNNAMED", "-jar", "segmentation_app-1.0.0.jar"]
