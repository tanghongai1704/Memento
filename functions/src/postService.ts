import {Firestore, Timestamp} from "firebase-admin/firestore";
import {FinalizePhotoInput} from "./postCore";

export type MediaVerifier = (input: FinalizePhotoInput) => Promise<boolean>;

export type FinalizePhotoResult =
  | {ok: true; createdAtMillis: number; updatedAtMillis: number}
  | {ok: false; reason: "MEDIA_INVALID" | "NOT_ACTIVE_MEMBER" | "POST_CONFLICT"};

export async function finalizePhotoPost(
  db: Firestore,
  uid: string,
  input: FinalizePhotoInput,
  verifyMedia: MediaVerifier,
  now = Timestamp.now(),
): Promise<FinalizePhotoResult> {
  if (!await verifyMedia(input)) return {ok: false, reason: "MEDIA_INVALID"};

  const connectionRef = db.collection("connections").doc(input.connectionId);
  const memberRef = connectionRef.collection("members").doc(uid);
  const postRef = connectionRef.collection("posts").doc(input.postId);

  return db.runTransaction(async (transaction): Promise<FinalizePhotoResult> => {
    const [connection, member, existingPost] = await transaction.getAll(
      connectionRef,
      memberRef,
      postRef,
    );

    if (existingPost.exists) {
      const mediaItems = existingPost.get("mediaItems");
      const media = Array.isArray(mediaItems) && mediaItems.length === 1 ? mediaItems[0] : null;
      const samePost = existingPost.get("authorId") === uid &&
        existingPost.get("connectionId") === input.connectionId &&
        existingPost.get("postType") === "PHOTO" &&
        existingPost.get("layoutType") === "SINGLE" &&
        existingPost.get("caption") === input.caption &&
        existingPost.get("clientCreatedAt") === input.clientCreatedAt &&
        existingPost.get("status") === "ACTIVE" &&
        media?.mediaId === input.mediaId &&
        media?.mediaType === "IMAGE" &&
        media?.storagePath === input.storagePath &&
        media?.thumbnailPath === null &&
        media?.mimeType === input.mimeType &&
        media?.width === input.width &&
        media?.height === input.height &&
        media?.durationMs === null &&
        media?.sizeBytes === input.sizeBytes &&
        media?.position === 0;
      if (!samePost) return {ok: false, reason: "POST_CONFLICT"};
      const createdAt = existingPost.get("createdAt") as Timestamp;
      const updatedAt = existingPost.get("updatedAt") as Timestamp;
      return {ok: true, createdAtMillis: createdAt.toMillis(), updatedAtMillis: updatedAt.toMillis()};
    }

    const memberIds = connection.get("memberIds");
    const active = connection.exists && connection.get("status") === "ACTIVE" &&
      Array.isArray(memberIds) && memberIds.includes(uid) &&
      member.exists && member.get("status") === "ACTIVE";
    if (!active) return {ok: false, reason: "NOT_ACTIVE_MEMBER"};

    transaction.create(postRef, {
      connectionId: input.connectionId,
      authorId: uid,
      postType: "PHOTO",
      layoutType: "SINGLE",
      caption: input.caption,
      mediaItems: [{
        mediaId: input.mediaId,
        mediaType: "IMAGE",
        storagePath: input.storagePath,
        thumbnailPath: null,
        mimeType: input.mimeType,
        width: input.width,
        height: input.height,
        durationMs: null,
        sizeBytes: input.sizeBytes,
        position: 0,
      }],
      clientCreatedAt: input.clientCreatedAt,
      createdAt: now,
      updatedAt: now,
      status: "ACTIVE",
      deletedAt: null,
      deletedBy: null,
      schemaVersion: 1,
    });
    transaction.update(connectionRef, {lastPostAt: now, updatedAt: now});
    return {ok: true, createdAtMillis: now.toMillis(), updatedAtMillis: now.toMillis()};
  });
}
