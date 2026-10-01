import {Firestore, Timestamp} from "firebase-admin/firestore";
import {FinalizePhotoInput, PhotoMediaInput} from "./postCore";
import {
  ConnectionStatus,
  MediaType,
  MemberStatus,
  PostStatus,
  PostType,
} from "./schema";

export type MediaVerifier = (input: PhotoMediaInput) => Promise<boolean>;

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
  const verified = await Promise.all(input.mediaItems.map(verifyMedia));
  if (verified.some((valid) => !valid)) return {ok: false, reason: "MEDIA_INVALID"};

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
      const sameMedia = Array.isArray(mediaItems) && mediaItems.length === input.mediaItems.length &&
        input.mediaItems.every((expected, index) => {
          const media = mediaItems[index];
          return media?.mediaId === expected.mediaId &&
            media?.mediaType === MediaType.IMAGE &&
            media?.storagePath === expected.storagePath &&
            media?.thumbnailPath === null &&
            media?.mimeType === expected.mimeType &&
            media?.width === expected.width &&
            media?.height === expected.height &&
            media?.durationMs === null &&
            media?.sizeBytes === expected.sizeBytes &&
            media?.position === expected.position;
        });
      const samePost = existingPost.get("authorId") === uid &&
        existingPost.get("connectionId") === input.connectionId &&
        existingPost.get("postType") === PostType.PHOTO &&
        existingPost.get("layoutType") === input.layoutType &&
        existingPost.get("caption") === input.caption &&
        existingPost.get("clientCreatedAt") === input.clientCreatedAt &&
        existingPost.get("status") === PostStatus.ACTIVE && sameMedia;
      if (!samePost) return {ok: false, reason: "POST_CONFLICT"};
      const createdAt = existingPost.get("createdAt") as Timestamp;
      const updatedAt = existingPost.get("updatedAt") as Timestamp;
      return {ok: true, createdAtMillis: createdAt.toMillis(), updatedAtMillis: updatedAt.toMillis()};
    }

    const memberIds = connection.get("memberIds");
    const active = connection.exists &&
      connection.get("status") === ConnectionStatus.ACTIVE &&
      Array.isArray(memberIds) && memberIds.includes(uid) &&
      member.exists && member.get("status") === MemberStatus.ACTIVE;
    if (!active) return {ok: false, reason: "NOT_ACTIVE_MEMBER"};

    transaction.create(postRef, {
      connectionId: input.connectionId,
      // Keep the post self-contained for collection-group feed queries and
      // downstream Functions. The trusted backend copies this from the
      // connection instead of accepting audience data from the client.
      memberIds,
      authorId: uid,
      postType: PostType.PHOTO,
      layoutType: input.layoutType,
      caption: input.caption,
      mediaItems: input.mediaItems.map((media) => ({
        mediaId: media.mediaId,
        mediaType: MediaType.IMAGE,
        storagePath: media.storagePath,
        thumbnailPath: null,
        mimeType: media.mimeType,
        width: media.width,
        height: media.height,
        durationMs: null,
        sizeBytes: media.sizeBytes,
        position: media.position,
      })),
      clientCreatedAt: input.clientCreatedAt,
      createdAt: now,
      updatedAt: now,
      status: PostStatus.ACTIVE,
      deletedAt: null,
      deletedBy: null,
      schemaVersion: 1,
    });
    transaction.update(connectionRef, {lastPostAt: now, updatedAt: now});
    return {ok: true, createdAtMillis: now.toMillis(), updatedAtMillis: now.toMillis()};
  });
}
