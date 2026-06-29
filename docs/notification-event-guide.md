# 알림 이벤트 발행 가이드

각 도메인에서 알림을 발생시키려면, **이벤트 객체를 생성하고 `ApplicationEventPublisher`로 발행**하면 됩니다.
알림 도메인의 리스너가 이벤트를 자동으로 수신하여 알림을 생성·저장합니다.

> 알림 도메인 코드를 직접 수정할 필요 없이, 아래 가이드대로 이벤트만 발행하면 됩니다.

---

## 전체 흐름

```
[각 도메인 서비스]
   ① 본래 작업 수행 (DM 저장, 팔로우 저장 등)
   ② publisher.publishEvent(이벤트 객체)    ← 트랜잭션 안에서 발행
        │
        ▼  (Spring이 자동 전달)
[NotificationEventListener]  @TransactionalEventListener(AFTER_COMMIT)
   ③ 트랜잭션이 커밋된 후에만 알림 생성·저장
```

---

## 발행 방법 (3단계)

### 1단계: `ApplicationEventPublisher` 주입

서비스 클래스에 `ApplicationEventPublisher`를 주입합니다.
`@RequiredArgsConstructor`를 쓰면 `private final`로 선언만 하면 됩니다.

```java
@Service
@RequiredArgsConstructor
public class FollowService {

    private final FollowRepository followRepository;
    private final ApplicationEventPublisher publisher;   // ← 추가
    
    // ...
}
```

### 2단계: 이벤트 객체 생성

아래 이벤트 레퍼런스 표에서 해당하는 이벤트 record를 `new`로 생성합니다.
단, `DirectMessageSentEvent`와 `FollowingUserWatchingEvent` 처럼 생성자에서 검증하는 타입만 예외가 발생합니다.
나머지 이벤트는 null/blank 허용 여부를 구현과 함께 확인해 주세요.
필수 파라미터를 빠뜨리면 예외가 발생합니다.

```java
UserFollowedEvent event = new UserFollowedEvent(
    targetUserId,    // receiverId: 알림을 받을 사용자의 UUID
    follower.getName()  // userName: 팔로우한 사람의 이름
);
```

### 3단계: 트랜잭션 안에서 발행

**반드시 `@Transactional` 메서드 안에서 발행해야 합니다.**
트랜잭션 밖에서 발행하면 리스너가 동작하지 않습니다.

```java
@Transactional
public void follow(UUID followerId, UUID targetId) {
    // ① 팔로우 저장
    Follow follow = Follow.create(followerId, targetId);
    followRepository.save(follow);

    // ② 이벤트 발행 (트랜잭션 안에서!)
    publisher.publishEvent(new UserFollowedEvent(targetId, follower.getName()));
    
    // 트랜잭션이 커밋되면 → 알림이 자동 생성됩니다.
    // 트랜잭션이 롤백되면 → 알림도 생성되지 않습니다. (안전)
}
```

---

## 이벤트 레퍼런스

### 1. `UserFollowedEvent` — 팔로우

**발행 시점:** 다른 사용자가 나를 팔로우했을 때

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `receiverId` | `UUID` | 알림을 받을 사용자 (팔로우 **당한** 사람) |
| `userName` | `String` | 팔로우 **한** 사람의 이름 |

```java
publisher.publishEvent(new UserFollowedEvent(targetUserId, followerName));
```

생성되는 알림: `"다린님이 나를 팔로우했어요."`

---

### 2. `DirectMessageSentEvent` — DM 전송

**발행 시점:** DM을 전송했을 때

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `receiverId` | `UUID` | 알림을 받을 사용자 (DM **수신자**) |
| `senderNickname` | `String` | DM 보낸 사람의 닉네임 |
| `content` | `String` | DM 메시지 내용 |

```java
publisher.publishEvent(new DirectMessageSentEvent(receiverId, sender.getNickname(), message.getContent()));
```

생성되는 알림: 제목 `"[DM] 다린"`, 내용은 50자로 잘림

> ⚠️ `receiverId`, `senderNickname`, `content` 모두 필수입니다. null 또는 빈 문자열이면 예외가 발생합니다.

---

### 3. `PlaylistSubscribedEvent` — 플레이리스트 구독

**발행 시점:** 다른 사용자가 내 플레이리스트를 구독했을 때

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `receiverId` | `UUID` | 알림을 받을 사용자 (플레이리스트 **소유자**) |
| `subscriberNickname` | `String` | 구독 **한** 사람의 닉네임 |
| `playlistName` | `String` | 구독된 플레이리스트 이름 |

```java
publisher.publishEvent(new PlaylistSubscribedEvent(ownerId, subscriber.getNickname(), playlist.getName()));
```

생성되는 알림: 제목 `"[플레이리스트] 내 플레이리스트"`, 내용 `"다린 님이 플레이리스트를 구독하셨습니다."`

---

### 4. `PlaylistUpdatedEvent` — 구독 플레이리스트 수정

**발행 시점:** 구독 중인 플레이리스트가 수정되었을 때

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `receiverId` | `UUID` | 알림을 받을 사용자 (플레이리스트 **구독자**) |
| `playlistName` | `String` | 수정된 플레이리스트 이름 |

```java
// 구독자가 여러 명이면 각각에 대해 발행
for (UUID subscriberId : subscriberIds) {
    publisher.publishEvent(new PlaylistUpdatedEvent(subscriberId, playlist.getName()));
}
```

생성되는 알림: 제목 `"[플레이리스트] 내 플레이리스트"`, 내용 `"플레이리스트가 업데이트 되었습니다."`

---

### 5. `RoleChangedEvent` — 권한 변경

**발행 시점:** ADMIN이 사용자의 권한을 변경했을 때

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `receiverId` | `UUID` | 알림을 받을 사용자 (권한이 **변경된** 사람) |
| `roleBefore` | `String` | 변경 전 권한 (예: `"USER"`) |
| `roleAfter` | `String` | 변경 후 권한 (예: `"ADMIN"`) |

```java
publisher.publishEvent(new RoleChangedEvent(userId, "USER", "ADMIN"));
```

생성되는 알림: 제목 `"내 권한이 변경되었어요."`, 내용 `"내 권한이 [USER]에서 [ADMIN]로 변경되었어요."`

---

### 6. `FollowingUserWatchingEvent` — 팔로우한 사용자의 시청 활동

**발행 시점:** 내가 팔로우한 사용자가 콘텐츠를 시청할 때 (WatchingSession 생성 시)

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `receiverId` | `UUID` | 알림을 받을 사용자 (시청자를 **팔로우한** 사람) |
| `userNickname` | `String` | 시청 중인 사용자의 닉네임 |
| `contentName` | `String` | 시청 중인 콘텐츠 이름 |

```java
// 시청자의 팔로워 각각에게 발행
for (UUID followerId : followerIds) {
    publisher.publishEvent(new FollowingUserWatchingEvent(followerId, watcher.getNickname(), content.getTitle()));
}
```

생성되는 알림: 제목 `"다린 님이 컨텐츠 시청중입니다."`, 내용 `"콘텐츠A 시청 중"`

> ⚠️ `receiverId`, `userNickname`, `contentName` 모두 필수입니다. null 또는 빈 문자열이면 예외가 발생합니다.

---

## 주의사항

### 1. 반드시 `@Transactional` 안에서 발행

```java
@Transactional   // ← 이게 있어야 리스너가 동작합니다
public void doSomething() {
    // ... 작업 ...
    publisher.publishEvent(event);
}
```

리스너는 `@TransactionalEventListener(phase = AFTER_COMMIT)`로 설정되어 있어서,
**활성 트랜잭션이 없으면 이벤트가 무시됩니다** (알림이 안 만들어짐).

### 2. 트랜잭션 롤백 시 알림은 생성되지 않음 (의도된 동작)

예를 들어 DM 저장이 실패(롤백)하면 알림도 만들어지지 않습니다.
"유령 알림" 방지를 위한 안전장치입니다.

### 3. `receiverId`는 알림을 받을 사람

이벤트의 `receiverId`는 **행동을 한 사람이 아니라, 알림을 받을 대상**입니다.
- 팔로우: receiverId = 팔로우 **당한** 사람 (팔로우 한 사람 ❌)
- DM: receiverId = DM **수신자** (발신자 ❌)
- 플레이리스트 구독: receiverId = 플레이리스트 **소유자** (구독자 ❌)

### 4. import 경로

모든 이벤트는 `com.codeit.team5.mopl.notification.event` 패키지에 있습니다.

```java
import com.codeit.team5.mopl.notification.event.UserFollowedEvent;
import com.codeit.team5.mopl.notification.event.DirectMessageSentEvent;
import com.codeit.team5.mopl.notification.event.PlaylistSubscribedEvent;
import com.codeit.team5.mopl.notification.event.PlaylistUpdatedEvent;
import com.codeit.team5.mopl.notification.event.RoleChangedEvent;
import com.codeit.team5.mopl.notification.event.FollowingUserWatchingEvent;
```

---

## 전체 예시 (팔로우 도메인)

```java
package com.codeit.team5.mopl.follow.service;

import com.codeit.team5.mopl.notification.event.UserFollowedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher publisher;

    @Transactional
    public void follow(UUID followerId, UUID targetId) {
        User follower = userRepository.findById(followerId).orElseThrow();
        User target = userRepository.findById(targetId).orElseThrow();

        Follow follow = Follow.create(follower, target);
        followRepository.save(follow);

        // 팔로우 저장이 커밋되면 알림이 자동 생성됩니다.
        publisher.publishEvent(new UserFollowedEvent(targetId, follower.getName()));
    }
}
```

---

## 문의

알림 이벤트 관련 질문이나 새로운 이벤트 타입 추가가 필요하면 알림 도메인 담당자에게 문의해 주세요.
