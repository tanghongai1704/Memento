import {Firestore, Timestamp} from "firebase-admin/firestore";
import {
  directKeyFor,
  formatInviteCode,
  generateInviteCode,
  normalizeInviteCode,
  sha256,
} from "./inviteCore";
import {
  ConnectionStatus,
  ConnectionType,
  MemberRole,
  MemberStatus,
} from "./schema";

type RedeemFailure =
  | "INVALID_INVITE"
  | "SELF_REDEEM";

export type RedeemResult =
  | {ok: true; connectionId: string}
  | {ok: false; reason: RedeemFailure};

export async function getMyInviteCode(
  db: Firestore,
  uid: string,
  now = Timestamp.now(),
): Promise<{code: string}> {
  const normalizedCode = generateInviteCode();
  const codeHash = sha256(normalizedCode);
  const lookupRef = db.collection("inviteCodeLookup").doc(codeHash);
  const ownerRef = db.collection("userInviteCodes").doc(uid);

  return db.runTransaction(async (transaction): Promise<{code: string}> => {
    const ownerSnapshot = await transaction.get(ownerRef);
    const creatorProfile = await transaction.get(db.collection("users").doc(uid));
    if (!creatorProfile.exists) throw new Error("Creator profile is missing.");

    const existingCode = normalizeInviteCode(ownerSnapshot.get("code"));
    if (existingCode != null) {
      return {code: formatInviteCode(existingCode)};
    }

    const collisionSnapshot = await transaction.get(lookupRef);
    if (collisionSnapshot.exists) throw new Error("Invite code collision.");
    transaction.create(lookupRef, {
      ownerUid: uid,
      createdAt: now,
      schemaVersion: 1,
    });
    transaction.create(ownerRef, {
      code: normalizedCode,
      codeHash,
      createdAt: now,
      schemaVersion: 1,
    });
    return {code: formatInviteCode(normalizedCode)};
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
  const lookupRef = db.collection("inviteCodeLookup").doc(codeHash);
  const connectionRef = db.collection("connections").doc();

  return db.runTransaction(async (transaction): Promise<RedeemResult> => {
    const lookupSnapshot = await transaction.get(lookupRef);
    const lookup = lookupSnapshot.data();
    if (normalizedCode == null || lookup == null) return {ok: false, reason: "INVALID_INVITE"};

    const creatorUid = lookup.ownerUid;
    if (typeof creatorUid !== "string" || creatorUid === uid) {
      return {ok: false, reason: creatorUid === uid ? "SELF_REDEEM" : "INVALID_INVITE"};
    }

    const [creatorProfile, redeemerProfile] = await transaction.getAll(
      db.collection("users").doc(creatorUid),
      db.collection("users").doc(uid),
    );
    if (!creatorProfile.exists || !redeemerProfile.exists) {
      return {ok: false, reason: "INVALID_INVITE"};
    }

    const directKey = directKeyFor(creatorUid, uid);
    const lockRef = db.collection("directConnectionLocks").doc(directKey);
    const lockSnapshot = await transaction.get(lockRef);
    if (lockSnapshot.exists && lockSnapshot.get("status") === ConnectionStatus.ACTIVE) {
      const connectionId = lockSnapshot.get("connectionId");
      return typeof connectionId === "string" ?
        {ok: true, connectionId} : {ok: false, reason: "INVALID_INVITE"};
    }

    const memberIds = [creatorUid, uid].sort();
    transaction.create(connectionRef, {
      type: ConnectionType.DIRECT,
      name: null,
      memberIds,
      createdBy: creatorUid,
      ownerId: null,
      maxMembers: 2,
      status: ConnectionStatus.ACTIVE,
      directKey,
      lastPostAt: null,
      createdAt: now,
      updatedAt: now,
      schemaVersion: 1,
    });
    transaction.create(connectionRef.collection("members").doc(creatorUid), {
      userId: creatorUid,
      role: MemberRole.MEMBER,
      status: MemberStatus.ACTIVE,
      joinedAt: now,
      leftAt: null,
      invitedBy: null,
      removedBy: null,
    });
    transaction.create(connectionRef.collection("members").doc(uid), {
      userId: uid,
      role: MemberRole.MEMBER,
      status: MemberStatus.ACTIVE,
      joinedAt: now,
      leftAt: null,
      invitedBy: creatorUid,
      removedBy: null,
    });
    transaction.set(lockRef, {
      connectionId: connectionRef.id,
      memberIds,
      status: ConnectionStatus.ACTIVE,
      createdAt: now,
      updatedAt: now,
      schemaVersion: 1,
    });
    return {ok: true, connectionId: connectionRef.id};
  });
}
