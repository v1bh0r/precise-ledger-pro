# Precise Ledger Pro

[![Java CI with Maven](https://github.com/v1bh0r/precise-ledger-pro/actions/workflows/maven.yml/badge.svg)](https://github.com/v1bh0r/precise-ledger-pro/actions/workflows/maven.yml)

## Development

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: https://quarkus.io/ .

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw compile quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at http://localhost:8080/q/dev/.

#### Packaging and running the application

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

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

#### Creating a native executable

You can create a native executable using:

```shell script
./mvnw package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/precise-ledger-pro-1.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult https://quarkus.io/guides/maven-tooling.

## Demo

### Dependencies

Install the Intellij HTTP Cli tool by following the instructions
on https://www.jetbrains.com/help/idea/http-client-cli.html

### Running the demo

```shell script
ijhttp demo.http
```

#### Sample output

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      Running IntelliJ HTTP Client with                      │
├──────────────────────┬──────────────────────────────────────────────────────┤
│        Files         │ demo.http                                            │
├──────────────────────┼──────────────────────────────────────────────────────┤
│  Public Environment  │                                                      │
├──────────────────────┼──────────────────────────────────────────────────────┤
│ Private Environment  │                                                      │
└──────────────────────┴──────────────────────────────────────────────────────┘
Request '#1' POST http://localhost:8080/api/v1/loans
Request '#2' GET http://localhost:8080/api/v1/loans/e353deee-16c9-4477-ae28-91dd6a653775
Request '#3' POST http://localhost:8080/bulk/api/v1/loans/e353deee-16c9-4477-ae28-91dd6a653775/interest-rates
Request '#4' GET http://localhost:8080/api/v1/interest-rates?loanId=e353deee-16c9-4477-ae28-91dd6a653775
Request '#5' POST http://localhost:8080/bulk/api/v1/loans/e353deee-16c9-4477-ae28-91dd6a653775/ledger-activities
Request '#6' GET http://localhost:8080/api/v1/loans/e353deee-16c9-4477-ae28-91dd6a653775/ledger
Request '#7' POST http://localhost:8080/api/v1/loans/e353deee-16c9-4477-ae28-91dd6a653775/ledger-activities
 


7 requests completed, 0 have failed tests

```