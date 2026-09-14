import {initializeApp} from "firebase-admin/app";
import {getFirestore} from "firebase-admin/firestore";
import {HttpsError, onCall} from "firebase-functions/v2/https";
import {
  createDirectInvite as createDirectInviteService,
  redeemDirectInvite as redeemDirectInviteService,
  revokeDirectInvite as revokeDirectInviteService,
} from "./inviteService";

initializeApp();
const db = getFirestore();
const callableOptions = {region: "asia-southeast1", enforceAppCheck: true};

function requireUid(auth: {uid: string} | undefined): string {
  if (!auth) throw new HttpsError("unauthenticated", "Please sign in and try again.");
  return auth.uid;
}

export const createDirectInvite = onCall(callableOptions, async (request) => {
  const uid = requireUid(request.auth);
  try {
    return await createDirectInviteService(db, uid);
  } catch (error) {
    console.error("createDirectInvite failed", error);
    throw new HttpsError("internal", "Could not create an invite. Please try again.");
  }
});

export const revokeDirectInvite = onCall(callableOptions, async (request) => {
  const uid = requireUid(request.auth);
  try {
    return await revokeDirectInviteService(db, uid);
  } catch (error) {
    console.error("revokeDirectInvite failed", error);
    throw new HttpsError("internal", "Could not revoke the invite. Please try again.");
  }
});

export const redeemDirectInvite = onCall(callableOptions, async (request) => {
  const uid = requireUid(request.auth);
  let result;
  try {
    result = await redeemDirectInviteService(db, uid, request.data?.code);
  } catch (error) {
    console.error("redeemDirectInvite failed", error);
    throw new HttpsError("internal", "Could not redeem the invite. Please try again.");
  }
  if (result.ok) return result;
  if (result.reason === "RATE_LIMITED") {
    throw new HttpsError("resource-exhausted", "Too many attempts. Please wait 10 minutes.");
  }
  if (result.reason === "SELF_REDEEM") {
    throw new HttpsError("failed-precondition", "You cannot use your own invite.");
  }
  if (result.reason === "ALREADY_CONNECTED") {
    throw new HttpsError("already-exists", "You are already connected.");
  }
  throw new HttpsError("invalid-argument", "This invite is invalid or has expired.");
});
