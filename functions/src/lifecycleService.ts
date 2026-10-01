import {FieldPath, Firestore, Timestamp} from "firebase-admin/firestore";
import {ConnectionMutationInput, PostMutationInput} from "./lifecycleCore";
import {
  ConnectionStatus,
  ConnectionType,
  MemberStatus,
  PostStatus,
} from "./schema";

export type DeletePostResult =
  | {ok: true; deletedAtMillis: number; updatedAtMillis: number}
  | {ok: false; reason: "NOT_ACTIVE_MEMBER" | "NOT_AUTHOR" | "POST_NOT_FOUND"};

export type DisconnectResult =
  | {ok: true; updatedAtMillis: number}
  | {ok: false; reason: "NOT_ACTIVE_MEMBER" | "NOT_DIRECT" | "CONNECTION_NOT_FOUND"};

export async function softDeletePost(
  db: Firestore,
  uid: string,
  input: PostMutationInput,
  now = Timestamp.now(),
): Promise<DeletePostResult> {
  const connectionRef = db.collection("connections").doc(input.connectionId);
  const memberRef = connectionRef.collection("members").doc(uid);
  const postRef = connectionRef.collection("posts").doc(input.postId);

  return db.runTransaction(async (transaction): Promise<DeletePostResult> => {
    const [connection, member, post] = await transaction.getAll(connectionRef, memberRef, postRef);
    const memberIds = connection.get("memberIds");
    const active = connection.exists && connection.get("status") === ConnectionStatus.ACTIVE &&
      Array.isArray(memberIds) && memberIds.includes(uid) &&
      member.exists && member.get("status") === MemberStatus.ACTIVE;
    if (!active) return {ok: false, reason: "NOT_ACTIVE_MEMBER"};
    if (!post.exists) return {ok: false, reason: "POST_NOT_FOUND"};
    if (post.get("authorId") !== uid) return {ok: false, reason: "NOT_AUTHOR"};

    if (post.get("status") === PostStatus.DELETED && post.get("deletedBy") === uid) {
      const deletedAt = post.get("deletedAt") as Timestamp;
      const updatedAt = post.get("updatedAt") as Timestamp;
      return {ok: true, deletedAtMillis: deletedAt.toMillis(), updatedAtMillis: updatedAt.toMillis()};
    }

    transaction.update(postRef, {
      status: PostStatus.DELETED,
      deletedAt: now,
      deletedBy: uid,
      updatedAt: now,
    });
    return {ok: true, deletedAtMillis: now.toMillis(), updatedAtMillis: now.toMillis()};
  });
}

export async function disconnectDirect(
  db: Firestore,
  uid: string,
  input: ConnectionMutationInput,
  now = Timestamp.now(),
): Promise<DisconnectResult> {
  const connectionRef = db.collection("connections").doc(input.connectionId);

  const result = await db.runTransaction(async (transaction): Promise<DisconnectResult> => {
    const connection = await transaction.get(connectionRef);
    if (!connection.exists) return {ok: false, reason: "CONNECTION_NOT_FOUND"};
    if (connection.get("type") !== ConnectionType.DIRECT) return {ok: false, reason: "NOT_DIRECT"};

    const memberIds = connection.get("memberIds");
    const directMemberIds = Array.isArray(memberIds) ? memberIds.filter(
      (memberId): memberId is string => typeof memberId === "string",
    ) : [];
    const callerRef = connectionRef.collection("members").doc(uid);
    const caller = await transaction.get(callerRef);

    if (connection.get("status") === ConnectionStatus.CLOSED &&
        caller.exists && caller.get("status") === MemberStatus.LEFT) {
      const updatedAt = connection.get("updatedAt") as Timestamp;
      return {ok: true, updatedAtMillis: updatedAt.toMillis()};
    }

    const active = connection.get("status") === ConnectionStatus.ACTIVE &&
      directMemberIds.length === 2 && new Set(directMemberIds).size === 2 && directMemberIds.includes(uid) &&
      caller.exists && caller.get("status") === MemberStatus.ACTIVE;
    if (!active) return {ok: false, reason: "NOT_ACTIVE_MEMBER"};

    const memberRefs = directMemberIds.map((memberId) =>
      connectionRef.collection("members").doc(memberId));
    const memberSnapshots = await transaction.getAll(...memberRefs);
    if (memberSnapshots.some((member) =>
      !member.exists || member.get("status") !== MemberStatus.ACTIVE)) {
      return {ok: false, reason: "NOT_ACTIVE_MEMBER"};
    }

    transaction.update(connectionRef, {
      status: ConnectionStatus.CLOSED,
      memberIds: [],
      updatedAt: now,
    });
    memberRefs.forEach((memberRef) => transaction.update(memberRef, {
      status: MemberStatus.LEFT,
      leftAt: now,
      removedBy: null,
    }));

    const directKey = connection.get("directKey");
    if (typeof directKey === "string" && directKey.length > 0) {
      transaction.set(db.collection("directConnectionLocks").doc(directKey), {
        status: ConnectionStatus.CLOSED,
        updatedAt: now,
      }, {merge: true});
    }
    return {ok: true, updatedAtMillis: now.toMillis()};
  });

  // The post audience is also the collection-group feed index. Clear it after
  // closing the connection so disconnected users stop matching feed queries.
  // Paginated batches avoid Firestore's per-batch write ceiling and make a
  // retry safe if a previous cleanup stopped partway through.
  if (result.ok) await revokePostAudience(db, input.connectionId);
  return result;
}

async function revokePostAudience(db: Firestore, connectionId: string): Promise<void> {
  const posts = db.collection("connections").doc(connectionId).collection("posts");
  let cursor: FirebaseFirestore.QueryDocumentSnapshot | undefined;

  while (true) {
    let query = posts.orderBy(FieldPath.documentId()).limit(400);
    if (cursor) query = query.startAfter(cursor);
    const page = await query.get();
    if (page.empty) return;

    const batch = db.batch();
    page.docs.forEach((post) => batch.update(post.ref, {memberIds: []}));
    await batch.commit();
    cursor = page.docs.at(-1);
    if (page.size < 400) return;
  }
}
