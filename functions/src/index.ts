import {initializeApp} from "firebase-admin/app";
import {getFirestore} from "firebase-admin/firestore";
import {getStorage} from "firebase-admin/storage";
import {HttpsError, onCall} from "firebase-functions/v2/https";
import {
  createDirectInvite as createDirectInviteService,
  redeemDirectInvite as redeemDirectInviteService,
  revokeDirectInvite as revokeDirectInviteService,
} from "./inviteService";
import {parseFinalizePhotoInput} from "./postCore";
import {finalizePhotoPost as finalizePhotoPostService} from "./postService";

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

export const finalizePhotoPost = onCall(callableOptions, async (request) => {
  const uid = requireUid(request.auth);
  const input = parseFinalizePhotoInput(request.data);
  if (!input) throw new HttpsError("invalid-argument", "Invalid photo post data.");
  let result;
  try {
    result = await finalizePhotoPostService(db, uid, input, async (photo) => {
      const [metadata] = await getStorage().bucket().file(photo.storagePath).getMetadata();
      return metadata.contentType === "image/jpeg" && Number(metadata.size) === photo.sizeBytes &&
        metadata.metadata?.authorId === uid;
    });
  } catch (error) {
    console.error("finalizePhotoPost failed", error);
    throw new HttpsError("internal", "Could not publish the post. Please retry.");
  }
  if (result.ok) return result;
  if (result.reason === "NOT_ACTIVE_MEMBER") {
    throw new HttpsError("permission-denied", "This connection is no longer active.");
  }
  if (result.reason === "POST_CONFLICT") {
    throw new HttpsError("already-exists", "This post ID is already in use.");
  }
  throw new HttpsError("failed-precondition", "The uploaded photo could not be verified.");
});
