# Description
A library of classes and functions implementing and augmenting the HTTP and Websocket protocols.
An image sharing site is then built using these classes and functions.
Supports accounts, global chat, comments, and image uploads. HTTP API uses buffering to support large file uploads.

# Features
* A Scala class that enables the instantiation of an object that contains the fields and payload of an HTTP message sent over a TCP connection. The class supports buffering of payload messages (appending multiple messages from a TCP stream) for large file uploads.
* A Scala class that enables the instantiation of an object that contains the cookies of an incoming HTTP request and the cookies that should be sent in an outgoing HTTP response.
* An API for parsing websocket data sent over a TCP connection.
* An API for building HTTP response messages.
* The site allows for large image uploads, posts, comments, user accounts, and a global chat feature.

# Running the Site Locally using Docker
* Ensure Docker is installed on your PC.
* Execute these commands in the root directory of the cloned repo:
```
docker build -t image_sharing_site .
docker run --publish 8006:8001 --name image_sharing_site image_sharing_site
```
Wait for the Docker container to complete setup and the go to http://localhost:8006/ in a browser of your choice.
