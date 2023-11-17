# Airlock SDK for Java

The **Airlock SDK for Java** is an API client for [Airlock](https://www.philterd.ai/airlock). Airlock is an AI policy layer to prevent the disclosure of sensitive information, such as PII and PHI, in your AI applications.

Refer to the [Airlock API](https://docs.philterd.ai/airlock/latest/api.html) documentation for details on the methods available.

## Example Usage

To apply a policy to text:

```
AirlockClient client = new AirlockClient.AirlockClientBuilder().withEndpoint("https://127.0.0.1:8080").build();
ApplyResponse applyResponse = client.apply(text);
```

## Dependency

Release dependencies are available in Maven Central.

```
<dependency>
  <groupId>ai.philterd</groupId>
  <artifactId>airlock-sdk-java</artifactId>
  <version>1.0.0</version>
</dependency>
```

Snapshot dependencies are available in the Maven Central Snapshot Repository by adding the repository to your `pom.xml`:

```
<repository>
  <id>snapshots</id>
  <url>https://s01.oss.sonatype.org/content/repositories/snapshots</url>
  <releases><enabled>false</enabled></releases>
  <snapshots><enabled>true</enabled></snapshots>
</repository>
```

## Release History

* 1.0.0:
  * Initial release.

## License

This project is licensed under the Apache License, version 2.0.

Copyright 2023 Philterd, LLC.
Philter is a registered trademark of Philterd, LLC.
