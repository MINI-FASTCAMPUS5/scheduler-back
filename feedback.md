# scheduler-back
패스트캠퍼스 미니프로젝트 12조, 연차/당직 프로그램 만들기

### 길민준
1. 현재 저희 조 예외처리는 HTTP STATUS CODE로 분류해 처리하고 있습니다.<br>
다른 조희 경우 각 도메인에 따른 예외로 처리하고 있어서 간결하고 좋아보이는데요. (2조)<br>
HTTP 상태코드로 예외를 잡기엔 그 범위가 넓어서 한도 끝도 없을 것 같아서,
어떨 때에 이런 방법을 쓰는지 궁금합니다.
- /com/fastcampus/minischeduler/core/advice/, ~/exception/

3. 과제 내용 중 양방향암호화(AES256 사용)가 있는데 이런식으로 적용하는 게 맞는지<br>
(BCryptPasswordEncoder 사용한 것처럼) 또 다른 방법이 있는지 궁금합니다.
- SchedulerAdminService.getAdminScheduleDetail()

4. 프론트 팀과 이야기 중에 API를 순수하게 만들어서<br>
클라이언트에서 하나의 요청에 필요한 여러 API를 호출하는 방식으로도 사용할 수 있다고 들었습니다.<br>
그럼 실제로도 단순하게 엔터티 자체를 리턴하는 API를 만들어서(필요에 따라 DTO에 데이터를 선택해서 리턴해야겠지만)<br>
사용하는 경우가 많은지 궁금합니다.

5. JPA repository에서 Entity 자체를 리턴하는 메서드가 아닌, DTO에 직접 SELECT 결과를 매핑하고 싶을 때<br>
저희 조의 경우 interface를 사용하였습니다.<br>
이 경우 AES256 인/디코딩 과정에서 setter를 사용할 때 문제가 있어서 결과적으로는 interface DTO로 받은 데이터를 다시<br>
class DTO로 옮겨 담았는데(비효율적인 과정인 것 같습니다.) 혹시 다른 방법이 있을지 궁금합니다.
- SchedulerAdminService.getAdminScheduleDetail()

---
### 강주희
1. SchedulerAdminService와 SchedulerUserService를 좀 더 깔끔하게 짤 방법이 있는지 여쭤보고 싶습니다!

---
### 문준호
1. 멘토님은 db에서 findAll 해서 data를 꺼낸 다음 자바로 파싱해서 dto에 담는지,<br>
아니면 쿼리를 사용해서 바로 dto에 담는지 궁금합니다!<br>
또 이 경우에 속도 차이가 많이 나는지도 질문드리고 싶습니다.