# Trek: Testable Replicated Key-Value Store

A key-value store with ZooKeeper-like client semantics, backed by Viewstamped Replication, and with the ability to simulate failures.

The `run` script assumes a 3-node server cluster on `corn19`, `corn20` and `corn21`, with a monitor on `corn22`. To run in other configurations, modify `bin/run`.

## Compiling
Run `bin/run compile`.

## Usage
In use, the system consists of a cluster of server replicas, clients and / or a tester program and a monitor for collecting server internal logs.

### Servers
Start by running `bin/run server SERVER_ID`.

### Monitor
Start by running `bin/run monitor`.

### Clients
Start by running `bin/run client`.

### Tester program
Start by running `bin/run tester`.
