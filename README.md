### **README.md**

# **HTTP Proxy Server with LRU Cache**

This project implements an HTTP Proxy Server that uses a Least Recently Used (LRU) caching mechanism to store and serve frequently accessed responses. The proxy acts as an intermediary between clients and servers, forwarding requests, caching responses, and improving overall performance by reducing redundant requests to the server.

---

## **Features**
- Proxies HTTP requests from clients to target servers.
- Caches HTTP responses using an LRU cache for faster repeated access.
- Evicts the least recently used entries when the cache limit is reached.
- Supports HTTP connections via `HttpURLConnection`.
- Handles image processing via `BufferedImage` and `ImageIO`.

---

## **Technologies Used**
- **Java Standard Libraries**:
  - `java.net`: For managing URL and HTTP connections.
  - `java.util`: For LRU cache implementation using collections.
  - `java.io`: For reading and writing input/output streams.
  - `javax.imageio`: For handling image data.
  - `java.awt.image.BufferedImage`: For working with images in memory.

---

## **How It Works**
1. **Proxy Server**:
   - Receives HTTP requests from a client.
   - Forwards the request to the target server using `HttpURLConnection`.
   - Retrieves and processes the server's response.
   - Caches the response for future requests.

2. **LRU Cache**:
   - Implemented using a customized `LinkedHashMap`.
   - Automatically evicts the least recently used cache entry when the cache size exceeds the defined limit.

3. **Image Handling**:
   - Supports processing and caching of images using `BufferedImage` and `ImageIO`.

---
