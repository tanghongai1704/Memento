const assert = require("node:assert/strict");
const {initializeApp} = require("firebase-admin/app");
const {getFirestore, Timestamp} = require("firebase-admin/firestore");
const {
  createDirectInvite,
  redeemDirectInvite,
  revokeDirectInvite,
} = require("../lib/inviteService");

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
  console.log("Direct invite emulator checks passed: create, replace, revoke, self, expiry, rate limit, duplicate lock, concurrent redeem.");
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
