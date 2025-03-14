# cinebox_batch_service

이 프로젝트는 영화진흥위원회와 KMDB API를 사용하여 영화 데이터를 수집하고, 이를 데이터베이스에 저장하는 배치 서버입니다. 이 서버는 주기적으로 API를 호출하여 최신 영화 정보를 업데이트합니다.

## 기능

- 영화진흥위원회와 KMDB API를 통해 영화 정보를 수집
- 수집한 데이터를 데이터베이스에 저장
- 배치 작업을 통해 주기적으로 데이터 업데이트

## 기술 스택

- **언어**: Java
- **프레임워크**: Spring Boot
- **데이터베이스**: MySQL
- **API**: 영화진흥위원회 API, KMDB API
- **빌드 도구**: Maven
- **기타**: Lombok, Spring Batch
