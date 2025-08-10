FROM sbtscala/scala-sbt:eclipse-temurin-focal-17.0.5_8_1.9.4_3.3.0

# Set working directory
WORKDIR /app

# Copy project files
COPY . /app

# Set up permissions (optional, for non-root usage)
# RUN chown -R root:root /app

# Default command: open sbt shell
CMD ["sbt", "~reStart"]