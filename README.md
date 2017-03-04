# ElasticSearchCodeSearchEngine
An excerpt of a functional reactive distributed search engine in Scala that indexed OpenHub projects/code.

Functional reactive distributed search engine in Scala using Akka; based on ElasticSearch, deployed on Google Cloud services. Using Ohloh RESTful API to OpenHub, data was streamed. Multiple actors handled requests to stream data, process JSON responses/data, index data into ElasticSearch, and return user search results. 
