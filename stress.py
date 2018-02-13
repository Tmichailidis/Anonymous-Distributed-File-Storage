import os
import subprocess
from time import sleep
from subprocess import call
from time import gmtime, strftime

output_dir = "output"
if not os.path.exists(output_dir):
    os.makedirs(output_dir)

compile_base_path = "src/gr/uoa/di/"
client_files = ["Client", "Util", "ClientFileManager", "Crypto"]
uploader_files = ["Uploader", "Client", "Util", "ClientFileManager", "Crypto"]
node_files = ["Node", "NodeHandler", "Crypto", "DataStore", "StorageLRU", "DataStore", "TaskMaintenance", "Util", "Requests", "ServerThread", "RoutingTable"]

#compile node
compile_args = ["javac", "-d", "output/"]
for file in node_files:
	compile_args.append(compile_base_path + file + ".java")
print("Compiling node")
call(compile_args)

#compile client
compile_args = ["javac", "-d", "output/"]
for file in client_files:
	compile_args.append(compile_base_path + file + ".java")
print("Compiling client")
call(compile_args)

#compile uploader
compile_args = ["javac", "-d", "output/"]
for file in uploader_files:
	compile_args.append(compile_base_path + file + ".java")
print("Compiling uploader")
call(compile_args)


print(strftime("%Y-%m-%d %H:%M:%S", gmtime()))
#run multiple nodes
# rtSize (eq) files_num * 3
nodes_num = 50
rtSize = 10000
portNum = 65000
print("Starting nodes")
processes = []
for i in range(nodes_num):
	run_args = ["java", "-cp", "./output/", "gr.uoa.di.NodeHandler", str(portNum), str(portNum - 1), str(rtSize)]
	processes.append(subprocess.Popen(run_args))
	portNum += 1

sleep(20)
print("Starting clients")
#connect clients to nodes and upload some files
portNum = 65000
for i in range(nodes_num):
	run_args = ["java", "-cp", "./output/", "gr.uoa.di.Uploader", str(portNum)]
	processes.append(subprocess.Popen(run_args))
	portNum += 1