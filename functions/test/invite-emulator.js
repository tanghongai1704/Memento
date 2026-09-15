const assert = require("node:assert/strict");
const {initializeApp} = require("firebase-admin/app");
const {getFirestore, Timestamp} = require("firebase-admin/firestore");
const {
  createDirectInvite,
  redeemDirectInvite,
  revokeDirectInvite,
} = require("../lib/inviteService");
const {finalizePhotoPost} = require("../lib/postService");

initializeApp({projectId: "demo-memento-schema"});
const db = getFirestore();

async function main() {
  const testUids = [
    "creator-a", "redeemer-b", "redeemer-c", "creator-d", "redeemer-e",
    "creator-expired", "redeemer-expired", "rate-user",
  ];
  const seed = db.batch();
  testUids.forEach((uid) => seed.set(db.collection("users").doc(uid), {displayName: uid}));
  await seed.commit();

  const created = await createDirectInvite(db, "creator-a");
  assert.match(created.code, /^[A-HJ-NP-Z2-9]{4}-[A-HJ-NP-Z2-9]{4}$/);
  assert.equal((await redeemDirectInvite(db, "creator-a", created.code)).reason, "SELF_REDEEM");

  const [first, second] = await Promise.all([
    redeemDirectInvite(db, "redeemer-b", created.code),
    redeemDirectInvite(db, "redeemer-c", created.code),
  ]);
  assert.equal([first, second].filter((result) => result.ok).length, 1);
  const connections = await db.collection("connections").get();
  assert.equal(connections.size, 1);
  const connection = connections.docs[0];
  assert.equal(connection.get("memberIds").length, 2);
  assert.equal((await connection.ref.collection("members").get()).size, 2);
  const winner = first.ok ? "redeemer-b" : "redeemer-c";
  const duplicateInvite = await createDirectInvite(db, "creator-a");
  assert.equal(
    (await redeemDirectInvite(db, winner, duplicateInvite.code)).reason,
    "ALREADY_CONNECTED",
  );
  const duplicateHash = require("../lib/inviteCore").sha256(duplicateInvite.code.replace("-", ""));
  const unusedInvite = await db.collection("invites").doc(duplicateHash).get();
  assert.equal(unusedInvite.get("status"), "ACTIVE");
  assert.equal(unusedInvite.get("usedCount"), 0);

  const replacement1 = await createDirectInvite(db, "creator-d");
  const replacement2 = await createDirectInvite(db, "creator-d");
  const oldHash = require("../lib/inviteCore").sha256(replacement1.code.replace("-", ""));
  assert.equal((await db.collection("invites").doc(oldHash).get()).get("status"), "REVOKED");
  assert.equal((await revokeDirectInvite(db, "creator-d")).revoked, true);
  assert.equal((await redeemDirectInvite(db, "redeemer-e", replacement2.code)).reason, "INVALID_INVITE");

  const expired = await createDirectInvite(db, "creator-expired", Timestamp.fromMillis(0));
  assert.equal((await redeemDirectInvite(db, "redeemer-expired", expired.code)).reason, "INVALID_INVITE");

  for (let index = 0; index < 5; index += 1) {
    assert.equal((await redeemDirectInvite(db, "rate-user", "AAAA-AAAA")).reason, "INVALID_INVITE");
  }
  assert.equal((await redeemDirectInvite(db, "rate-user", "BBBB-BBBB")).reason, "RATE_LIMITED");
  const photoInput = {
    connectionId: connection.id, postId: "post-photo", mediaId: "media-photo",
    clientCreatedAt: 123456789, caption: "MVP photo",
    storagePath: `connections/${connection.id}/posts/post-photo/media-photo.jpg`,
    mimeType: "image/jpeg", width: 1920, height: 1080, sizeBytes: 2048,
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
    db, "redeemer-e", {...photoInput, postId: "not-member", storagePath:
      `connections/${connection.id}/posts/not-member/media-photo.jpg`}, async () => true,
  );
  assert.equal(rejectedMember.reason, "NOT_ACTIVE_MEMBER");
  const rejectedMedia = await finalizePhotoPost(
    db, "creator-a", {...photoInput, postId: "bad-media", storagePath:
      `connections/${connection.id}/posts/bad-media/media-photo.jpg`}, async () => false,
  );
  assert.equal(rejectedMedia.reason, "MEDIA_INVALID");
  console.log("Direct invite emulator checks passed: create, replace, revoke, self, expiry, rate limit, duplicate lock, concurrent redeem.");
  console.log("Photo finalize emulator checks passed: membership, media verification, atomic metadata, idempotent retry.");
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
