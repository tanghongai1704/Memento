export const MEDIA_CLEANUP_GRACE_MS = 7 * 24 * 60 * 60 * 1000;

export type StoredMediaPath = {
  connectionId: string;
  postId: string;
  mediaId: string;
};

export function parseStoredMediaPath(path: string): StoredMediaPath | null {
  const match = /^connections\/([A-Za-z0-9_-]{1,128})\/posts\/([A-Za-z0-9_-]{1,128})\/([A-Za-z0-9_-]{1,128})\.jpg$/
    .exec(path);
  if (!match) return null;
  return {connectionId: match[1], postId: match[2], mediaId: match[3]};
}

export function shouldDeleteStoredMedia(input: {
  path: string;
  createdAtMillis: number;
  nowMillis: number;
  postExists: boolean;
  postStatus?: string;
  deletedAtMillis?: number;
  postStoragePaths?: string[];
}): boolean {
  if (!parseStoredMediaPath(input.path)) return false;
  if (input.nowMillis - input.createdAtMillis < MEDIA_CLEANUP_GRACE_MS) return false;
  if (!input.postExists) return true;
  return input.postStatus === "DELETED" &&
    typeof input.deletedAtMillis === "number" &&
    input.nowMillis - input.deletedAtMillis >= MEDIA_CLEANUP_GRACE_MS &&
    input.postStoragePaths?.includes(input.path) === true;
}
