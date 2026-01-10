# Java Multi-Client Chat Application with File Transfer

A **console-based multi-client chat application** built using **Java Socket Programming** that supports:

- Real-time chat
- Private messaging
- Online user tracking
- **Targeted file transfer between specific users**
- Communication across **different computers on the same network**

This project follows **real-world client–server architecture** and uses a **protocol-enforced design** to ensure correctness and reliability.

---

## Features

### Chat Features
- Multiple clients can connect simultaneously
- Broadcast messaging (send message to all users)
- Private messaging using `@username`
- Real-time online/offline user updates
- Error handling

### File Transfer Features
- Send files to a **specific user only**
- Receiver approval before file transfer (commented down in code for now)
- File transfer runs in **parallel threads**
- Chat does **not freeze** during file transfer
- Works across **different PCs on the same LAN**
- Prevents empty or corrupted files

---

## 🏗 Architecture Overview

Client ──(CHAT: 7777)──▶ Server ◀──(CHAT: 7777)── Client   
Client ──(FILE: 8888)──▶ Server ◀──(FILE: 8888)── Client


### Ports Used
| Port |        Purpose              |
|------|-----------------------------|
| 7777 | Chat & control messages     |
| 8888 | File transfer (binary data) |

---

## Project Structure

├── Server.java  
├── ClientHandler.java  
├── Client.java  
└── README.md  


### File Description
- **Server.java**  
  Starts the chat server and file relay server.

- **ClientHandler.java**  
  Handles one connected client (chat + file protocol).

- **Client.java**  
  Console-based client application.

---

## Communication Protocol

### Chat Commands/Protocols
|      Command          |     Description    |
|-----------------------|--------------------|
| `ENTER_USERNAME`      | Username handshake |
| `MESSAGE\|user\|text` | Private message    |
| `BROADCAST\|text`     | Broadcast message  |
| `ONLINE_USERS\|list`  | Online users       |
| `ERROR`               | Error from client  |
| `LOGOUT`              | Disconnect         |

---

### File Transfer Protocol (Protocol-Enforced Pairing)

1. Sender requests file transfer  
`FILE_REQUEST|receiver|filename|size`

3. Receiver receives offer  
`FILE_OFFER|sender|filename|size`

4. Receiver accepts  
`FILE_ACCEPT|sender|filename`

4. Server authorizes transfer  
`FILE_START|sender|receiver|filename|size`

6. Both clients connect to `FILE_PORT (8888)`

7. Server relays file bytes
    File transfer starts **only after FILE_START**  
    Prevents wrong pairing and empty files

---
