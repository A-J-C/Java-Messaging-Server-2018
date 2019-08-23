REM add javac to path
set "path=%path%;C:\Program Files (x86)\Java\jdk1.8.0_111\bin"		

REM compile client
javac client\Client.java 			
REM compile server
javac server\Server.java								

REM launch server
start cmd /k java server.Server		
REM launch client after sleeping for 2 seconds to give the servers a change to start up
TIMEOUT 2
start cmd /k java client.Client 				