import http from 'k6/http';
import type { StructuredRequestBody } from 'k6/http';

import config from '../config.ts';
import type { CursorResponse } from '../types/global.type.ts';
import type {
  UserResponse,
  UserUpdateRequest,
} from '../types/user.type.ts';
import {
  get,
  patchMultipart,
} from '../utils/http-client.ts';

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

  if (params.emailLike) {
    parts.push(`emailLike=${encodeURIComponent(params.emailLike)}`);
  }

  return parts.join('&');
}

export function getUser(
    token: string,
    userId: string,
    tag: string = config.tags.user.detail,
): UserResponse | null {
  const url = config.endpoints.user.detail.replace(
      '{userId}',
      userId,
  );

  return get<UserResponse>(url, {
    token,
    tag,
  });
}

export function getUsers(
    token: string,
    params: UserListParams,
    tag: string,
): CursorResponse<UserResponse> | null {
  const url = `${config.endpoints.user.list}?${buildQuery(params)}`;

  return get<CursorResponse<UserResponse>>(url, {
    token,
    tag,
  });
}

// PATCH /api/users/{userId} — multipart/form-data
// request는 JSON 파트이며 image는 선택적인 파일 파트다.
// request도 http.file()로 감싸야 k6가 multipart/form-data로 인코딩하고
// 각 파트의 Content-Type을 지정한다.
export function updateProfile(
    token: string,
    userId: string,
    request: UserUpdateRequest,
    image?: ProfileImage,
): UserResponse | null {
  const fields: StructuredRequestBody = {
    request: http.file(
        JSON.stringify(request),
        '',
        'application/json',
    ),
  };

  if (image) {
    fields.image = http.file(
        image.data,
        image.filename,
        image.contentType,
    );
  }

  const url = config.endpoints.user.detail.replace(
      '{userId}',
      userId,
  );

  return patchMultipart<UserResponse>(url, fields, {
    token,
    tag: config.tags.user.update,
  });
}
