import { createHash, randomInt } from "node:crypto";
import { applicationDefault, initializeApp } from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";
import { getFirestore, Timestamp } from "firebase-admin/firestore";

const projectId = process.env.FIREBASE_PROJECT_ID;

if (!projectId) {
  throw new Error("Missing FIREBASE_PROJECT_ID environment variable.");
}

initializeApp({ credential: applicationDefault(), projectId });

const auth = getAuth();
const db = getFirestore();
const targetEmail = "u123@example.com";
const testEmails = [
  "oliver17@example.com",
  "amelia24@example.com",
  "henry31@example.com",
  "charlotte42@example.com",
  "james58@example.com",
  "sophia63@example.com",
  "william76@example.com",
  "isabella81@example.com",
  "lucas94@example.com",
  "evelyn105@example.com",
];

const codeAlphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
const codeLength = 8;

function sha256(value) {
  return createHash("sha256").update(value, "utf8").digest("hex");
}

function directKeyFor(firstUid, secondUid) {
  return sha256(JSON.stringify([firstUid, secondUid].sort()));
}

function generateInviteCode() {
  let result = "";
  for (let index = 0; index < codeLength; index += 1) {
    result += codeAlphabet[randomInt(codeAlphabet.length)];
  }
  return result;
}

function normalizeInviteCode(value) {
  if (typeof value !== "string") return null;
  const normalized = value.trim().replaceAll("-", "").toUpperCase();
  if (normalized.length !== codeLength) return null;
  return [...normalized].every((character) => codeAlphabet.includes(character))
    ? normalized
    : null;
}

function formatInviteCode(normalized) {
  return `${normalized.slice(0, 4)}-${normalized.slice(4)}`;
}

async function ensureInviteCode(uid) {
  const ownerRef = db.collection("userInviteCodes").doc(uid);

  for (let attempt = 0; attempt < 10; attempt += 1) {
    const generatedCode = generateInviteCode();
    const codeHash = sha256(generatedCode);
    const lookupRef = db.collection("inviteCodeLookup").doc(codeHash);

    const result = await db.runTransaction(async (transaction) => {
      const ownerSnapshot = await transaction.get(ownerRef);
      const existingCode = normalizeInviteCode(ownerSnapshot.get("code"));
      if (existingCode) return formatInviteCode(existingCode);

      const [profileSnapshot, collisionSnapshot] = await transaction.getAll(
        db.collection("users").doc(uid),
        lookupRef,
      );
      if (!profileSnapshot.exists) throw new Error(`Missing profile for UID ${uid}`);
      if (collisionSnapshot.exists) return null;

      const now = Timestamp.now();
      transaction.create(lookupRef, {
        ownerUid: uid,
        createdAt: now,
        schemaVersion: 1,
      });
      transaction.create(ownerRef, {
        code: generatedCode,
        codeHash,
        createdAt: now,
        schemaVersion: 1,
      });
      return formatInviteCode(generatedCode);
    });

    if (result) return result;
  }

  throw new Error(`Could not allocate a unique invite code for UID ${uid}`);
}

async function ensureDirectConnection(creatorUid, targetUid) {
  const directKey = directKeyFor(creatorUid, targetUid);
  const lockRef = db.collection("directConnectionLocks").doc(directKey);
  const connectionRef = db.collection("connections").doc();

  return db.runTransaction(async (transaction) => {
    const lockSnapshot = await transaction.get(lockRef);
    if (lockSnapshot.exists && lockSnapshot.get("status") === "ACTIVE") {
      return { connectionId: lockSnapshot.get("connectionId"), created: false };
    }

    const memberIds = [creatorUid, targetUid].sort();
    const now = Timestamp.now();
    transaction.create(connectionRef, {
      type: "DIRECT",
      name: null,
      memberIds,
      createdBy: creatorUid,
      ownerId: null,
      maxMembers: 2,
      status: "ACTIVE",
      directKey,
      lastPostAt: null,
      createdAt: now,
      updatedAt: now,
      schemaVersion: 1,
    });
    transaction.create(connectionRef.collection("members").doc(creatorUid), {
      userId: creatorUid,
      role: "MEMBER",
      status: "ACTIVE",
      joinedAt: now,
      leftAt: null,
      invitedBy: null,
      removedBy: null,
    });
    transaction.create(connectionRef.collection("members").doc(targetUid), {
      userId: targetUid,
      role: "MEMBER",
      status: "ACTIVE",
      joinedAt: now,
      leftAt: null,
      invitedBy: creatorUid,
      removedBy: null,
    });
    transaction.set(lockRef, {
      connectionId: connectionRef.id,
      memberIds,
      status: "ACTIVE",
      createdAt: now,
      updatedAt: now,
      schemaVersion: 1,
    });
    return { connectionId: connectionRef.id, created: true };
  });
}

const targetAccount = await auth.getUserByEmail(targetEmail);
const testAccounts = await Promise.all(testEmails.map((email) => auth.getUserByEmail(email)));
const allUids = [targetAccount.uid, ...testAccounts.map((account) => account.uid)];
const profileSnapshots = await db.getAll(
  ...allUids.map((uid) => db.collection("users").doc(uid)),
);
const missingProfileUids = profileSnapshots
  .filter((snapshot) => !snapshot.exists)
  .map((snapshot) => snapshot.id);

if (missingProfileUids.length > 0) {
  throw new Error(`Missing user profiles: ${missingProfileUids.join(", ")}`);
}

const results = [];
for (let index = 0; index < testAccounts.length; index += 1) {
  const account = testAccounts[index];
  const inviteCode = await ensureInviteCode(account.uid);
  const connection = await ensureDirectConnection(account.uid, targetAccount.uid);
  results.push({
    email: testEmails[index],
    inviteCode,
    connectionId: connection.connectionId,
    status: connection.created ? "created" : "already connected",
  });
}

console.table(results);
console.log(`Done. ${results.length} users are connected to ${targetEmail}.`);
