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

## How to Run

Requirements: JDK 17 or newer and Maven 3.8 or newer.

```bash
# Compile
mvn compile

# Run the tests
mvn test

# Build the runnable JAR
mvn package

# Start the application
java -jar target/stockroom-1.0.jar

# Generate the Javadoc (output in target/site/apidocs)
mvn javadoc:javadoc
```
