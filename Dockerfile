FROM java:8

RUN echo 'hosts: files mdns4_minimal [NOTFOUND=return] dns mdns4' >> /etc/nsswitch.conf

COPY ./code/ /usr/p2pd/
COPY ./lib/ /usr/p2pd/lib/

RUN mkdir /usr/p2pd/bin

WORKDIR /usr/p2pd

ENV classpath=/usr/p2pd/lib/commons-cli-1.3.1.jar:/usr/p2pd/lib/commons-fileupload-1.2.2.jar:/usr/p2pd/lib/commons-io-2.4.jar:/usr/p2pd/lib/commons-lang3-3.1.jar:/usr/p2pd/lib/dns_sd.jar:/usr/p2pd/lib/jackson-annotations-2.0.2.jar:/usr/p2pd/lib/jackson-core-2.0.2.jar:/usr/p2pd/lib/jackson-databind-2.0.2.jar:/usr/p2pd/lib/junit-3.8.1.jar:/usr/p2pd/lib/commons-codec-1.10.jar:/usr/p2pd/lib/jmdns.jar

RUN find -name "*.java" > sources.txt && javac -d /usr/p2pd/bin -cp $classpath @sources.txt

RUN cp /usr/p2pd/res/* /usr/p2pd/bin

EXPOSE 8765

WORKDIR /usr/p2pd/bin

ENTRYPOINT ["java", "-cp", "/usr/p2pd/lib/*:/usr/p2pd/bin", "-Djava.net.preferIPv4Stack=true", "com.rhfung.p2pd.Server"]

CMD ["--debug"]
