export type PostMutationInput = {
  connectionId: string;
  postId: string;
};

export type ConnectionMutationInput = {
  connectionId: string;
};

function validId(value: unknown): value is string {
  return typeof value === "string" && value.length > 0 &&
    value.length <= 128 && !value.includes("/");
}

export function parsePostMutationInput(value: unknown): PostMutationInput | null {
  if (value == null || typeof value !== "object") return null;
  const data = value as Record<string, unknown>;
  if (!validId(data.connectionId) || !validId(data.postId)) return null;
  return {connectionId: data.connectionId, postId: data.postId};
}

export function parseConnectionMutationInput(value: unknown): ConnectionMutationInput | null {
  if (value == null || typeof value !== "object") return null;
  const data = value as Record<string, unknown>;
  if (!validId(data.connectionId)) return null;
  return {connectionId: data.connectionId};
}
