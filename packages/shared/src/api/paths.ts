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
