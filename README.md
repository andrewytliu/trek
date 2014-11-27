# Job List
## Done
* VR: Normal phase commit
* VR: View change
* VR: Recovery
* KeyValueStore

## Todo
* Testing coordinator
* Make DStoreService async and handle timeouts in client (see below)
* VR: Take a look into concurrency issue
* VR: Efficient recovery
* VR: State transfer

## Nice to have
* Command line argument parser for DStoreServer and DStoreClient

# Test plan
## Correctness
* Fail model: Crash or network partition
* Fail node: Primary or slave
* Recover timing: Before/during/after view change
* Correctness: By comparing every entry in log

Suppose k = 3 the normal action would be (primary = 1):

1. 1 -> 2 : prepare
2. 1 -> 3 : prepare
3. 2 -> 1 : prepareOK
4. 3 -> 1 : prepareOK
5. 1 -> 2 : commit
6. 1 -> 3 : commit

After each step, test to fail the nodes by 4 times (fail model * fail node)

## Implementation
* Additional DStoreTesting interface that allows the monitor to set whether a server is
  partitioned
* Each method implementing DStoreInternal checks if partitioned and just return if true
  * Why not just check in RpcServer and not call handle if partitioned
    * What is the behaviour if we do this? (Actually time out? Return invalid result?)
    * Monitor would never be able to set isPartitioned back to false
* DStoreService should be made async so that we can do the same to simulate timeouts due to
  partitioning (another interface?); right now we have to return a StoreResponse
* Testing crashes: Probably easier to just kill processes locally? It probably isn't impossible
  to implement a method that kills the server, but we'd still need to bring the server back up
  manually for recovery
  * If we don't actually kill the server and instead just ignore all requests, then it becomes
    like the partitioning case
  * Maybe extend the partitioning case? Crash = partition + clear log or something

## Benchmark
* Need to test for RPC speed first
* Is this really important?
* Conclusion: Should probably just skip
