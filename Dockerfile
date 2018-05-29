FROM gradle:4.7-jdk8 AS buildimg

# Copying source code
COPY ./code/ /home/gradle/code/

# Copying few libraries that aren't pulled in from Java repository
COPY ./lib/ /home/gradle/lib/

# Copying gradle build instructions
COPY ./*.gradle /home/gradle/

RUN mkdir -p /home/gradle/bin/build

WORKDIR /home/gradle

RUN gradle -x test build

FROM java:8

RUN echo 'hosts: files mdns4_minimal [NOTFOUND=return] dns mdns4' >> /etc/nsswitch.conf

RUN mkdir -p /usr/p2pd/bin

COPY --from=buildimg /home/gradle/bin/build/lib/* /usr/p2pd/lib/
COPY --from=buildimg /home/gradle/dist/p2p-dictionary-SNAPSHOT.jar /usr/p2pd/

EXPOSE 8765

WORKDIR /usr/p2pd/

ENTRYPOINT ["java", "-cp", "/usr/p2pd/p2p-dictionary-SNAPSHOT.jar:/usr/p2pd/lib/*:.", "-Djava.net.preferIPv4Stack=true", "com.rhfung.p2pd.Server"]

CMD ["--debug"]
