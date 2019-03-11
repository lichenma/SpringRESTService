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

<br><br>
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


<br><br>
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


<br><br>
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


<br><br>
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
any RESTful components within: 

```java 
@GetMapping("/employees")
Resources<Resource<Employee>> all() {
	List<Resource<Employee>> employees=repository.findAll().stream()
		.map(employee -> new Resource<>(employee, 
			linkTo(methodOn(EmployeeController.class).one(employee.getId())).withSelfRel(),
			linkTo(methodOn(EmployeeController.class).all()).withRel("employees")))
		.collect(Collectors.toList());

	return new Resources<>(employees, 
		linkTo(methodOn(EmployeeController.class).all()).withSelfRel());
}
```

The method which used to just be repository.findAll() has now grown to incorporate many more elements.
Lets examine them: 

* *Resources<>* is another Spring HATEOAS container aimed at encapsulating collections. It also lets 
  the user include links. 
  
What does "encapsulating collections" mean? Is it collections of employees? 

Not exactly.  

Since we are talking REST, it should encapsulate collections of employee resources. This is why we 
fetch all the employees, but then transform them into a list of Resource<Employee> objects (Thanks 
to Java 8 Stream API). 

Now if we restart the application and fetch the aggregate root, we see the following: 

```
$ curl -v localhost:8080/employees
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
  0     0    0     0    0     0      0      0 --:--:-- --:--:-- --:--:--     0*   Trying ::1...
* TCP_NODELAY set
* Connected to localhost (::1) port 8080 (#0)
> GET /employees HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/7.63.0
> Accept: */*
>
< HTTP/1.1 200
< Content-Type: application/hal+json;charset=UTF-8
< Transfer-Encoding: chunked
< Date: Fri, 08 Mar 2019 16:14:27 GMT
<
{ [444 bytes data]
100   437    0   437    0     0    458      0 --:--:-- --:--:-- --:--:--   458{"_embedded":{"employeeList":[{"id":1,"name":"Tony Stark","role":"IT Technician","_links":{"self":{"href":"http://localhost:8080/employees/1"},"employees":{"href":"http://localhost:8080/employees"}}},{"id":2,"name":"Bruce Wayne","role":"Business Analyst","_links":{"self":{"href":"http://localhost:8080/employees/2"},"employees":{"href":"http://localhost:8080/employees"}}}]},"_links":{"self":{"href":"http://localhost:8080/employees"}}}
* Connection #0 to host localhost left intact
```

If we format the output, we are now given this JSON when we fetch the aggregate root: 

```
{  
   "_embedded":{  
      "employeeList":[  
         {  
            "id":1,
            "name":"Tony Stark",
            "role":"IT Technician",
            "_links":{  
               "self":{  
                  "href":"http://localhost:8080/employees/1"
               },
               "employees":{  
                  "href":"http://localhost:8080/employees"
               }
            }
         },
         {  
            "id":2,
            "name":"Bruce Wayne",
            "role":"Business Analyst",
            "_links":{  
               "self":{  
                  "href":"http://localhost:8080/employees/2"
               },
               "employees":{  
                  "href":"http://localhost:8080/employees"
               }
            }
         }
      ]
   },
   "_links":{  
      "self":{  
         "href":"http://localhost:8080/employees"
      }
   }
}
```

This root node serves up a collection of employee resources but there is also a top-level "self" link.
The "collection" is listed under the "\_embedded" section. This is how HAL represents collections. 

Each individual member of the collection has their information as well as related links. 

The importance of these links is that it makes it possible to evolve REST services over time - 
Scalability. Existing links can be maintained while new links are added in the future. Newer clients 
may take advantage of the new links, while legacy clients can sustain themselves on the old links. This
is especially helpful if services get relocated and moved around. As long as the link struture is 
maintained, clients can STILL find and interact with things. 


<br><br> 
## Simplifying Link Creation 

As the code stands right now, there is a lot of repetition when creating a single employee link. The 
code to provide a single link to an employee as well as an "employees" links to the aggregate root 
was shown twice. 

We can tackle this issue by simply defining a function that converts Employee objects to 
Resource<Employee> objects. While we could just create this, there are benefits down the road if we 
implement Spring HATEOAS's *ResourceAssemble* interface 


```java 
package payroll; 

import static org,springframework.hateoas.mvc.ControllerLinkBuilder.*;

import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.stereotype.Component; 

@Component
class EmployeeResourceAssembler implements ResourceAssembler<Employee, Resource<Employee>> {

	@Override
	public Resource<Employee> toResource(Employee employee) {
		
		return new Resource<>(employee, 
			linkTo(methodOn(EmployeeController.class).one(employee.getId())).withSelfRel(),
			linkTo(methodOn(EmployeeController.class).all()).withRel("employees"));
	}
}
```

This simple interface has one method: *toResource()*. It is based on converting a non-resource object
(Employee) into a resource-based object(Resource<Employee>).

All the code previously used in the controller can be moved into this class. By applying Spring 
Framework's @Component, this component will be automatically created when the application starts 




```
   	Spring HATEOAS's abstract base class for all resources is ResourceSupport. But for 
	simplicity,Resource<T> can be used to wrap all Plain Old Java Objects (POJOs) as resources
```




To leverage this assembler, you only have to alter the EmployeeController by injecting the assembler
in the constructor. Then the class becomes: 

```java 
@RestController 
class EmployeeController {
	
	private final EmployeeRepository repository; 

	private final EmployeeResourceAssembler assembler; 

	EmployeeController(EmployeeRepository repository, EmployeeResourceAssembler assembler) {
		
		this.repository = repository; 
		this.assembler = assembler; 
	}
	
	...

}
```


Now we can use it in the single-item employee method: 

```java
@GetMapping("/employees/{id}")
Resource<Employee> one(@PathVariable Long id) {
	
	Employee employee = repository.findById(id)
		.orElseThrow(() -> new EmployeeNotFoundException(id)); 

	return assembler.toResource(employee); 
}
```

The code has not changed a lot but now instead of creating the Resource<Employee> instance here, we 
delegate it to the assembler. 


We can do the same thing in the aggregate root controller to simplify things: 

```java 
@GetMapping("/employees")
Resources<Resource<Employee>> all() {
	
	List<Resource<Employee>> employees = repository.findAll().stream()
		.map(assembler::toResource)
		.collect(Collectors.toList());

	return new Resources<>(employees,
		linkTo(methodOn(EmployeeController.class).all()).withSelfRel());
}
```


The code is again almost the same but we got to replace all of the Resource<Employee> creation logic 
with map(assembler::toResource). Thanks to Java 8 method references, it is extremely easy to plug it in
and simplify the controller. 

```
	A key design goal for Spring HATEOAS is to make it easier to do The Right Thing TM. In this 
	scenario, adding hypermedia to the service without hard coding anything
```

At this stage, we have created a Spring MVC REST controller that produces hypermedia-powered content.
Clients that don't use HAL can ignore the extra bits while consuming the pure data. Clients that do use
HAL can better navigate the empowered API. 

This is great but not quite all of the things needed to build a truly RESTful service with Spring. 








<br><br> 
## Evolving REST APIs 



With one additional library and a few lines of extra code, we have added hypermedia to the application.
But that is not the only thing required for a RESTful service. Another import part of REST is the fact
that it is neither a technology stack nor a single standard. 



REST is a collection of architectural constraints that when adopted make an application more resilient.A key factor of resilience is that when you make upgrades to your services, your clients don't suffer 
from downtime. 



In the "olden" days, upgrades were notorious for breaking clients. In other words, an upgrade to the 
server required an update to the client. In this day and age, hours or even minutes of downtime spent
doing an upgrade can cost millions in lost revenue. 



Some companies require that you present management with a plan to minimize downtime. In the past, you 
could get away with upgrading at 2:00 am on a Sunday when load was at a minimum. But in today's 
internet-based e-commerce with international customers, such strategies are not as effective. 



SOAP-based services and CORBA-based services were incredibly brittle. It was hard to roll out a server
that could support both old and new clients. WIth REST-based practices, it's much easier. Especially 
using the Spring stack. 



Imagine this design problem: You have just rolled out a system with this Employee - based record. The 
system is a major hit. You have sold your system to countless enterprises. Suddenly, the need for an 
employee's name to be split into firstName and lastName arises. 



RIP. That is a problem 



Before you being changing the Employee class and replacing the name field with firstName and lastName, 
think about this. Will this break any clients? How long will it take to upgrade them? Do you even 
controll all of the clients accessing your services? 

```
Downtime = Lost Money
```



Is management ready for that? 





<br><br> 
## Strategy - Never Delete Values  


There is an old strategy that predates REST by years 


```
Never delete a column in a database 

			-unknown
```


You can always add columns (fields) to a database table but don't take any away. This principle is the 
same for RESTful services. We can add new fields to JSON representations but we should not take any 
away. 



```
{  
    "id":2,
    "firstName": "Bruce",
    "lastName": "Wayne",
    "name":"Bruce Wayne",
    "role":"Business Analyst",
    "_links":{  
       "self":{  
	  "href":"http://localhost:8080/employees/2"
       },
       "employees":{  
	  "href":"http://localhost:8080/employees"
       }
}
```        



In this formate we show firstName, lastName AND name. While there is a bit of duplication of
information, the purpose is to support both old and new clients. That means that you can upgrade the 
server without requiring clients to upgrade at the same time. A good move that should reduce downtime. 


Not only should we show this information in both the "old way" and the "new way" we should also process
incoming data both ways. 


Here's an example: 

* Employee record that handles both "old" and "new" clients 


```java
package payroll; 

import lombok.Data; 

import javax.persistence.Entity
import javax.persistence.GeneratedValue; 
import javax.persistence.Id; 

@Data
@Entity 
class Employee {
	
	private @Id @GeneratedValue Long id; 
	private String firstName; 
	private String lastName; 
	private String role; 

	Employee(String firstName, String lastName, String role) { 
		this.firstName=firstName; 
		this.lastName=lastName;
		this.role=role; 
	}

	public String getName() {
		return this.firstName + " "+this.lastName;
	}

	public void setName(String name) {
		String[] parts=name.split(" "); 
		this.firstName=parts[0];
		this.lastName=parts[1];
	}
}
```

This class is very similar to the previous version of Employee. Let's go over some of the changes: 

* Field name has been replaced by firstName and lastName. Lombok will generate getters and setters for
  these 

* A "virtual" getter for the old name property, getName() is defined. it uses the firstName and 
  lastName fields to produce a value 

* A "virtual" setter for the old name property is also defined, setName(). It parses an incoming string
  and stores it into the proper fields 



Not every change to the API is as simple as splitting a string or merging two strings but in many cases
it is not impossible to come up with a set of transformations. 




<br><br> 
## Ensure Proper Response

Another fine tuning is to ensure that each of the REST methods returns a proper response. We update the
POST method like this: 

```java
@PostMapping("/employees")
ResponseEntity<?> newEmployee(@RequestBody Employee newEmployee) throws URISyntaxException {
	
	Resource<Employee> resource = assembler.toResource(repository.save(newEmployee));

	return ResponseEntity
		.created(new URI(resource.getId().expand().getHref()))
		.body(resource);
}
```


* The new Employee object is saved as before but the resulting object is wrapped using the
  EmployeeResourceAssembler 

* Spring MVC's ResponseEntity is used to create an HTTP 201 Created status message. This type of 
  response typically includes a Location response header, and we use the newly formed link 

* Additionally, return the resource-based version of the saved object 


With this tweak in place, you can use the same endpoint to create a new employee resource, and use the 
legacy name field: 




```
$ curl -v -X POST localhost:8080/employees -H 'Content-Type:application/json' -d '{"name": "Ant Man", "role": "Thief"}'
```




The output is shown below: 

```
> POST /employees HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/7.63.0
> Accept: */*
> Content-Type:application/json
> Content-Length: 36
>
} [36 bytes data]
* upload completely sent off: 36 out of 36 bytes
< HTTP/1.1 201
< Location: http://localhost:8080/employees/3
< Content-Type: application/hal+json;charset=UTF-8
< Transfer-Encoding: chunked
< Date: Sat, 09 Mar 2019 17:53:59 GMT
<
{ [199 bytes data]
100   229    0   193  100    36    242     45 --:--:-- --:--:-- --:--:--   287
{  
   "id":3,
   "firstName":"Ant",
   "lastName":"Man",
   "role":"Thief",
   "name":"Ant Man",
   "_links":{  
      "self":{  
         "href":"http://localhost:8080/employees/3"
      },
      "employees":{  
         "href":"http://localhost:8080/employees"
      }
   }
}
* Connection #0 to host localhost left intact
```

This is not only has the resulting object rendered in HAL (both name as well as firstName/lastName), 
but also the Location header populated with *http://localhost:8080/employees/3*. A hypermedia powered
client could opt to "surf" to this new resource and proceed to interact with it. 


The PUT controller method requires similar tweaks: 

```java 
@PutMapping("/employees/{id}")
ResponseEntity<?> replaceEmployee(@RequestBody Employee newEmployee, @PathVariable Long id) throws 
URISyntaxException {

	Employee updatedEmployee = repository.findById(id)
		.map(employee -> {
			employee.setName(newEmployee.getName());
			employee.setRole(newEmployee.getRole());
			return repository.save(employee);
		})
		.orElseGet(() -> {
			newEmployee.setId(id);
			return repository.save(newEmployee);
		});

	Resource<Employee> resource = assembler.toResource(updatedEmployee);

	return ResponseEntity
		.created(new URI(resource.getId().expand().getHref()))
		.body(resource);
}
```

The Employee object built from the save() operation is then wrapped using the EmployeeResourceAssembler
into a Resource<Employee> object. Since we want a more detailed HTTP response code than 200 OK, we will
use Spring MVC's ResponseEntity wrapper. It has a handy static method **created()** where we can plug 
in the resource's URI. 


By grabbing the resource you can fetch it's "self" link via the getId() method call. This method yields
a Link which you can turn into a Java URI. To tie things up nicely, you inject the resource itself into
the body() method. 



```
	In REST, a resource's id is the URI of that resource. Hence, Spring HATEOAS doesn't hand you
	the id field of the underlying data type (which no client should), but instead, the URI for it.
	Do not confuse ResourceSupport.getId() with Employee.getId(). 
```


It is debatable if HTTP 201 Created carries the right semantics since we aren't necessarily "creating"
a new resource. But it comes pre-loaded with a Location response header, so we go with it. 


```
$ curl -v -X PUT localhost:8080/employees/3 -H 'Content-Type:application/json' -d '{"name": "Ant Man", "role": "Pest Control"}'
```

The employee resource has been updated and the location URI sent back: 

```

> PUT /employees/3 HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/7.63.0
> Accept: */*
> Content-Type:application/json
> Content-Length: 43
>
} [43 bytes data]
* upload completely sent off: 43 out of 43 bytes
< HTTP/1.1 201
< Location: http://localhost:8080/employees/3
< Content-Type: application/hal+json;charset=UTF-8
< Transfer-Encoding: chunked
< Date: Sat, 09 Mar 2019 20:00:52 GMT
<
{ [206 bytes data]
100   243    0   200  100    43    609    131 --:--:-- --:--:-- --:--:--   740
{  
   "id":3,
   "firstName":"Ant",
   "lastName":"Man",
   "role":"Pest Control",
   "name":"Ant Man",
   "_links":{  
      "self":{  
         "href":"http://localhost:8080/employees/3"
      },
      "employees":{  
         "href":"http://localhost:8080/employees"
      }
   }
}
* Connection #0 to host localhost left intact
```





Finally we update the DELETE operation suitably: 
```java 
@DeleteMapping("/employees/{id}")
ResponseEntity<?> deleteEmployee(@PathVariable Long id) {
	
	repository.deleteById(id);

	return ResponseEntity.noContent().build();
}
```




Now if we try to delete an employee profile we are given an HTTP 204 No Content response: 
```
$ curl -v -X DELETE localhost:8080/employees/1
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
  0     0    0     0    0     0      0      0 --:--:-- --:--:-- --:--:--     0*   Trying ::1...
* TCP_NODELAY set
* Connected to localhost (::1) port 8080 (#0)
> DELETE /employees/1 HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/7.63.0
> Accept: */*
>
  0     0    0     0    0     0      0      0 --:--:-- --:--:-- --:--:--     0< HTTP/1.1 204
< Date: Sun, 10 Mar 2019 18:00:22 GMT
<
  0     0    0     0    0     0      0      0 --:--:-- --:--:-- --:--:--     0
* Connection #0 to host localhost left intact
```


Making changes to the fields in the Employee class will also require coordination with the database 
team so that they can properly migrate existing content into the new columns. We are new ready for an
upgrade that will NOT disturb existing clients while newer clients can take advantage of the 
enhancements! 


There may be concern about the amount of information being sent over the wire. In some systems where 
every byte counts, evolution of APIs may need to take a backseat. Don't pursue such premature 
optimization until we measure. 









<br><br>
## Building links into the REST API



So far, we have built an evolvable API with the bare bones links. To grow the API and better serve 
clients, we need to embrace the concept of **Hypermedia as the Engine of Application State** 

What does this mean? We will explore it in the following section: 

Business logic inevitably builds up rules that involve processes. The risk of such systems is we often
carry such server-side logic into clients and build up strong coupling. REST is about breaking down 
such connections and minimizing such coupling. 

To show how to cope with state changes without triggering breaking changes in clients, imagine adding a
system that fulfills orders. 


As a first step, define an *Order* record: 

```java
package payroll; 

import lombok.Data; 

import javax.persistence.Entity; 
import javax.persistence.GeneratedValue; 
import javax.persistence.Id;

@Entity 
@Data
@Table(name="CUSTOMER_ORDER") 
class Order {
	
	private @Id @GeneratedValue Long id; 

	private String description; 
	private Status status; 

	Order(String description, Status status) {
		
		this.description = description; 
		this.status = status; 
	}
}
```


* The class requires a JPA @Table annotation changing the table's name to CUSTOMER\_ORDER because
  ORDER is not a valid name for table 

* It includes a description field as well as a status field

Orders must go through a certain series of state transitions from the time a customer submits an order
and it is either fulfilled or cancelled. This can be captured as a Java enum: 



```java 
package payroll; 

enum Status {

	IN_PROGRESS, 
	COMPLETED,
	CANCELLED;
}
```



This enum captures the various states an Order can occupy. For this tutorial, we will keep it simple.

To support interacting with orders in the database, you must define a corresponding Spring Data 
repository: 




```java 
interface OrderRepository extends JpaRepository<Order, Long> {
}
```



With this in place, you can now define a basic OrderController: 





```java
@RestController
class OrderController { 

	private final OrderRepository orderRepository; 
	private final OrderResourceAssembler assembler; 

	OrderController(OrderRepository orderRepository, orderResourceAssembler assembler) {
	
		this.orderRepository = orderRepository; 
		this.assembler = assembler; 

	} 

	@GetMapping("/orders")
	Resources<Resource<Order>> all() {
		
		List<Resource<Order>> orders = orderRepository.findAll().stream()
			.map(assembler::toResource)
			.collect(Collectors.toList());

		return new Resources<>(orders,
			linkTo(methodOn(OrderController.class).all()).withSelfRel());

	}

	@GetMapping("/orders/{id}")
	Resource<Order> one(@PathVariable Long id) {
		
		return assembler.toResource(
			orderRepository.findById(id)
				.orElseThrow(() -> new OrderNotFoundException(id)));
	}

	@PostMapping("/orders") 
	ResponseEntity<Resource<Order>> newOrder(@RequestBody Order order) {
		
		order.setStatus(Status.IN_PROGRESS); 
		Order newOrder= orderRepository.save(order);

		return ResponseEntity
			.created(linkTo(methodOn(OrderController.class).one(newOrder.getId())).toUri())
			.body(assembler.toResource(newOrder));
	}
}
```




* It contains the same REST controller setup as the controllers we have built so far
* It injects both an OrderRepository as well as a OrderResourceAssembler 
* The first two Spring MVC routes handle the aggregate root as well a single item Order resource 
  request
* The third Spring MVC route handles creating new orders by starting them in the IN\_PROGRESS state
* All the controller methods return one of Spring HATEOAS's ResourceSupport subclasses to properly 
  render hypermedia (or a wrapper around such a type) 




Before building the OrderResourceAssembler, let's take a look at what needs to happen. We are modelling
the flow of states between Status.IN\_PROGRESS, Status.COMPLETED and STATUS.CANCELLED. A natural thing
when serving up such data to clients is to let the clients make the decision on what it can do based on
this payload. 


But this would be wrong. 



What happens when a new state is introduced into this flow? The placement of various buttons on the UI
would probably be erroneous. 



What if the name of each state was changed, perhaps while coding international support and showing 
locale-specific text for each state? That would most likely break all the clients. 





<br><br> 
## Spring HATEOAS

Enter HATEOAS or Hypermedia as the Engine of Application State. Instead of clients parsing the payload,
given them links to signal valid actions. Decouple state-based actions from the payload of data. In 
other words, when CANCEL and COMPLETE are valid actions, dynamically add them to the list of links. 
**Clients only need show users the corresponding buttons when the links exist.** 

This decouples clients from having to know WHEN such actions are valid, reducing the risk of the server
and its clients getting out of sync on the logic of state transitions. 

Having already embraced the concept of Spring HATEOAS ResourceAssembler components, putting such logic
in the OrderResourceAssembler would be the perfect place to capture this business rule: 


```java 
package payroll; 

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.*;

import org.springframework.hateoas.Resource; 
import org.springframework.hateoas.ResourceAssembler; 
import org.springframework.stereotype.Component; 


@Component 
class OrderResourceAssembler implements ResourceAssembler<Order, Resource<Order>> {
	
	@Override
	public Resource<Order> toResource(Order order) {
		
		// Unconditional links to single-item resource and aggregate root 

		Resource<Order> orderResource = new Resource<>(order, 
			linkTo(methodOn(OrderController.class).one(order.getId())).withSelfRel(),
			linkTo(methodOn(OrderController.class).all()).withRel("orders")
		);

		// Conditional links based on state of the order 

		if (order.getStatus() == Status.IN_PROGRESS) { 
			
			orderResource.add(
				linkTo(methodOn(OrderController.class)
					.cancel(order.getId())).withRel("cancel"));
			orderResource.add(
				linkTo(methodOn(OrderController.class)
					.complete(order.getId())).withRel("complete"));
	}

	return orderResource;
}
```


This resource assembler always inclues the self link to the single-item resource as well as a link back
to the aggregate root. But it also includes two conditional links to OrderController.cancel(id) as well
as OrderController.complete(id). These links are ONLY shown when the order's status is
Status.IN\_PROGRESS. 



If clients can adopt HAL and the ability to read links instead of simply reading the data of plain old
JSON, they can trade in the need for domain knowledge about the order system. This naturally *reduces
coupling* between the client and server. And it opens the door to tuning the flow of order fulfillment
without breaking clients in the process. 



To round out order fulfillment, we add the following to the OrderController for the cancel operation: 



```java 
@DeleteMapping("/orders/{id}/cancel")
ResponseEntity<ResourceSupport> cancel(@PathVariable Long id) {

	Order order = orderRepository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));

	if (order.getStatus() == Status.IN_PROGRESS) {
		order.setStatus(Status.CANCELLED);
		return ResponseEntity.ok(assembler.toResource(orderRepository.save(order)));
	}

	return ResponseEntity
		.status(HttpStatus.METHOD_NOT_ALLOWED) 
		.body(new VndErrors.VndError("Method Not Allowed", "You Cannot Cancel an Order that is
		      in the " + order.getStatus() + " Status"));
}
```




This method checks the Order status before allowing it to be cancelled. If its not a valid state, it 
returns a Spring HATEOAS VndError, a hypermedia-supporting error container. If the transition is indeed
valid, it transistions the Order to CANCELLED. 






Now, we add the following to OrderController for order completion: 

```java 
@PutMapping("/orders/{id}/complete") 
ResponseEntity<ResourceSuppport> complete(@PathVariable Long id) {

	Order order= orderRepository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));

	if (order.getStatus() == Status.IN_PROGRESS) {
		order.setStatus(Status.COMPLETED);
		return ResponseEntity.ok(assembler.toResource(orderRepository.save(order)));
	}

	return ResponseEntity
		.status(HttpStatus.METHOD_NOT_ALLOWED)
		.body(new VndErrors.VndError("Method Not Allowed", "You Cannot Complete an Order that
		      is in the " + order.getStatus() + " Status"));
}
```


This implements similar logic to prevent an Order status from being completed unless in the proper 
state. 







<br><br> 
## Testing the Application


Now by adding a little extra initialization code to LoadDatabase: 

```java 
orderRepository.save(new Order("MacBook Pro", Status.COMPLETED));
orderRepository.save(new Order("iPhone", Status.IN_PROGRESS));

orderRepository.findAll().forEach(order -> {
	log.info("Preloaded " + order);
});
```

Now we can test things out. To use the new service just perform a few operations: 

```
$ curl -v http://localhost:8080/orders | json_pp

{
   "_links" : {
      "self" : {
         "href" : "http://localhost:8080/orders"
      }
   },
   "_embedded" : {
      "orderList" : [
         {
            "status" : "COMPLETED",
            "id" : 3,
            "description" : "MacBook Pro",
            "_links" : {
               "orders" : {
                  "href" : "http://localhost:8080/orders"
               },
               "self" : {
                  "href" : "http://localhost:8080/orders/3"
               }
            }
         },
         {
            "status" : "IN_PROGRESS",
            "id" : 4,
            "description" : "iPhone",
            "_links" : {
               "orders" : {
                  "href" : "http://localhost:8080/orders"
               },
               "complete" : {
                  "href" : "http://localhost:8080/orders/4/complete"
               },
               "cancel" : {
                  "href" : "http://localhost:8080/orders/4/cancel"
               },
               "self" : {
                  "href" : "http://localhost:8080/orders/4"
               }
            }
         }
      ]
   }
}
```

This HAL document immediately shows different links for each order, based upon its present state

* The first order, being COMPLETED only has the navigational links. The state transition links are not
  shown 

* The second order, being IN\_PROGRESS additionally has the cancel link as well as the complete link



<br><br>
Let's try cancelling an order: 

```
$ curl -v -X DELETE http://localhost:8080/orders/4/cancel | json_pp
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
  0     0    0     0    0     0      0      0 --:--:-- --:--:-- --:--:--     0*   Trying ::1...
* TCP_NODELAY set
* Connected to localhost (::1) port 8080 (#0)
> DELETE /orders/4/cancel HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/7.63.0
> Accept: */*
>
< HTTP/1.1 200
< Content-Type: application/hal+json;charset=UTF-8
< Transfer-Encoding: chunked
< Date: Mon, 11 Mar 2019 23:21:40 GMT
<
{ [167 bytes data]
100   161    0   161    0     0    234      0 --:--:-- --:--:-- --:--:--   234
* Connection #0 to host localhost left intact
{
   "id" : 4,
   "status" : "CANCELLED",
   "description" : "iPhone",
   "_links" : {
      "self" : {
         "href" : "http://localhost:8080/orders/4"
      },
      "orders" : {
         "href" : "http://localhost:8080/orders"
      }
   }
}
```

This response shows an HTTP 200 status code indicating that it was successful. The response HAL 
document shows that order in its new state (CANCELLED). And the state-altering links are gone. 



If we try running the same operation again: 

```
$ curl -v -X DELETE http://localhost:8080/orders/4/cancel | json_pp
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
  0     0    0     0    0     0      0      0 --:--:-- --:--:-- --:--:--     0*   Trying ::1...
* TCP_NODELAY set
* Connected to localhost (::1) port 8080 (#0)
> DELETE /orders/4/cancel HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/7.63.0
> Accept: */*
>
< HTTP/1.1 405
< Content-Type: application/hal+json;charset=UTF-8
< Transfer-Encoding: chunked
< Date: Mon, 11 Mar 2019 23:24:35 GMT
<
{ [108 bytes data]
100   102    0   102    0     0   3290      0 --:--:-- --:--:-- --:--:--  3290
* Connection #0 to host localhost left intact
{
   "logref" : "Method Not Allowed",
   "message" : "You Cannot Cancel an Order that is in the CANCELLED status"
}
```



This time we see an HTTP 405 Method Not Allowed response. DELETE has become an invalid operation. The
VndError response object clearly indicates that you aren't allowed to "cancel" an order already in the
"CANCELLED" status. 


Additionally, trying to complete the same order also fails: 

```
$ curl -v -X PUT http://localhost:8080/orders/4/complete | json_pp
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
  0     0    0     0    0     0      0      0 --:--:-- --:--:-- --:--:--     0*   Trying ::1...
* TCP_NODELAY set
* Connected to localhost (::1) port 8080 (#0)
> PUT /orders/4/complete HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/7.63.0
> Accept: */*
>
< HTTP/1.1 405
< Content-Type: application/hal+json;charset=UTF-8
< Transfer-Encoding: chunked
< Date: Mon, 11 Mar 2019 23:27:20 GMT
<
{ [110 bytes data]
100   104    0   104    0     0   6500      0 --:--:-- --:--:-- --:--:--  6500
* Connection #0 to host localhost left intact
{
   "logref" : "Method Not Allowed",
   "message" : "You Cannot Complete an Order that is in the CANCELLED status"
}
```

With all of this in place, the order fulfillment service is capable of conditionally showing what 
operations are available. It also guards against invalid operations. 


By leveraging the protocol of hypermedia and links, clients can be built sturdier and less likely to 
break simply because of a change in the data. And Spring HATEOAS eases building the hypermedia you need
to serve your clients. 




<br><br>
## Summary 

Thanks to spring.io for this tutorial and through it we have gone through various tactics to build 
REST API. As it happens, REST isn't just about pretty URIs and returning JSON instead of XML. 


Instead, the following tactics help make the services less likely to break exisitng clients you may or
may not control: 

* Do not remove old fields. Instead, support them. 
* Use rel-based links so clients don't have to hard code URIs
* Retain old links as long as possible. Even if you have to change the URI, keep the rels so older 
  clients have a path onto the newer features
* Use links, not payload data, to instruct clients when various state-driving operations are available


It may appear to be a bit of effort to build up ResourceAssembler implementations for each resource 
type and to use these components in all of your controllers. But this extra bit of server-side setup
(made simpler thanks to Spring HATEOAS) can ensure the clients you control (and more importantly those
you don't) can upgrade with ease as you evolve your API. 



