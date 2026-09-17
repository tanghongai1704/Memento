import type {Firestore} from "firebase-admin/firestore";
import {Bucket} from "@google-cloud/storage";
import {parseStoredMediaPath, shouldDeleteStoredMedia} from "./cleanupCore";

export type CleanupResult = {scanned: number; deleted: number; skipped: number};

function timestampMillis(value: unknown): number | undefined {
  if (typeof value !== "object" || value === null || !("toMillis" in value)) return undefined;
  const toMillis = (value as {toMillis?: unknown}).toMillis;
  if (typeof toMillis !== "function") return undefined;
  const millis = toMillis.call(value);
  return typeof millis === "number" && Number.isFinite(millis) ? millis : undefined;
}

export async function cleanupExpiredMedia(
  db: Firestore,
  bucket: Bucket,
  nowMillis = Date.now(),
): Promise<CleanupResult> {
  const [files] = await bucket.getFiles({prefix: "connections/"});
  let deleted = 0;
  let skipped = 0;

  for (const file of files) {
    const parsed = parseStoredMediaPath(file.name);
    const createdAtMillis = Date.parse(file.metadata.timeCreated ?? "");
    if (!parsed || !Number.isFinite(createdAtMillis)) {
      skipped += 1;
      continue;
    }
    const post = await db.collection("connections").doc(parsed.connectionId)
      .collection("posts").doc(parsed.postId).get();
    const mediaItems = post.exists && Array.isArray(post.get("mediaItems")) ? post.get("mediaItems") : [];
    const deletedAt = post.exists ? post.get("deletedAt") : null;
    const removable = shouldDeleteStoredMedia({
      path: file.name,
      createdAtMillis,
      nowMillis,
      postExists: post.exists,
      postStatus: post.exists ? post.get("status") : undefined,
      deletedAtMillis: timestampMillis(deletedAt),
      postStoragePaths: mediaItems.map((media: unknown) =>
        typeof media === "object" && media !== null && "storagePath" in media ?
          (media as {storagePath?: unknown}).storagePath : undefined,
      ).filter((path: unknown): path is string => typeof path === "string"),
    });
    if (!removable) {
      skipped += 1;
      continue;
    }
    await file.delete({ignoreNotFound: true});
    deleted += 1;
  }
  return {scanned: files.length, deleted, skipped};
}
