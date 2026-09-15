export const MAX_PHOTO_BYTES = 5 * 1024 * 1024;
export const MAX_PHOTO_EDGE = 1920;
export const MAX_CAPTION_LENGTH = 1000;

export type FinalizePhotoInput = {
  connectionId: string;
  postId: string;
  mediaId: string;
  clientCreatedAt: number;
  caption: string | null;
  storagePath: string;
  mimeType: "image/jpeg";
  width: number;
  height: number;
  sizeBytes: number;
};

function validId(value: unknown): value is string {
  return typeof value === "string" && value.length > 0 &&
    value.length <= 128 && !value.includes("/");
}

export function parseFinalizePhotoInput(value: unknown): FinalizePhotoInput | null {
  if (value == null || typeof value !== "object") return null;
  const data = value as Record<string, unknown>;
  if (!validId(data.connectionId) || !validId(data.postId) || !validId(data.mediaId)) return null;
  const expectedPath = `connections/${data.connectionId}/posts/${data.postId}/${data.mediaId}.jpg`;
  const caption = data.caption == null ? null : data.caption;
  if (caption !== null && (typeof caption !== "string" || caption.length > MAX_CAPTION_LENGTH)) return null;
  if (data.storagePath !== expectedPath || data.mimeType !== "image/jpeg") return null;
  if (!Number.isInteger(data.width) || !Number.isInteger(data.height) ||
      Number(data.width) <= 0 || Number(data.height) <= 0 ||
      Number(data.width) > MAX_PHOTO_EDGE || Number(data.height) > MAX_PHOTO_EDGE) return null;
  if (!Number.isInteger(data.sizeBytes) || Number(data.sizeBytes) <= 0 ||
      Number(data.sizeBytes) > MAX_PHOTO_BYTES) return null;
  if (!Number.isInteger(data.clientCreatedAt) || Number(data.clientCreatedAt) <= 0) return null;
  return {
    connectionId: data.connectionId,
    postId: data.postId,
    mediaId: data.mediaId,
    clientCreatedAt: Number(data.clientCreatedAt),
    caption,
    storagePath: data.storagePath,
    mimeType: "image/jpeg",
    width: Number(data.width),
    height: Number(data.height),
    sizeBytes: Number(data.sizeBytes),
  };
}
