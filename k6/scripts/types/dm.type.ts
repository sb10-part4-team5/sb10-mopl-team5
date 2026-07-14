// UserSummaryResponse.java 대응 (Conversation/DirectMessage 응답에 중첩되는 사용자 요약)
export interface UserSummaryResponse {
  userId: string;
  name: string;
  profileImageUrl: string | null;
}

// DirectMessageResponse.java
export interface DirectMessageResponse {
  id: string;
  conversationId: string;
  sender: UserSummaryResponse;
  receiver: UserSummaryResponse;
  content: string;
  createdAt: string;
}

// ConversationResponse.java
export interface ConversationResponse {
  id: string;
  with: UserSummaryResponse;
  latestMessage: DirectMessageResponse | null;
  hasUnread: boolean;
}
