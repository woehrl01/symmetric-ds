# SymmetricDS
SymmetricDS is a database and file synchronization solution that is platform-independent, web-enabled, and database agnostic.  SymmetricDS was built to make data replication across two to tens of thousands of databases and file systems fast, easy and resilient.  We specialize in near real time, bi-directional data replication across large node networks over the WAN or LAN.

## Development Setup
We use Eclipse for development.

To setup a development environment run the following commands:
```
cd symmetric-assemble
./gradlew develop
```

This will generate Eclipse project artifacts.  You can then import the projects into your Eclipse workspace.

## 4.0 Architecture

SymmetricDS 4.0 will include a rearchitecture of some of the SymmetricDS software layers to enable new features and provide optimizations for performance and usability.

### Immediate Goals
These are goals for the 4.0 release.
* Simplify Code Struture
* Simplify Service APIs
  * Java Services
  * REST
  * Allow curl to be used to manage servers
  * Make it easy to use SymmetricDS APIs to use embedded in applications for general purpose CDC
* Improve performance
  * More asynchronicity
  * Make channels even more independent  
    * Asynchronous
    * Data capture table per channel.  Remove contention on data capture between channels
  * Reduce the overhead of processing a batch
    * The status on batch tables were getting updated more often that the actual data that changed
* Improve Robustness
  * Support versioning of configuration
  * Transports 
    * Keep connections alive during loads
    * Send or request status of unacked batches so batches are not sent multiple times
    * Back off on errors
 * Simplify registration and distribution of configuration 
 * Make sending data loads cleaner
   * Remove the concept of initial load and reverse initial load from sym_node_security in favor of some type of 
 * Clean up DDL Utils
   * Faster
   * More accurate
   * Support reading metadata about triggers
   * Automatic DDL replication
* Architect to support new features
  * Separate the configuration datasource from the runtime datasource
    * Do not require SymmetricDS tables to be installed on a target database
    * Allow the architecture to better support NOSQL data source and targets
load request structure

### Longer Term Goals
These are goals for post 4.0, but should be kept in mind while working on 4.0.
* Support NOSQL databases
   * Hadoop
   * Elasticsearch
   * Mongo
   * Cassandra
* Update the professional web console to use REST services
* Snapshot replication
* General purpose database maintainence jobs
* Auto Update Infrastructure
* Invent Routing DSL that is used for dynamic routing and data loads
* Log scrapping
