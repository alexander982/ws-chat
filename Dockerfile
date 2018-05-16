FROM java:8-alpine
MAINTAINER Your Name <you@example.com>

ADD target/uberjar/cljs-tst.jar /cljs-tst/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/cljs-tst/app.jar"]
