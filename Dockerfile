FROM openjdk:11
ENV SBT_VERSION 1.5.5
RUN curl -L -o sbt-$SBT_VERSION.zip https://github.com/sbt/sbt/releases/download/v1.5.5/sbt-$SBT_VERSION.zip
RUN unzip sbt-$SBT_VERSION.zip -d ops

WORKDIR /Site
COPY . /Site

EXPOSE 8001
CMD /ops/sbt/bin/sbt run

#docker build -t image_sharing_site_new .
#docker run --publish 8006:8001 --name image_sharing_site_new image_sharing_site_new
#source tutorial: https://yzhong-cs.medium.com/getting-started-with-docker-scala-sbt-d91f8ac22f5f
