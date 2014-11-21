# Job List
## Done
* VR: Normal phase commit
* VR: View change
* VR: Recovery
* KeyValueStore

## Todo
* DStoreClient CLI for the *REAL* command
* Testing coordinator
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

## Benchmark
* Need to test for RPC speed first
* Is this really important?
