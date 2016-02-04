FROM java:7

RUN echo 'hosts: files mdns4_minimal [NOTFOUND=return] dns mdns4' >> /etc/nsswitch.conf

COPY ./code/ /usr/p2pd/
COPY ./lib/ /usr/p2pd/lib/

RUN mkdir /usr/p2pd/bin

COPY ./docker/dockerstart ./usr/p2pd/bin/start

WORKDIR /usr/p2pd

ENV classpath=/usr/p2pd/lib/commons-cli-1.3.1.jar:/usr/p2pd/lib/commons-fileupload-1.2.2.jar:/usr/p2pd/lib/commons-io-2.4.jar:/usr/p2pd/lib/commons-lang3-3.1.jar:/usr/p2pd/lib/dns_sd.jar:/usr/p2pd/lib/jackson-annotations-2.0.2.jar:/usr/p2pd/lib/jackson-core-2.0.2.jar:/usr/p2pd/lib/jackson-databind-2.0.2.jar:/usr/p2pd/lib/junit-3.8.1.jar:/usr/p2pd/lib/commons-codec-1.10.jar:/usr/p2pd/lib/jmdns.jar

RUN find -name "*.java" > sources.txt && javac -d /usr/p2pd/bin -cp $classpath @sources.txt

RUN cp /usr/p2pd/res/* /usr/p2pd/bin

RUN chmod u+x /usr/p2pd/bin/start

EXPOSE 8765

ENTRYPOINT ["/usr/p2pd/bin/start"]

CMD ["--debug"]
