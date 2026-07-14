import http, { StructuredRequestBody } from 'k6/http';
import config from '../config.ts';
import { CursorResponse } from '../types/global.type.ts';
import { patchMultipart } from '../utils/http-client.ts';
import { UserResponse, UserUpdateRequest } from '../types/user.type.ts';
import { get } from '../utils/http-client.ts';

export interface UserListParams {
  limit: number;
  sortBy: string;
  sortDirection: string;
  emailLike?: string;
}

export interface ProfileImage {
  data: ArrayBuffer | string;
  filename: string;
  contentType: string;
}

// k6 런타임 호환성을 위해 기존 content.api.ts와 같이 query string을 직접 구성한다.
function buildQuery(params: UserListParams): string {
  const parts = [
    `limit=${params.limit}`,
    `sortBy=${encodeURIComponent(params.sortBy)}`,
    `sortDirection=${encodeURIComponent(params.sortDirection)}`,
  ];
  if (params.emailLike) parts.push(`emailLike=${encodeURIComponent(params.emailLike)}`);
  return parts.join('&');
}

export function getUsers(
  token: string,
  params: UserListParams,
  tag: string,
): CursorResponse<UserResponse> | null {
  const url = `${config.endpoints.user.list}?${buildQuery(params)}`;
  return get<CursorResponse<UserResponse>>(url, { token, tag });
}

// PATCH /api/users/{userId} — multipart/form-data (request는 JSON 파트, image는 선택 파일)
// request도 http.file()로 감싸야 k6가 multipart로 인코딩하고 Content-Type을 지정해줌
export function updateProfile(
    token: string,
    userId: string,
    request: UserUpdateRequest,
    image?: ProfileImage,
): UserResponse | null {
  const fields: StructuredRequestBody = {
    request: http.file(JSON.stringify(request), '', 'application/json'),
  };
  if (image) {
    fields.image = http.file(image.data, image.filename, image.contentType);
  }

  const url = config.endpoints.user.detail.replace('{userId}', userId);
  return patchMultipart<UserResponse>(url, fields, { token, tag: config.tags.user.update });
}
