## protopie be

## 1. project configuration
```
docker compose up --build
```

## 2. 사용한 스택 및 라이브러리
* Server Application: Spring Boot (Webflux)
* Server Language: Kotlin
* Database: Postgres
* Messaging: Kafka

## 3. Database Schema

서버를 실행하게 되면 init.sql 이 실행됩니다. 해당 sql에 이번 어플리케이션에서 사용되는 DB 스키마가 포함되어 있습니다.

기능을 간소화하기 위해 email/password(암호화)/userName/role 만 설정하였습니다.

## 4. Logic Explanation

* API specification: 서버가 뜬 후 http://localhost:8080/webjars/swagger-ui/index.html 에서 확인 가능합니다
* signup/signin 은 인증 없이 접근 가능합니다.
* 이 외의 endpoint는 인증 없이 접근했을 경우 401 에러가 반환됩니다.
* 권한이 없는 사용자는 403 에러가 반환됩니다. 권한이 없는 사용자는 다음과 같습니다.
  * GET /users Endpoint: 관리자(Admin)이 아닌 경우
  * GET, PUT, DELETE /users/{userId} Endpoint: 관리자가 아닌 경우, 혹은 Member이면서 본인이 아닌 경우
* MessageQueue는 Kafka를 사용했습니다. 탈퇴시에 발급되며 탈퇴한 회원의 id와 탈퇴 시점이 전송됩니다.
* UnitTest는 Controller/Service의 비즈니스 로직에 적용되어 있습니다.
* IntegrationTest는 인증/인가 위주의 케이스에 적용되어 있습니다.