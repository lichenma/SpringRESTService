# Building REST services with Spring 


Starting from the very beginning, we will be following a basic tutorial and covering some simple base
concepts regarding the spring framework. REST has quickly become the de-facto standard for building web
services because they are so easy to build and consume. There is another discussion about how rest fits
into the world of microservices architecture but for now we will focus on simply building the RESTful 
service. 

REST is very beneficial because it embraces the precepts of the web including the architecture benefits
and everything else. Its author Roy Fielding was probably involved in a dozen specs which govern how
the web operates. 

The web and its core protocol, HTTP, provide a stack of features:

* Suitable actions (GET, POST, PUT, DELETE) 
* Caching 
* Redirection and forwarding 
* Security (encryption and authentication) 

These are all critical factors on building resilient services but building on top of HTTP, REST APIs 
provide the means to build flexible APIs that can: 

* Support backward compatibility
* Evolvable APIs 
* Scaleable services 
* Securable services 
* A spectrum of stateless to stateful services 

REST is not a standard per se but an approach with a set of constraints on your architecture that can 
help you build web-scale systems. The project will use the Spring portfolio to build a RESTful service
while leveraging the stackless features of REST. 


This tutorial will utilize Spring Boot which can be built using the Spring Initializr with the 
following dependencies: 

* Web 
* JPA
* H2
* Lombok 


## Remote Procedural Call

Starting from the simplest thing we can construct, we are going to leave off the concepts of REST and
then add them in later to understand the difference. 

This example models a simple payroll service that manages the employees of a company. Simply put we
will be storing employee objects in an H2/Postgres in-memory database, and access them via JPA. This
will be wrapped with a Spring MVC layer to access remotely. 

```java
package payroll;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Data
@Entity
class Employee {

	private @Id @GeneratedValue Long id;
	private String name;
	private String role;

	Employee(String name, String role) {
		this.name = name;
		this.role = role;
	}

	// this was not located in the tutorial but for some reason you need a default constructor 
	   which has no arguments 

	Employee(){
	// default no args constructor
	}

}
```


Important things to note about this java class: 

* @Data is a Lombok annotation to create all the getters, setters, equals, hash, and toString 
  methods based on the fields 

*  @Entity is a JPA annotation to make this object ready for storage in a JPA-based data store

* id, name, and role are the attribute for our domain object, the first being marked with more
  JPA annotations to indicate it's the primary key and automatically populated by the JPA provider

* a custom constructor is created when we need to create a new instance but don't have an id


With this domain object definition, we can turn to Spring Data JPA to handle the tedious database 
interactions. Spring Data repositories are interfaces with methods supporting reading, updating, 
deleting and creating records against a back end data store. Some repositories also support data paging
and sorting when appropriate. Spring Data synthesizes implementations based on conventions found in
the name of the methods in the interface. 


There are multiple repository implementations besides JPA. You can use Spring Data MongoDB, Spring Data
GemFire, Spring Data Cassandra, etc. For this tutorial we will stick with JPA 




```java
package payroll;

import org.springframework.data.jpa.repository.JpaRepository;

interface EmployeeRepository extends JpaRepository<Employee, Long> {

}
```




The interface extends Spring Data JPA's JpaRespository , specifying the domain type as Employee and the
id type as Long. This interface, though empty on the surface, is full of functionality given that it 
supports: 

* Creating new instances
* Updating existing ones
* Deleting 
* Finding (one, all, by simple or complex properties) 



Spring Data's repository solution makes it possible to sidestep data store specifics and instead solve
a majority of problems using domain-specific terminology. 




At this point, this is enough a launch an application- Spring Boot app is at minimum a public static 
void main entry-point and the @SpringBootApplication annotation. This tells Spring Boot to help out
whenever possible. 


```java
package payroll;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PayrollApplication {

	public static void main(String... args) {
		SpringApplication.run(PayrollApplication.class, args);
	}
}
```

@SpringBootApplication is a meta-annotation that pulls in component scanning, autoconfiguration and 
property support. In essence it starts up a servlet container and serves up our service. 



## Inserting Data


An application with no data is not very interesting so lets load it. The following class will get 
loaded automatically by Spring: 


```java
package payroll;

import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
class LoadDatabase {
    @Bean
    CommandLineRunner initDatabase(EmployeeRepository repository){
        return args -> {
            log.info("Preloading "+repository.save(new Employee("Tony Stark", "IT Technician")));
            log.info("Preloading "+repository.save(new Employee("Bruce Wayne", "Business Analyst")));

        };
    }
}
```

When it gets loaded: 

* Spring Boot will run ALL CommandLineRunner beans once the application context is loaded
* This runner will request a copy of the EmployeeRepository you just created
* Using it, this class will create two entities and store them 
* @Slf4j is a Lombok annotation to autocreate an Slf4j-based LoggerFactory as log, allowing
  us to log these newly created "employees" 




Now when running PayRollApplication we can see this: 

```
...
2019-02-05 14:22:10.150  INFO 43680 --- [           main] com.example.RESTService.LoadDatabase     
: Preloading Employee(id=1, name=Tony Stark, role=IT Technician)

2019-02-05 14:22:10.153  INFO 43680 --- [           main] com.example.RESTService.LoadDatabase     
: Preloading Employee(id=2, name=Bruce Wayne, role=Business Analyst)
...
```



## HTTP Platform


To wrap the repository in a web layer we have to utilize the Spring MVC. Thanks to Spring Boot, there
is little in infrastructure to code. Instead, we can focus on actions: 



```java
package payroll;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
class EmployeeController {

	private final EmployeeRepository repository;

	EmployeeController(EmployeeRepository repository) {
		this.repository = repository;
	}

	// Aggregate root

	@GetMapping("/employees")
	List<Employee> all() {
		return repository.findAll();
	}

	@PostMapping("/employees")
	Employee newEmployee(@RequestBody Employee newEmployee) {
		return repository.save(newEmployee);
	}

	// Single item

	@GetMapping("/employees/{id}")
	Employee one(@PathVariable Long id) {

		return repository.findById(id)
			.orElseThrow(() -> new EmployeeNotFoundException(id));
	}

	@PutMapping("/employees/{id}")
	Employee replaceEmployee(@RequestBody Employee newEmployee, @PathVariable Long id) {

		return repository.findById(id)
			.map(employee -> {
				employee.setName(newEmployee.getName());
				employee.setRole(newEmployee.getRole());
				return repository.save(employee);
			})
			.orElseGet(() -> {
				newEmployee.setId(id);
				return repository.save(newEmployee);
			});
	}

	@DeleteMapping("/employees/{id}")
	void deleteEmployee(@PathVariable Long id) {
		repository.deleteById(id);
	}
}
```



Important things to note about this java class: 
* @RestController indicates that the data returned by each method will be written straight into
  the response body instead of rendering a template

* An EmployeeRepository is injected by constructor into the controller

* We have routes for each operations (@GetMapping, @PostMapping, @PutMapping, and 
  @DeleteMapping, corresponding to the HTTP GET, POST, PUT and DELETE calls 

* EmployeeNotFoundException is an exception used to indicate when an employee is looked up but
  not found 



```java
package payroll;

class EmployeeNotFoundException extends RuntimeException {

	EmployeeNotFoundException(Long id) {
		super("Could not find employee " + id);
	}
}
```


When an EmployeeNotFoundException is thrown this extra bit of Spring MVC Configuration is used to 
render an HTTP 404:



```java
EmployeeNotFoundAdvice.java


package payroll;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
class EmployeeNotFoundAdvice {

	@ResponseBody
	@ExceptionHandler(EmployeeNotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	String employeeNotFoundHandler(EmployeeNotFoundException ex) {
		return ex.getMessage();
	}
}
```



Important things to note about this class: 
* @ResponseBody signals that this advice is rendered straight into the response body
* @ExceptionHandler configures the advice to only respond if an EmployeeNotFoundException is
  thrown 

* @ResponseStatus says to issue an HttpStatus.NOT\_FOUND ie. HTTP 404
* the body of the advice generates the content. In this case it gives the message of the 
  exception



Right now we can test the application by running 

```
mvn clean spring-boot:run 
```


When the app starts, we can see the data that we loaded into the database, access a non existing 
Employee record (returns 404 error), create a new Employee record, alter the user, or delete the user
using the following commands:


```
curl -v localhost:8080/employees
curl -v localhost:8080/employees/99
curl -X POST localhost:8080/employees -H 'Content-type:application/json' -d '{"name":"Thor", "role":
"electrician"}'
curl -X PUT localhost:8080/employees/3 -H 'Content-type:application/json' -d '{"name":"Thor", "role":
"Norse God"}'
curl -X DELETE localhost:8080/employees/3
```



## What is a REST Service? 

So far in this tutorial we have covered creating a web-based service that handles the core operations 
involving employee data. This however, is not enough to make things "RESTful" 

* Pretty URLs like /employees/3 aren't REST 
* Merely using GET, POST, etc aren't REST 
* Having all the CRUD operations laid out aren't REST 


What we have built up to this point is better described as RPC (Remote Procedure Call). This is because
there is no way to know how to interact with this service. If it was published today, we would also 
have to write a document or host a developer's portal somewhere with all the details. 




This is a statement by Roy Fielding about the difference between REST and RPC: 
```
"I am getting frustrated with the number of people calling any HTTP-based interface a REST API. Today's
example is SocialSite REST API. That is RPC. It screams RPC. There is so much coupling on display that
it should be given an X rating. What needs to be done to make the REST architectural style clear on the
notion that hypertext is a constraint? In other words, if the engine of application state (and hence 
the API) is not being driven by hypertext, then it cannot be RESTful and cannot be a REST API. Period.
Is there some broken manual somewhere that needs to be fixed?" 
```





The side effect of NOT including hypermedia in our representations is that clients MUST hard code URIs 
to navigate the API. This leads to the same brittle nature that predated the rise of e-commerce on the
web. It's a signal that our JSON output needs a little help. 

Introducing Spring HATEOAS, a Spring project aimed at helping to build hypermedia-driven outputs. To 
upgrade the service to being RESTful, add this to your build: 


```
<dependency> 
	<groupId>org.springframework.boot</groupId> 
	<artifactId>spring-boot-starter-hateoas</artifactId> 
</dependency> 
```


This small library gives us the constructs to define a RESTful service and then render it in an 
acceptible format for client consumption. A critical ingredient to any RESTful service is adding links
to relevant operations. To make the controller more RESTful add links like this:




```
@GetMapping("/employees/{id}")
Resource<Employee> one(@PathVariable Long id) {
	Employee employee= repository.findById(id)
		.orElseThrow(() -> new EmployeeNotFoundException(id));

	return new Resource<>(employee,
		linkTo(methodOn(EmployeeController.class).one(id)).withSelfRel(),
		linkTo(methodOn(EmployeeController.class).all()).withRel("employees"));
}
```





This is very similar to what we had before, but a few things have changed: 

* The return type of the method has changed from Employee to Resource<Employee>. Resource<T> is
  a generic container from Spring HATEOAS that include not only the data but a collection of 
  links 

* linkTo(methodOn(EmployeeController.class).one(id)).withSelfRel() asks that Spring HATEOAS 
  build a link to the EmployeeCOntroller's one() method, and flag it as a self link 

* linkTo(methodOn(EmployeeController.class).all()).withRel("employees") asks Spring HATEOAS to 
  build a link to the aggregate root, all(), and call it "employees" 



What do we mean by "build a link"? One of Spring HATEOAS's core types is Link. It includes a URI and a
rel (relation). Links are what empower the web. Before the World Wide Web. other document systems 
would render information or links, but it was the link of documents with data that stitched the web 
together. 

Roy Fielding enourages building APIs with the same techniques that made the web successful and links 
are one of them. 

If you restart the application and query the employee record of Tony, there is now a slightly different
response than earlier: 

```
$ curl -v localhost:8080/employees/1
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
  0     0    0     0    0     0      0      0 --:--:-- --:--:-- --:--:--     0*   Trying ::1...
* TCP_NODELAY set
* Connected to localhost (::1) port 8080 (#0)
> GET /employees/1 HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/7.63.0
> Accept: */*
>
< HTTP/1.1 200
< Content-Type: application/hal+json;charset=UTF-8
< Transfer-Encoding: chunked
< Date: Mon, 25 Feb 2019 23:52:19 GMT
<
{ [175 bytes data]
100   169    0   169    0     0    676      0 --:--:-- --:--:-- --:--:--   676{"id":1,"name":"Tony Stark","role":"IT Technician","_links":{"self":{"href":"http://localhost:8080/employees/1"},"employees":{"href":"http://localhost:8080/employees"}}}
* Connection #0 to host localhost left intact
```


Notice now that the output shows not only the data elements seen before (id, name, and role), but also
a \_link entry containing two URIs. This is formatted using Hypertext Application Language (HAL) - an 
Internet Draft standard convention for definining hypermedia such as links to external resources within
JSON or XML code. HAL is a lightweight mediatype that allows encoding not just data but also hypermedia
controls, alerting consumers to other parts of the API they can navigate toward. In this case, there is
a "self" link (kind of like a this statement in coding) along with a link back to the aggregate root. 
(Aggregate roots are the only objects clients load and it includes access to child objects) 

To make the aggregate root also more RESTful, you want to include top level links while ALSO including
any RESTful components within. 










































