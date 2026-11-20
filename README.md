# Stockroom

A Java Swing inventory management system styled as a stockroom ledger: add, view, update and delete items, with stock status derived automatically and data saved to CSV.

## Team

| Name         | Reg. No. |
|--------------|----------|
| Arsal Saeed  | 506676   |
| Laiba Rehman | 505099   |

## Tech Stack

- Java 17
- Swing (no external UI libraries)
- Maven
- JUnit 5 (tests only)
- Javadoc
- Bundled fonts: Space Grotesk and IBM Plex Mono (SIL Open Font License 1.1)

## Requirements

| Requirement | Version | Notes |
|-------------|---------|-------|
| JDK         | 17 or newer | Must be a JDK, not just a JRE. `JAVA_HOME` must point to it. |
| Maven       | none needed | The bundled Maven Wrapper (`mvnw` / `mvnw.cmd`) downloads Maven 3.9.9 on first run. |
| Internet    | first build only | Maven downloads JUnit 5 and the build plugins into `~/.m2`. |

Everything else is in the repository: the dependencies are declared in `pom.xml`, and the fonts
(Space Grotesk and IBM Plex Mono, with their OFL licences) are bundled in `src/main/resources/fonts`.
The build checks the JDK version and stops with a clear message if it is older than 17.

Check your JDK:

```bash
java -version
```

## How to Run

After cloning, run these commands from the project folder.
On Windows use `mvnw` (Command Prompt / PowerShell: `.\mvnw`); on macOS, Linux or Git Bash use `./mvnw`.

```bash
# Compile
./mvnw compile

# Run the tests
./mvnw test

# Build the runnable JAR
./mvnw package

# Start the application
java -jar target/stockroom-1.0.jar

# Generate the Javadoc (output in target/site/apidocs)
./mvnw javadoc:javadoc
```

If Maven is already installed you can use `mvn` instead of `./mvnw`.
