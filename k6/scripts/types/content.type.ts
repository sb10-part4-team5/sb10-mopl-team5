import { CursorResponse } from './global.type.ts';

export { CursorResponse };

// ContentType.java 의 @JsonValue 값
export type ContentType = 'movie' | 'tvSeries' | 'sport';

// ContentResponse.java
export interface ContentResponse {
  id: string;
  type: ContentType;
  title: string;
  description: string;
  thumbnailUrl: string | null;
  tags: string[];
  averageRating: number;
  reviewCount: number;
  watcherCount: number;
}
