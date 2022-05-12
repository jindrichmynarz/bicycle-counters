FROM clojure:openjdk-11-lein-slim-buster
MAINTAINER Jindřich Mynarz <mynarzjindrich@gmail.com>

WORKDIR /
COPY . ./
RUN lein uberjar

CMD java -jar target/bicycle_counters.jar
