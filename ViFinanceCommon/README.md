# ViFinanceNews Project

A modular Java-based news reading web application.

## 📦 Project Structure

vi_finance_news/
│
├── ViFinanceCommon/ # Shared models and utilities
├── AuthService/ # Authentication service (local and Google)
├── UserService/ # User management service
├── pom.xml # Parent POM (Java 22, modules, dependencies)
├── mvnw / mvnw.cmd # Maven Wrapper (Linux/Windows)
└── .mvn/jvm.config # Java version config


---

## ✅ Prerequisites

Before you begin, ensure you have the following installed on your machine:

- **[Java 22 JDK](https://jdk.java.net/22/)**  
  After installation, set your environment variable:
  - `JAVA_HOME` → path to JDK 22 (e.g., `C:\Java\jdk-22`)
  - Add `%JAVA_HOME%\bin` to your system `Path`
  - Confirm with:
    ```bash
    java -version
    ```

> ⚠️ Java 22 is required to compile and run this project.

---

🚀 Getting Started
1. Clone the Repository
git clone https://github.com/your-username/vi_finance_news.git
cd vi_finance_news

2. Build with Maven Wrapper (no need to install Maven)
On Windows:
mvnw.cmd clean install

On Linux/macOS:
./mvnw clean install

3. Run a Service
Example (for UserService):

bash
Copy
Edit
cd UserService
java -jar target/UserService.jar

⚙️ Environment Variables
Some modules use a .env file to load secrets and config (e.g., Redis, DB):

Place a .env file inside each required module:
ViFinanceCommon/.env
🛡️ .env files should not be committed. Add this to .gitignore.

🧪 Running Tests
mvnw.cmd test   # on Windows
./mvnw test     # on Linux/macOS
