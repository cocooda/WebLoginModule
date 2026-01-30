# WebLoginModule

This module provides the core authentication and authorization functionality. It is designed with modularity, security, and integration in mind, enabling seamless user management across a multi-service architecture.

## 🔑 Key Features
- Secure Sign-In/Sign-Up Flows  
Role validation, access control, and session/token management.

- Authentication Services  
Local and Google-based authentication support.

- Authorization Logic  
Role-based access control for different user types.

- Microservice Integration  
Built as part of a modular system, demonstrating backend system thinking and engineering discipline.

- Reusable Design  
Can be integrated into other web applications as a standalone login module.

## 📦 Project Structure

- WebLoginModule/
+ ServiceCommon/ # Shared models and utilities
+ AuthService/ # Authentication service (local and Google)
+ UserService/ # User management service
+ pom.xml # Parent POM (Java 22, modules, dependencies)
+ mvnw / mvnw.cmd # Maven Wrapper (Linux/Windows)
+ .mvn/jvm.config # Java version config


---

## ✅ Prerequisites

Before you begin, ensure you have the following installed on your machine:

- **[Java 22 JDK](https://jdk.java.net/22/)**  
  After installation, set your environment variable:
  - `JAVA_HOME` → path to JDK 22 (e.g., `C:\Java\jdk-22`)
  - Add `%JAVA_HOME%\bin` to your system `Path`
  - Confirm with:
    ```
    java -version
    ```

> ⚠️ Java 22 is required to compile and run this project.

---

🚀 Getting Started
1. Clone the Repository
```
git clone https://github.com/cocooda/WebLoginModule.git
cd WebLoginModule
```

2. Build with Maven Wrapper (no need to install Maven)
On Windows:
```
mvnw.cmd clean install
```

On Linux/macOS:
```
./mvnw clean install
```

3. Run a Service
Example (for UserService):
```
cd UserService
java -jar target/UserService.jar
```
⚙️ Environment Variables
Some modules use a .env file to load secrets and config (e.g., Redis, DB):

Place a .env file inside each required module:
- `SerivceCommon/.env`
🛡️ .env files should not be committed. Add this to .gitignore.

🧪 Running Tests
```
mvnw.cmd test   # on Windows
./mvnw test     # on Linux/macOS
```