# Étape 1 : compilation — utilise une image avec le JDK complet (nécessaire pour compiler),
# jetée après le build (elle n'existe pas dans l'image finale).
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copié séparément du reste du code : tant que pom.xml ne change pas, Docker réutilise le cache
# de cette étape (les dépendances ne sont pas re-téléchargées à chaque modification de code Java).
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
# mvnw perd son bit executable quand Git le suit depuis Windows -- le restaurer explicitement.
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# Étape 2 : exécution — image finale beaucoup plus légère, avec seulement le JRE (pas besoin du
# JDK pour faire tourner un jar déjà compilé) et le jar produit par l'étape précédente.
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# N'exécute jamais le process applicatif en root à l'intérieur du conteneur : limite ce qu'un
# attaquant pourrait faire s'il obtenait une exécution de code via une autre faille.
RUN addgroup --system maplenou && adduser --system --ingroup maplenou maplenou
USER maplenou

EXPOSE 8080
# MaxRAMPercentage borne le tas a 70% de la memoire detectee du conteneur (cgroup) plutot que
# de laisser le JVM deviner -- sans ca, un pic de charge peut faire grossir le tas jusqu'a
# saturer les 512 Mo du plan Render "starter" et declencher un OOM-kill silencieux (le
# conteneur redemarre, les quelques requetes en vol pendant le redemarrage echouent avec une
# erreur 502/504 -- symptome "ca marche apres un rafraichissement" remonte en test).
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70.0", "-XX:InitialRAMPercentage=50.0", "-jar", "app.jar"]
