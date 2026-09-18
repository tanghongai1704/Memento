const assert = require("node:assert/strict");
const {initializeApp} = require("firebase-admin/app");
const {getFirestore, Timestamp} = require("firebase-admin/firestore");
const {
  getMyInviteCode,
  redeemDirectInvite,
} = require("../lib/inviteService");
const {finalizePhotoPost} = require("../lib/postService");
const {disconnectDirect, softDeletePost} = require("../lib/lifecycleService");

initializeApp({projectId: "demo-memento-schema"});
const db = getFirestore();

async function main() {
  const testUids = [
    "creator-a", "redeemer-b", "redeemer-c", "creator-d", "redeemer-e",
  ];
  const seed = db.batch();
  testUids.forEach((uid) => seed.set(db.collection("users").doc(uid), {displayName: uid}));
  await seed.commit();

  const created = await getMyInviteCode(db, "creator-a");
  assert.match(created.code, /^[A-HJ-NP-Z2-9]{4}-[A-HJ-NP-Z2-9]{4}$/);
  assert.deepEqual(await getMyInviteCode(db, "creator-a"), created);
  assert.equal((await redeemDirectInvite(db, "creator-a", created.code)).reason, "SELF_REDEEM");

  const [first, second] = await Promise.all([
    redeemDirectInvite(db, "redeemer-b", created.code),
    redeemDirectInvite(db, "redeemer-c", created.code),
  ]);
  assert.equal(first.ok, true);
  assert.equal(second.ok, true);
  const connections = await db.collection("connections").get();
  assert.equal(connections.size, 2);
  const connection = connections.docs.find((doc) => doc.get("memberIds").includes("redeemer-b"));
  assert.ok(connection);
  assert.equal(connection.get("memberIds").length, 2);
  assert.equal((await connection.ref.collection("members").get()).size, 2);
  const repeated = await redeemDirectInvite(db, "redeemer-b", created.code);
  assert.equal(repeated.ok, true);
  assert.equal(repeated.connectionId, connection.id);
  assert.equal((await db.collection("connections").get()).size, 2);
  assert.equal((await redeemDirectInvite(db, "redeemer-e", "AAAA-AAAA")).reason, "INVALID_INVITE");
  const photoInput = {
    connectionId: connection.id, postId: "post-photo",
    clientCreatedAt: 123456789, caption: "MVP photo", layoutType: "SINGLE",
    mediaItems: [{
      mediaId: "media-photo",
      storagePath: `connections/${connection.id}/posts/post-photo/media-photo.jpg`,
      mimeType: "image/jpeg", width: 1920, height: 1080, sizeBytes: 2048, position: 0,
    }],
  };
  const published = await finalizePhotoPost(db, "creator-a", photoInput, async () => true);
  assert.equal(published.ok, true);
  const post = await connection.ref.collection("posts").doc("post-photo").get();
  assert.equal(post.get("authorId"), "creator-a");
  assert.equal(post.get("mediaItems").length, 1);
  assert.equal(post.get("clientCreatedAt"), 123456789);
  assert.equal((await connection.ref.get()).get("lastPostAt") instanceof Timestamp, true);
  const retried = await finalizePhotoPost(db, "creator-a", photoInput, async () => true);
  assert.deepEqual(retried, published);
  const conflictingRetry = await finalizePhotoPost(
    db, "creator-a", {...photoInput, caption: "Changed after publish"}, async () => true,
  );
  assert.equal(conflictingRetry.reason, "POST_CONFLICT");
  const rejectedMember = await finalizePhotoPost(
    db, "redeemer-e", {...photoInput, postId: "not-member"}, async () => true,
  );
  assert.equal(rejectedMember.reason, "NOT_ACTIVE_MEMBER");
  const rejectedMedia = await finalizePhotoPost(
    db, "creator-a", {...photoInput, postId: "bad-media"}, async () => false,
  );
  assert.equal(rejectedMedia.reason, "MEDIA_INVALID");
  const rejectedDelete = await softDeletePost(db, "redeemer-b", {
    connectionId: connection.id, postId: "post-photo",
  });
  assert.equal(rejectedDelete.reason, "NOT_AUTHOR");
  const deleted = await softDeletePost(db, "creator-a", {
    connectionId: connection.id, postId: "post-photo",
  });
  assert.equal(deleted.ok, true);
  assert.deepEqual(await softDeletePost(db, "creator-a", {
    connectionId: connection.id, postId: "post-photo",
  }), deleted);
  assert.equal((await connection.ref.collection("posts").doc("post-photo").get()).get("status"), "DELETED");
  assert.equal((await disconnectDirect(db, "redeemer-e", {connectionId: connection.id})).reason,
    "NOT_ACTIVE_MEMBER");
  const disconnected = await disconnectDirect(db, "redeemer-b", {connectionId: connection.id});
  assert.equal(disconnected.ok, true);
  const closed = await connection.ref.get();
  assert.equal(closed.get("status"), "CLOSED");
  assert.deepEqual(closed.get("memberIds"), []);
  const closedMembers = await connection.ref.collection("members").get();
  closedMembers.docs.forEach((member) => assert.equal(member.get("status"), "LEFT"));
  assert.equal((await disconnectDirect(db, "creator-a", {connectionId: connection.id})).ok, true);
  const rejectedAfterDisconnect = await finalizePhotoPost(
    db, "creator-a", {...photoInput, postId: "after-disconnect"}, async () => true,
  );
  assert.equal(rejectedAfterDisconnect.reason, "NOT_ACTIVE_MEMBER");
  const reconnected = await redeemDirectInvite(db, "redeemer-b", created.code);
  assert.equal(reconnected.ok, true);
  assert.notEqual(reconnected.connectionId, connection.id);
  console.log("Permanent invite code checks passed: stable code, reusable code, self guard, invalid code, idempotent pair lock.");
  console.log("Photo finalize emulator checks passed: membership, media verification, atomic metadata, idempotent retry.");
  console.log("Lifecycle checks passed: author-only soft delete, disconnect revocation, idempotency and reconnect isolation.");
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
