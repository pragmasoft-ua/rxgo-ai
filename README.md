# rxgo-ai

## About

RxGo AI project uses [OpenAI API](https://platform.openai.com/) to generate RxGo promotional texts.

This project uses Quarkus and PicoCli

## Configuration

Before using the app, you need to obtain OpenAI [API key](https://platform.openai.com/account/api-keys), then create **.env** file in the project root and write there your token as

```java
openai.apikey=<paste your token here>
```

You can also configure OpenAI API timeout (default is 3 minutes)

```java
openai.apikey=<paste your token here>
openai.timeout.seconds=100
```

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw compile quarkus:dev
```

You can change default prompt by passing it to the stdin

```shell script
./mvnw package
echo "Hi" |  java -jar .\target\quarkus-app\quarkus-run.jar
```

> **_NOTE:_** Quarkus now ships with a Dev UI, which is available in dev mode only at http://localhost:8080/q/dev/.

## Packaging and running the application

The application can be packaged using:

```shell script
./mvnw package
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./mvnw package -Dquarkus.package.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using

```shell script
java -jar target/rxgo-ai-2023.5.1-SNAPSHOT-runner.jar
```

## Creating a native executable

You can create a native executable using:

```shell script
./mvnw package -Pnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./mvnw package -Pnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/rxgo-ai-2023.5.1-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult https://quarkus.io/guides/maven-tooling.

## Related Guides

- Picocli ([guide](https://quarkus.io/guides/picocli)): Develop command line applications with Picocli

## Provided Code

### Picocli Example

Hello and goodbye are civilization fundamentals. Let's not forget it with this example picocli application by changing the <code>command</code> and <code>parameters</code>.

[Related guide section...](https://quarkus.io/guides/picocli#command-line-application-with-multiple-commands)

Also for picocli applications the dev mode is supported. When running dev mode, the picocli application is executed and on press of the Enter key, is restarted.

As picocli applications will often require arguments to be passed on the commandline, this is also possible in dev mode via:

```shell script
./mvnw compile quarkus:dev -Dquarkus.args='Quarky'
```

## TODO

native image build
https://quarkus.io/extensions/org.apache.camel.quarkus/camel-quarkus-support-retrofit
