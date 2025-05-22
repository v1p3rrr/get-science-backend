# Stage 1: Сборка приложения с использованием Gradle
FROM gradle:jdk17-alpine AS build
WORKDIR /home/gradle/project

# Копируем файлы Gradle и настройки проекта
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle

# Копируем исходный код
# Это копируется после файлов сборки для лучшего использования кэша Docker
# Если изменятся только исходники, Docker сможет переиспользовать слой с загруженными зависимостями (если бы мы их загружали отдельно)
COPY src ./src

# Предоставляем права на выполнение для gradlew
RUN chmod +x ./gradlew

# Собираем приложение, пропуская тесты. --no-daemon рекомендуется для CI/Docker.
RUN ./gradlew build -x test --no-daemon --refresh-dependencies

# Stage 2: Создание финального образа на основе собранного JAR
FROM openjdk:17-alpine
WORKDIR /app

# Указываем путь к собранному JAR файлу из предыдущего этапа
# Ищем JAR, который, скорее всего, является SNAPSHOT версией (например, project-name-0.0.1-SNAPSHOT.jar)
ARG JAR_NAME=*-SNAPSHOT.jar
COPY --from=build /home/gradle/project/build/libs/get-science-backend-0.0.1-SNAPSHOT.jar app.jar


# Добавляем healthcheck
HEALTHCHECK --interval=30s --timeout=3s \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
