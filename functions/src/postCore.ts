export const MAX_PHOTO_BYTES = 5 * 1024 * 1024;
export const MAX_PHOTO_EDGE = 1920;
export const MAX_CAPTION_LENGTH = 1000;
export const MAX_PHOTOS_PER_POST = 5;

export type PhotoMediaInput = {
  mediaId: string;
  storagePath: string;
  mimeType: "image/jpeg";
  width: number;
  height: number;
  sizeBytes: number;
  position: number;
};

export type FinalizePhotoInput = {
  connectionId: string;
  postId: string;
  clientCreatedAt: number;
  caption: string | null;
  layoutType: "SINGLE" | "GRID" | "COLLAGE" | "CAROUSEL";
  mediaItems: PhotoMediaInput[];
};

function validId(value: unknown): value is string {
  return typeof value === "string" && value.length > 0 &&
    value.length <= 128 && !value.includes("/");
}

export function parseFinalizePhotoInput(value: unknown): FinalizePhotoInput | null {
  if (value == null || typeof value !== "object") return null;
  const data = value as Record<string, unknown>;
  if (!validId(data.connectionId) || !validId(data.postId)) return null;
  const caption = data.caption == null ? null : data.caption;
  if (caption !== null && (typeof caption !== "string" || caption.length > MAX_CAPTION_LENGTH)) return null;
  if (!Number.isInteger(data.clientCreatedAt) || Number(data.clientCreatedAt) <= 0) return null;
  const legacyMedia = data.mediaId == null ? null : [{
    mediaId: data.mediaId,
    storagePath: data.storagePath,
    mimeType: data.mimeType,
    width: data.width,
    height: data.height,
    sizeBytes: data.sizeBytes,
    position: 0,
  }];
  const rawMedia = Array.isArray(data.mediaItems) ? data.mediaItems : legacyMedia;
  if (!rawMedia || rawMedia.length < 1 || rawMedia.length > MAX_PHOTOS_PER_POST) return null;
  const mediaItems: PhotoMediaInput[] = [];
  for (let position = 0; position < rawMedia.length; position += 1) {
    const media = rawMedia[position];
    if (media == null || typeof media !== "object") return null;
    const item = media as Record<string, unknown>;
    if (!validId(item.mediaId) || item.position !== position) return null;
    const expectedPath = `connections/${data.connectionId}/posts/${data.postId}/${item.mediaId}.jpg`;
    if (item.storagePath !== expectedPath || item.mimeType !== "image/jpeg") return null;
    if (!Number.isInteger(item.width) || !Number.isInteger(item.height) ||
        Number(item.width) <= 0 || Number(item.height) <= 0 ||
        Number(item.width) > MAX_PHOTO_EDGE || Number(item.height) > MAX_PHOTO_EDGE) return null;
    if (!Number.isInteger(item.sizeBytes) || Number(item.sizeBytes) <= 0 ||
        Number(item.sizeBytes) > MAX_PHOTO_BYTES) return null;
    mediaItems.push({
      mediaId: item.mediaId,
      storagePath: item.storagePath,
      mimeType: "image/jpeg",
      width: Number(item.width),
      height: Number(item.height),
      sizeBytes: Number(item.sizeBytes),
      position,
    });
  }
  if (new Set(mediaItems.map((item) => item.mediaId)).size !== mediaItems.length) return null;
  const layoutType = data.layoutType ?? (mediaItems.length === 1 ? "SINGLE" : "GRID");
  if (!["SINGLE", "GRID", "COLLAGE", "CAROUSEL"].includes(String(layoutType))) return null;
  if ((mediaItems.length === 1) !== (layoutType === "SINGLE")) return null;
  return {
    connectionId: data.connectionId,
    postId: data.postId,
    clientCreatedAt: Number(data.clientCreatedAt),
    caption,
    layoutType: layoutType as FinalizePhotoInput["layoutType"],
    mediaItems,
  };
}
