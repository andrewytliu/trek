# Trek: Testable Replicated Key-Value Store

A key-value store with ZooKeeper-like client semantics, backed by Viewstamped Replication, and with the ability to simulate failures.

The `run` script assumes a 3-node server cluster on `corn19`, `corn20` and `corn21`, with a monitor on `corn22`. To run in other configurations, modify `bin/run`.

## Compiling
Make sure you have JDK 7 and Maven installed and run `bin/run compile`.

## Usage
In use, the system consists of a cluster of server replicas, clients and / or a tester program and a monitor for collecting server internal logs.

### Servers
Start by running `bin/run server server_id`.

### Monitor
Start by running `bin/run monitor`.

### Clients
Start by running `bin/run client`.

#### Operations
* Create node: `create path data [isSequential=false]`
* Delete node: `delete path [ignoreVersion=false]`
* Test node existence: `exists path`
* Get node data: `getData path`
* Set node data: `setData path data [ignoreVersion=false]`
* List node children: `getChildren path`

### Tester program
Start by running `bin/run tester`.

#### Operations
* Simulating network partitions
  * Simulate partition (make specified replicas appear partitioned from other replicas): `partition server1[|server2|...]`
  * Resolve partition (make specified replicas reachable from one another): `group server1|server2|...`
  * Resolve all partitions: `reset`
  * View current partition status: `print`
* Simulating crash failures
  * Simulate crash failure at `server` after `rpcCount` outgoing RPCs: `kill server [rpcCount=0]`
  * Bring back crashed replica: `recover server`
  * View current replica status: `health`
* Automated testing
  * Test network partitions during one VR commit: `normaltest`
  * Test crash failures during one VR commit: `partitiontest`
* Others
  * Check if all nodes have consistent commit logs: `consistent`
  * Adjust VR heartbeat to `t` ms: `heartbeat t`
