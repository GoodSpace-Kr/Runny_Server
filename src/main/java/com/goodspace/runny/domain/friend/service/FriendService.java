package com.goodspace.runny.domain.friend.service;

import com.goodspace.runny.domain.friend.dto.FriendDto;
import com.goodspace.runny.domain.friend.entity.Friendship;
import com.goodspace.runny.domain.friend.entity.FriendshipStatus;
import com.goodspace.runny.domain.friend.entity.RelationStatus;
import com.goodspace.runny.domain.friend.repository.FriendshipRepository;
import com.goodspace.runny.domain.friend.repository.PlaygroundInviteRepository;
import com.goodspace.runny.domain.dog.repository.UserDogRepository;
import com.goodspace.runny.domain.user.dto.UserSummary;
import com.goodspace.runny.domain.user.entity.User;
import com.goodspace.runny.domain.user.repository.UserRepository;
import com.goodspace.runny.domain.user.service.UserSummaryService;
import com.goodspace.runny.global.exception.BusinessException;
import com.goodspace.runny.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 친구 서비스. 검색(관계 상태 포함), 요청 보내기/취소, 받은 요청 확인/수락/거절, 친구 목록/상세/삭제를 담당한다.
 */
@Service
@RequiredArgsConstructor
public class FriendService {

    private final FriendshipRepository friendshipRepository;
    private final PlaygroundInviteRepository playgroundInviteRepository;
    private final UserRepository userRepository;
    private final UserDogRepository userDogRepository;
    private final UserSummaryService userSummaryService;
    private final FriendNotificationHook friendNotificationHook;

    /** 친구 검색 - 닉네임 부분 일치, 전체 반환(MVP 규모 페이징 제거). 각 결과에 나와의 관계 상태 포함 */
    @Transactional(readOnly = true)
    public FriendDto.SearchResponse search(Long userId, String nickname) {
        List<User> users = userRepository.findByNicknameContainingAndIdNotOrderByIdAsc(nickname, userId);

        Map<Long, RelationStatus> relationByUserId = relationMap(userId);
        Map<Long, Long> pendingRequestIds = pendingRequestIdMap(userId);
        List<Long> resultUserIds = users.stream().map(User::getId).toList();
        Map<Long, UserSummary> summaries = userSummaryService.summarizeAll(resultUserIds);

        List<FriendDto.SearchItem> content = resultUserIds.stream()
                .map(id -> new FriendDto.SearchItem(
                        summaries.get(id),
                        relationByUserId.getOrDefault(id, RelationStatus.NONE),
                        pendingRequestIds.get(id)))
                .toList();
        return new FriendDto.SearchResponse(content);
    }

    /** 친구 요청 보내기 - 자기 자신/중복/이미 친구 검증. 동시 요청은 UNIQUE 제약 위반을 비즈니스 예외로 변환 */
    @Transactional
    public void sendRequest(Long userId, Long targetUserId) {
        if (userId.equals(targetUserId)) {
            throw new BusinessException(ErrorCode.FRIEND_001);
        }
        if (!userRepository.existsById(targetUserId)) {
            throw new BusinessException(ErrorCode.USER_003);
        }
        friendshipRepository.findBetween(Math.min(userId, targetUserId), Math.max(userId, targetUserId))
                .ifPresent(existing -> {
                    if (existing.getStatus() == FriendshipStatus.ACCEPTED) {
                        throw new BusinessException(ErrorCode.FRIEND_003);
                    }
                    throw new BusinessException(ErrorCode.FRIEND_002);
                });
        try {
            friendshipRepository.saveAndFlush(new Friendship(userId, targetUserId));
        } catch (DataIntegrityViolationException e) {
            // A->B, B->A 동시 요청 경쟁 - (user_low_id, user_high_id) UNIQUE 위반을 잡아 변환
            throw new BusinessException(ErrorCode.FRIEND_002);
        }
        friendNotificationHook.onFriendRequestReceived(targetUserId, userId);
    }

    /** 보낸 요청 취소 - 요청자 본인 + PENDING 상태만 */
    @Transactional
    public void cancelRequest(Long userId, Long requestId) {
        Friendship friendship = findPendingRequest(requestId);
        if (!friendship.getRequesterId().equals(userId)) {
            throw new BusinessException(ErrorCode.FRIEND_006);
        }
        friendshipRepository.delete(friendship);
    }

    /** 보낸 요청 목록 (PENDING) */
    @Transactional(readOnly = true)
    public List<FriendDto.RequestItem> getSentRequests(Long userId) {
        return toRequestItems(
                friendshipRepository.findByRequesterIdAndStatusOrderByIdDesc(userId, FriendshipStatus.PENDING),
                userId);
    }

    /** 받은 요청 목록 (PENDING) - 미확인 여부를 함께 반환하고 조회 시점에 일괄 확인 처리 */
    @Transactional
    public FriendDto.ReceivedRequests getReceivedRequests(Long userId) {
        boolean hadUnchecked = friendshipRepository
                .existsByReceiverIdAndStatusAndCheckedFalse(userId, FriendshipStatus.PENDING);
        List<FriendDto.RequestItem> items = toRequestItems(
                friendshipRepository.findByReceiverIdAndStatusOrderByIdDesc(userId, FriendshipStatus.PENDING),
                userId);
        friendshipRepository.markReceivedChecked(userId);
        return new FriendDto.ReceivedRequests(hadUnchecked, items);
    }

    /** 받은 요청 미확인 존재 여부 - 친구 목록 버튼 빨간 점용 */
    @Transactional(readOnly = true)
    public boolean hasUncheckedReceivedRequests(Long userId) {
        return friendshipRepository.existsByReceiverIdAndStatusAndCheckedFalse(userId, FriendshipStatus.PENDING);
    }

    /** 요청 수락 - 수신자 본인만. 수락 시 업적/알림 훅 호출 */
    @Transactional
    public void acceptRequest(Long userId, Long requestId) {
        Friendship friendship = findPendingRequest(requestId);
        if (!friendship.getReceiverId().equals(userId)) {
            throw new BusinessException(ErrorCode.FRIEND_006);
        }
        friendship.accept();
        friendNotificationHook.onFriendAccepted(friendship.getRequesterId(), friendship.getReceiverId());
    }

    /** 요청 거절 - 행 삭제로 처리해 상대가 재요청할 수 있게 한다 */
    @Transactional
    public void rejectRequest(Long userId, Long requestId) {
        Friendship friendship = findPendingRequest(requestId);
        if (!friendship.getReceiverId().equals(userId)) {
            throw new BusinessException(ErrorCode.FRIEND_006);
        }
        friendshipRepository.delete(friendship);
    }

    /**
     * 친구 목록 - 놀이터 초대된(같이 놀고 있는) 친구 최상단 + isPlayingTogether 플래그.
     * 친구 요청 탭 배지 숫자용으로 미확인 받은 요청 수를 함께 반환하며, 여기서는 읽음 처리하지 않는다.
     */
    @Transactional(readOnly = true)
    public FriendDto.FriendListResponse getFriends(Long userId) {
        List<Long> friendIds = friendshipRepository.findAcceptedOf(userId).stream()
                .map(f -> f.otherUserId(userId))
                .toList();
        List<Long> invitedIds = playgroundInviteRepository.findByOwnerIdOrderByIdAsc(userId).stream()
                .map(invite -> invite.getFriendUserId())
                .toList();

        Map<Long, UserSummary> summaries = userSummaryService.summarizeAll(friendIds);
        List<FriendDto.FriendItem> friends = friendIds.stream()
                .filter(summaries::containsKey)
                .map(id -> new FriendDto.FriendItem(summaries.get(id), invitedIds.contains(id)))
                .sorted((a, b) -> Boolean.compare(b.isPlayingTogether(), a.isPlayingTogether()))
                .toList();
        int uncheckedRequestCount = friendshipRepository
                .countByReceiverIdAndStatusAndCheckedFalse(userId, FriendshipStatus.PENDING);
        return new FriendDto.FriendListResponse(friends, uncheckedRequestCount);
    }

    /**
     * 유저 프로필 상세 - 친구가 아니어도 조회 가능하다(검색 결과에서 프로필 진입).
     * 관계 상태와 PENDING 요청 ID를 함께 내려 프론트가 버튼(친구 추가/신청 취소/놀이터 초대)을 분기한다.
     */
    @Transactional(readOnly = true)
    public FriendDto.FriendDetail getFriendDetail(Long userId, Long friendUserId) {
        UserSummary summary = userSummaryService.summarize(friendUserId);
        if (summary == null) {
            throw new BusinessException(ErrorCode.USER_003);
        }
        // 나와의 관계 단건 조회 (방향 무관) - 관계 상태와 PENDING 요청 ID를 함께 도출
        Friendship relation = friendshipRepository
                .findBetween(Math.min(userId, friendUserId), Math.max(userId, friendUserId))
                .orElse(null);
        RelationStatus relationStatus = toRelationStatus(relation, userId);
        Long requestId = (relation != null && relation.getStatus() == FriendshipStatus.PENDING)
                ? relation.getId() : null;
        // 활성 강아지 스탯 조회 (요약에는 외형만 있으므로 별도 조회)
        if (summary.dog() == null) {
            return new FriendDto.FriendDetail(summary, 0, 0, 0, relationStatus, requestId);
        }
        return userDogRepository.findById(summary.dog().dogId())
                .map(dog -> new FriendDto.FriendDetail(summary,
                        dog.getStamina(), dog.getEndurance(), dog.getSpeed(), relationStatus, requestId))
                .orElse(new FriendDto.FriendDetail(summary, 0, 0, 0, relationStatus, requestId));
    }

    /** 관계 엔티티를 나 기준 관계 상태로 변환 (없으면 NONE) */
    private RelationStatus toRelationStatus(Friendship relation, Long userId) {
        if (relation == null) {
            return RelationStatus.NONE;
        }
        if (relation.getStatus() == FriendshipStatus.ACCEPTED) {
            return RelationStatus.FRIEND;
        }
        return relation.getRequesterId().equals(userId) ? RelationStatus.REQUESTED : RelationStatus.RECEIVED;
    }

    /** 나와 PENDING 상태인 요청의 ID 맵 (상대 유저 ID -> requestId) - 검색 결과에서 즉시 취소·수락용 */
    private Map<Long, Long> pendingRequestIdMap(Long userId) {
        Map<Long, Long> map = new HashMap<>();
        friendshipRepository.findAllInvolving(userId).stream()
                .filter(friendship -> friendship.getStatus() == FriendshipStatus.PENDING)
                .forEach(friendship -> map.put(friendship.otherUserId(userId), friendship.getId()));
        return map;
    }

    /** 친구 삭제 - 관계 삭제 + 서로의 놀이터 초대도 함께 제거 */
    @Transactional
    public void deleteFriend(Long userId, Long friendUserId) {
        Friendship friendship = friendshipRepository
                .findBetween(Math.min(userId, friendUserId), Math.max(userId, friendUserId))
                .filter(f -> f.getStatus() == FriendshipStatus.ACCEPTED)
                .orElseThrow(() -> new BusinessException(ErrorCode.FRIEND_004));
        friendshipRepository.delete(friendship);
        playgroundInviteRepository.deleteBetween(userId, friendUserId);
    }

    /** 회원 탈퇴 시 상호작용 데이터 정리 - 친구 관계/요청/놀이터 초대 전부 삭제 (UserService에서 호출) */
    @Transactional
    public void deleteAllInteractionsOf(Long userId) {
        friendshipRepository.deleteAllInvolving(userId);
        playgroundInviteRepository.deleteAllInvolving(userId);
    }

    /** 두 유저가 친구인지 검증 */
    public void assertFriends(Long userId, Long otherUserId) {
        boolean friends = friendshipRepository
                .findBetween(Math.min(userId, otherUserId), Math.max(userId, otherUserId))
                .map(f -> f.getStatus() == FriendshipStatus.ACCEPTED)
                .orElse(false);
        if (!friends) {
            throw new BusinessException(ErrorCode.FRIEND_004);
        }
    }

    /** 나와 관련된 관계를 상대 유저 ID -> 관계 상태 맵으로 변환 (검색용) */
    private Map<Long, RelationStatus> relationMap(Long userId) {
        Map<Long, RelationStatus> map = new HashMap<>();
        friendshipRepository.findAllInvolving(userId).forEach(f -> {
            Long otherId = f.otherUserId(userId);
            if (f.getStatus() == FriendshipStatus.ACCEPTED) {
                map.put(otherId, RelationStatus.FRIEND);
            } else if (f.getRequesterId().equals(userId)) {
                map.put(otherId, RelationStatus.REQUESTED);
            } else {
                map.put(otherId, RelationStatus.RECEIVED);
            }
        });
        return map;
    }

    /** 요청 목록 변환 공통 - 상대 유저 요약 포함 */
    private List<FriendDto.RequestItem> toRequestItems(List<Friendship> requests, Long myUserId) {
        List<Long> otherIds = requests.stream().map(f -> f.otherUserId(myUserId)).toList();
        Map<Long, UserSummary> summaries = userSummaryService.summarizeAll(otherIds);
        return requests.stream()
                .filter(f -> summaries.containsKey(f.otherUserId(myUserId)))
                .map(f -> new FriendDto.RequestItem(f.getId(), summaries.get(f.otherUserId(myUserId))))
                .toList();
    }

    /** PENDING 요청 조회 공통 - 없으면 FRIEND_006, 이미 처리되었으면 FRIEND_007 */
    private Friendship findPendingRequest(Long requestId) {
        Friendship friendship = friendshipRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FRIEND_006));
        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new BusinessException(ErrorCode.FRIEND_007);
        }
        return friendship;
    }

}
