import {Firestore, Timestamp} from "firebase-admin/firestore";
import {
  DIRECT_INVITE_TTL_MS,
  MAX_REDEEM_ATTEMPTS,
  REDEEM_WINDOW_MS,
  directKeyFor,
  formatInviteCode,
  generateInviteCode,
  normalizeInviteCode,
  sha256,
} from "./inviteCore";

type RedeemFailure =
  | "INVALID_INVITE"
  | "SELF_REDEEM"
  | "ALREADY_CONNECTED"
  | "RATE_LIMITED";

export type RedeemResult =
  | {ok: true; connectionId: string}
  | {ok: false; reason: RedeemFailure};

const ACTIVE = "ACTIVE";

export async function createDirectInvite(
  db: Firestore,
  uid: string,
  now = Timestamp.now(),
): Promise<{code: string; expiresAtMillis: number}> {
  const normalizedCode = generateInviteCode();
  const codeHash = sha256(normalizedCode);
  const inviteRef = db.collection("invites").doc(codeHash);
  const ownerRef = db.collection("directInviteOwners").doc(uid);
  const expiresAt = Timestamp.fromMillis(now.toMillis() + DIRECT_INVITE_TTL_MS);

  await db.runTransaction(async (transaction) => {
    const ownerSnapshot = await transaction.get(ownerRef);
    const creatorProfile = await transaction.get(db.collection("users").doc(uid));
    const previousHash = ownerSnapshot.get("codeHash");
    const previousRef = typeof previousHash === "string" ?
      db.collection("invites").doc(previousHash) : null;
    const previousSnapshot = previousRef ? await transaction.get(previousRef) : null;
    const collisionSnapshot = await transaction.get(inviteRef);
    if (!creatorProfile.exists) throw new Error("Creator profile is missing.");
    if (collisionSnapshot.exists) throw new Error("Invite code collision.");

    if (previousRef && previousSnapshot?.get("status") === ACTIVE) {
      transaction.update(previousRef, {
        status: "REVOKED",
        revokedAt: now,
      });
    }

    transaction.create(inviteRef, {
      purpose: "DIRECT_PAIR",
      createdBy: uid,
      targetConnectionId: null,
      maxUses: 1,
      usedCount: 0,
      status: ACTIVE,
      createdAt: now,
      expiresAt,
      revokedAt: null,
      schemaVersion: 1,
    });
    transaction.set(ownerRef, {codeHash, expiresAt, updatedAt: now});
  });

  return {code: formatInviteCode(normalizedCode), expiresAtMillis: expiresAt.toMillis()};
}

export async function revokeDirectInvite(
  db: Firestore,
  uid: string,
  now = Timestamp.now(),
): Promise<{revoked: boolean}> {
  return db.runTransaction(async (transaction) => {
    const ownerRef = db.collection("directInviteOwners").doc(uid);
    const ownerSnapshot = await transaction.get(ownerRef);
    const codeHash = ownerSnapshot.get("codeHash");
    if (typeof codeHash !== "string") return {revoked: false};

    const inviteRef = db.collection("invites").doc(codeHash);
    const inviteSnapshot = await transaction.get(inviteRef);
    const owned = inviteSnapshot.exists && inviteSnapshot.get("createdBy") === uid;
    const active = owned && inviteSnapshot.get("status") === ACTIVE;
    if (active) {
      transaction.update(inviteRef, {status: "REVOKED", revokedAt: now});
    }
    transaction.delete(ownerRef);
    return {revoked: active};
  });
}

export async function redeemDirectInvite(
  db: Firestore,
  uid: string,
  rawCode: unknown,
  now = Timestamp.now(),
): Promise<RedeemResult> {
  const normalizedCode = normalizeInviteCode(rawCode);
  const codeHash = normalizedCode ? sha256(normalizedCode) : sha256("invalid");
  const inviteRef = db.collection("invites").doc(codeHash);
  const rateRef = db.collection("inviteRedeemRateLimits").doc(uid);
  const connectionRef = db.collection("connections").doc();

  return db.runTransaction(async (transaction): Promise<RedeemResult> => {
    const rateSnapshot = await transaction.get(rateRef);
    const inviteSnapshot = await transaction.get(inviteRef);
    const windowStartedAt = rateSnapshot.get("windowStartedAt") as Timestamp | undefined;
    const oldCount = Number(rateSnapshot.get("attemptCount") ?? 0);
    const insideWindow = windowStartedAt != null &&
      now.toMillis() - windowStartedAt.toMillis() < REDEEM_WINDOW_MS;
    const attemptCount = insideWindow ? oldCount + 1 : 1;
    if (insideWindow && oldCount >= MAX_REDEEM_ATTEMPTS) {
      return {ok: false, reason: "RATE_LIMITED"};
    }

    const rateData = {
      attemptCount,
      windowStartedAt: insideWindow ? windowStartedAt : now,
      updatedAt: now,
    };

    const invite = inviteSnapshot.data();
    const valid = normalizedCode != null && invite != null &&
      invite.purpose === "DIRECT_PAIR" && invite.status === ACTIVE &&
      invite.expiresAt instanceof Timestamp && invite.expiresAt.toMillis() > now.toMillis() &&
      Number(invite.usedCount) < Number(invite.maxUses);
    if (!valid) {
      transaction.set(rateRef, rateData);
      return {ok: false, reason: "INVALID_INVITE"};
    }

    const creatorUid = invite.createdBy;
    if (typeof creatorUid !== "string" || creatorUid === uid) {
      transaction.set(rateRef, rateData);
      return {ok: false, reason: creatorUid === uid ? "SELF_REDEEM" : "INVALID_INVITE"};
    }

    const [creatorProfile, redeemerProfile] = await transaction.getAll(
      db.collection("users").doc(creatorUid),
      db.collection("users").doc(uid),
    );
    if (!creatorProfile.exists || !redeemerProfile.exists) {
      transaction.set(rateRef, rateData);
      return {ok: false, reason: "INVALID_INVITE"};
    }

    const directKey = directKeyFor(creatorUid, uid);
    const lockRef = db.collection("directConnectionLocks").doc(directKey);
    const lockSnapshot = await transaction.get(lockRef);
    if (lockSnapshot.exists && lockSnapshot.get("status") === ACTIVE) {
      transaction.set(rateRef, rateData);
      return {ok: false, reason: "ALREADY_CONNECTED"};
    }

    const memberIds = [creatorUid, uid].sort();
    transaction.create(connectionRef, {
      type: "DIRECT",
      name: null,
      memberIds,
      createdBy: creatorUid,
      ownerId: null,
      maxMembers: 2,
      status: ACTIVE,
      directKey,
      lastPostAt: null,
      createdAt: now,
      updatedAt: now,
      schemaVersion: 1,
    });
    transaction.create(connectionRef.collection("members").doc(creatorUid), {
      userId: creatorUid,
      role: "MEMBER",
      status: ACTIVE,
      joinedAt: now,
      leftAt: null,
      invitedBy: null,
      removedBy: null,
    });
    transaction.create(connectionRef.collection("members").doc(uid), {
      userId: uid,
      role: "MEMBER",
      status: ACTIVE,
      joinedAt: now,
      leftAt: null,
      invitedBy: creatorUid,
      removedBy: null,
    });
    transaction.set(lockRef, {
      connectionId: connectionRef.id,
      memberIds,
      status: ACTIVE,
      createdAt: now,
      updatedAt: now,
      schemaVersion: 1,
    });
    transaction.update(inviteRef, {
      targetConnectionId: connectionRef.id,
      usedCount: Number(invite.usedCount) + 1,
      status: "USED",
    });
    transaction.delete(db.collection("directInviteOwners").doc(creatorUid));
    transaction.set(rateRef, rateData);
    return {ok: true, connectionId: connectionRef.id};
  });
}
