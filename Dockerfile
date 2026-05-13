FROM ubuntu:22.04
LABEL maintainer="b.gamard@sismics.com"

# Run Debian in non interactive mode
ENV DEBIAN_FRONTEND noninteractive

# Configure env
ENV LANG C.UTF-8
ENV LC_ALL C.UTF-8
ENV JAVA_HOME /usr/lib/jvm/java-11-openjdk-amd64/
ENV JAVA_OPTIONS -Dfile.encoding=UTF-8 -Xmx1g
ENV JETTY_VERSION 11.0.20
ENV JETTY_HOME /opt/jetty

# Install packages
RUN sed -i 's/ main restricted/ main restricted universe multiverse/g' /etc/apt/sources.list && \
    apt-get -o Acquire::Retries=3 -o Acquire::http::Timeout=30 update && \
    apt-get -o Acquire::Retries=3 -o Acquire::http::Timeout=30 -y -q --no-install-recommends install \
    vim less procps unzip wget tzdata openjdk-11-jdk \
    ffmpeg \
    mediainfo \
    tesseract-ocr \
    tesseract-ocr-chi-sim \
    tesseract-ocr-deu \
    tesseract-ocr-fra \
    tesseract-ocr-eng \
    tesseract-ocr-spa \
    && apt-get clean && \
    rm -rf /var/lib/apt/lists/*
RUN dpkg-reconfigure -f noninteractive tzdata

# Install Jetty
RUN wget -nv -O /tmp/jetty.tar.gz \
    "https://repo1.maven.org/maven2/org/eclipse/jetty/jetty-home/${JETTY_VERSION}/jetty-home-${JETTY_VERSION}.tar.gz" \
    && tar xzf /tmp/jetty.tar.gz -C /opt \
    && mv /opt/jetty* /opt/jetty \
    && useradd jetty -U -s /bin/false \
    && chown -R jetty:jetty /opt/jetty \
    && mkdir /opt/jetty/webapps \
    && chmod +x /opt/jetty/bin/jetty.sh

EXPOSE 8080

# Install app
RUN mkdir /app && \
    cd /app && \
    java -jar /opt/jetty/start.jar --add-modules=server,http,webapp,deploy

ADD docs.xml /app/webapps/docs.xml
ADD docs-web/target/docs-web-*.war /app/webapps/docs.war

WORKDIR /app

CMD ["java", "-jar", "/opt/jetty/start.jar"]
