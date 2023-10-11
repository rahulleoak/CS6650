# CS6650 : Building Scalable and Distributed Systems

#Notes:
- To acheive the same throughput in the result, you need to create 2 EC2 instances and run the client (JAR) file on one and connect it with the server on the second instance.
- The GoServer at higher levels of requests will tend to crash due to overload, currently there is no workaround so just re-run the server with the same command you used to run when the console indicates it crashed
