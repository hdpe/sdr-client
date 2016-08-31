# Spring Data REST Model Generator #

A client-side data model generator for a JPA Spring Data REST-provided API.

The `hal-client` generic client framework can then leverage this generated model to provide a simple, link-traversing client for the source API. This is primarily intended to provide a convenient means for the setup, query and tear-down of data in automated acceptance tests.

## Building ##

Download and install [hdr-client](https://github.com/hdpe/hal-client) first.

Then, to build and install into the local Maven repository:

`mvn install`


## Quickstart ##

### Creating an application ###

Create a new folder `greeting/` for your project.

Create a new Maven `pom.xml` with the following content:

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">

	<modelVersion>4.0.0</modelVersion>

	<parent>
		<groupId>org.springframework.boot</groupId>
		<artifactId>spring-boot-starter-parent</artifactId>
		<version>1.4.0.RELEASE</version>
	</parent>

	<groupId>greeting</groupId>
	<artifactId>app</artifactId>
	<version>0.0.1-SNAPSHOT</version>

	<properties>
		<sdr-model-gen.version>0.0.1-SNAPSHOT</sdr-model-gen.version>
	</properties>

	<dependencies>
		<dependency>
			<groupId>uk.co.blackpepper.sdr.model.gen</groupId>
			<artifactId>sdr-model-gen</artifactId>
			<version>${sdr-model-gen.version}</version>
		</dependency>

		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-web</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-data-jpa</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-data-rest</artifactId>
		</dependency>

		<dependency>
			<groupId>com.h2database</groupId>
			<artifactId>h2</artifactId>
		</dependency>
	</dependencies>

</project>
```

Add the following source files:

`src/main/java/greeting/App.java`:

```java
@SpringBootApplication
public class App extends RepositoryRestConfigurerAdapter {

	public static void main(String[] args) {
		SpringApplication.run(App.class, args);
	}

	@Override
	public void configureJacksonObjectMapper(ObjectMapper objectMapper) {
		objectMapper.setVisibility(PropertyAccessor.ALL, Visibility.NONE)
			.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
	}
}
```

`src/main/java/greeting/model/Greeting.java`:

```java
@Entity
@RestRepository("/greetings")
public class Greeting {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	private String message;
}
```

`src/main/java/greeting/repository/GreetingRepository.java`:

```java
public interface GreetingRepository extends CrudRepository<Greeting, Integer> {
}
```

and run the `App` class to verify your application starts successfully.

`2016-08-08 14:40:09.479  INFO 24824 --- [           main] greeting.App                             : Started App in 3.737 seconds (JVM running for 3.95)`

### Generating the client data model ###

Add the following to your `pom.xml`:

```xml
<build>
  <plugins>
    <plugin>
      <groupId>uk.co.blackpepper.sdr.model.gen</groupId>
      <artifactId>sdr-model-gen-maven-plugin</artifactId>
      <version>${sdr-model-gen.version}</version>
      <executions>
        <execution>
          <goals>
            <goal>generate</goal>
          </goals>
          <configuration>
            <packageName>greeting.model</packageName>
          </configuration>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```

Rebuild your application and you should see the following file has been generated in `target/generated-sources/sdr-model/greeting/model/client`:

```java
package greeting.model.client;

import uk.co.blackpepper.hal.client.annotation.RemoteResource;
import java.net.URI;
import com.fasterxml.jackson.annotation.JsonIgnore;
import uk.co.blackpepper.hal.client.annotation.ResourceId;

@RemoteResource("/greetings")
public class Greeting {

	private URI id;
	private String message;

	@JsonIgnore
	@ResourceId
	public URI getId() {
		return id;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
```

### Invoke the client ###

Add JUnit to your pom's dependencies section:

```xml
<dependency>
  <groupId>junit</groupId>
  <artifactId>junit</artifactId>
  <scope>test</scope>
</dependency>
```

And add the following test at `src/test/java/greeting/GreetingTest.java`:

```java
public class GreetingTest {

	@Test
	public void testClient() {
		ClientFactory factory = new ClientFactory(URI.create("http://localhost:8080"));
		Client<Greeting> client = factory.create(Greeting.class);

		Greeting entity = new Greeting();
		entity.setMessage("hello world!");

		URI location = client.post(entity);

		assertThat(client.get(location).getMessage(), is("hello world!"));
	}
}
```

ensuring you import the *generated* `Greeting` class from `greeting.model.client`.

Check the server application is still running, and run the test to verify it passes!

## But, in real life... ##

Obviously the above example is pretty contrived, as the same result could be achieved just by using the original `@Entity` class and Spring's `RestTemplate`.

### Associations ###

 The power comes when associations between entities are introduced - when navigating entity associations via the client data model accessors, HTTP requests will be issued to lazily retrieve the associated objects, transparently navigating the HATEOAS links in the returned HAL JSON.

### Data model artifact ###

The other primary motivation for this tool is so that a data model can be generated for use by a client application that does *not* have a dependency on the server application code. For this, it is suggested that you generate the data model into a separate artifact, which imports the server project as an *optional* dependency so it is not resolved by the client application transitively.

## Usage ##

### Entity associations ###

#### Class level ####

* `@RestRepository(path)` - the path to the remote repository: *Required* for entities with repositories

#### Field level ####

Associations are assumed to be *linked* (i.e. to an entity that has its own repository) unless marked as *embedded* as follows:

* `@EmbeddedResource` - mark a single-valued association to be an embedded association, that is, to an entity that does not have a repository
* `@EmbeddedResources` - mark a collection-valued assocation to be an embedded assocation
* `@RestIgnore` - don't generate a property in the client data model for this field

### Client API ###

The client supports `get`, `getAll`, `post` and `delete` operations - it is not intended to support `put`/`patch` at this time.

## Limitations ##

ID fields (PKs) must be generated in the database (annotated with `@GeneratedValue`).

The tool assumes that it is the *fields* of the server-side entities that should be the basis of the generated artifacts, not the properties, so Spring Data REST must have its `ObjectMapper` configured to use field- rather than property-level access. This is an intentional design decision - the fields comprise the state of the entity, and accessors/mutators may not be present or not directly pass through to the underlying fields.

## Roadmap ##

There are plenty of things still to do with this:

* back-references to embedded associations' contexts should be supported
* support generation from more than one package
* test coverage needs to be hardened up significantly
* remove @RestRepository and derive the repository path from the Spring Data REST repository model
* improve flaky m2e integration
* ...
