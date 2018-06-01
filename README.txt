# myFirstWebserver

myFirstWebserver is a Java project with the following .java files:

Application.java
StockInit.java
BadInputException.java
NegativeVolumeException.java
StockNotFoundException.java
UserNotFoundException.java
UsersPortfolios.java

Please make sure that "pom.xml" is in the package so you get all the dependencies.


The project is represents a partial solution (does not fulfill all the technical requirements) for the Fyber Junior Software Engineer challenge.

Indeed, the project was complex. Solving this project exposed me to the following new technologies & concepts:
* Apache Tomcat
* Spring Boot
* Spring MVC
* REST
* Maven (dependencies, etc)
* Postman
* Jmeter
* External APIs (IEX trading)
and still I was able to get to the point that I did. Thank you for the challenge, and I hope that I was able to demonstrate significant learning skills.


## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes.

### Prerequisites

Make sure that you have Java 1.8 running.
Eclipse IDE (or another)


### Installation & Deployment

Open up your favorite IDE
(This assume you are running Eclipse IDE)

Files > Import > Import Existing Projects into Workspace > Select archive file > Navigate to the downloaded myFirstWebserver.tar file & select it > Finish

Now you should have the project imported into your IDE.

Let's run the server:

1. Open the newly imported project
2. Open src/main/Java and double click Application.java
3. Run the application as a Spring Boot Application

If the console is not showing, then open it (Windows > Show view > Console). After a few moments, you should see the application starting up and the server is running.

To see the server in action, use an HTTP request application such as Postman.
Configure the requests as follows:

Request to                                   Request Method       @Params  Value     	 Format               
---------------------------------------------------------------------------------------------------------
localhost:8080/newuser                        POST              Body             		 application/json

localhost:8080/existinguser/updatestocks      POST				@Param     clientID   	 number
																Body					 application/json
																
localhost:8080/existinguser/getvalue		  GET				@Param	   clientID		 number
localhost:8080/buying/performance			  GET				@Param     clientID		 number
localhost:8080/buying/stability				  GET				@Param     clientID		 number
localhost:8080/buying/best					  GET				@Param	   clientID		 number


A request that does not conform to the specification listed above will be met with an error, though the server ~will not~ should not crash. The server is fairly resilient to crashes.

Stock symbols may be input in lower or upper case, but they must be valid stock symbols. A properly formatted JSON stock portfolio is formatted as an array of stock symbols and the volume (amount) that the owner owns. For example:

[
  {
    "name": "AAPL",
    "volume": 2
  },
  {
    "name": "GOOGL",
    "volume": 40
  },
  {
    "name": "MSFT",
    "volume": 3
  }
]

To easily edit the JSON for testing purposes, you can use an online JSON editting tool linked to below. Users are encouraged to play around with the stock tool, and test the application for resilience to crashing by sending incorrectly formatted data, invalid stock symbols (ie. APPL, GOOGLE, etc.), and negative volume (stock amounts).

https://jsoneditoronline.org/?id=c4a09cd677bd469db023fd0a58a9e911

Market prices will be updated every minute (and not at every request) to reduce latency on the client side. Stock prices are updated through the IEX stock API which samples real world relevant stock data. More information at https://iextrading.com/developer/ .

Users should first open a new portfolio to recieve a client ID and only then execute the other actions. Any other order of operations will be met with an error. Updates to an existing stock portfolio is done by sending the users totally new portfolio (NOT by sending a 'delta' portfolio or list of changes). 

Please note, the application is missing a persistence layer meaning that if the server is shut down, then all user information is lost. The developer will continue work on the system and attempt to implement a persistence layer based on MongoDB.


## Built With

* [Maven](https://maven.apache.org/) - Dependency Management

## Authors

* Noah Lerner - Junior Software Engineer candidate

## Acknowledgments

* Hat tip to Spring.io for their helpful tutorials.
* Hat tip to the experts at StackOverflow with for their abundance of knowledge.
* Hat tip to all the people who had the same bugs as I did which made solving mine much easier.
* Hat tip to you (the reader) for reviewing my project and getting to this acknowledgement.
