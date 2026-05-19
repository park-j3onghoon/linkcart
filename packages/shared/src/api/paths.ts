/**
 * 백엔드 API 엔드포인트 경로를 한 곳에서 관리한다.
 * 경로가 변경되면(예: AIP 컬렉션 이름 변경) 이 파일만 갱신하면 된다.
 *
 * 동적 ID가 들어가는 경로는 함수로 노출한다.
 */
export const API_PATHS = {
  productsParse: "/api/v1/products:parse",
  imagesProxy: "/api/v1/images:proxy",
  shareListsLookup: "/api/v1/shareLists:lookup",
  authOAuthGoogle: "/api/v1/auth/oauth/google",
  authTokensRefresh: "/api/v1/auth/tokens:refresh",
  authTokensRevoke: "/api/v1/auth/tokens:revoke",
  usersMe: "/api/v1/users/me",
  userProducts: "/api/v1/users/me/products",
} as const;

export function userProductByIdPath(id: string | number): string {
  return `${API_PATHS.userProducts}/${id}`;
}
