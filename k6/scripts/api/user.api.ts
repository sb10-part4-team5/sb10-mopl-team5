import http, { StructuredRequestBody } from 'k6/http';
import config from '../config.ts';
import { patchMultipart } from '../utils/http-client.ts';
import { UserResponse, UserUpdateRequest } from '../types/user.type.ts';

export interface ProfileImage {
  data: ArrayBuffer | string;
  filename: string;
  contentType: string;
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
