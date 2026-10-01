import { applicationDefault, initializeApp } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";

const projectId = process.env.FIREBASE_PROJECT_ID;
const apply = process.argv.includes("--apply");

if (!projectId) {
  throw new Error("Missing FIREBASE_PROJECT_ID environment variable.");
}

initializeApp({ credential: applicationDefault(), projectId });
const db = getFirestore();

let scanned = 0;
let changed = 0;
let batch = db.batch();
let pendingWrites = 0;

async function flush() {
  if (!apply || pendingWrites === 0) return;
  await batch.commit();
  batch = db.batch();
  pendingWrites = 0;
}

const connections = await db.collection("connections").get();
for (const connection of connections.docs) {
  const rawMemberIds = connection.get("memberIds");
  const memberIds = connection.get("status") === "ACTIVE" && Array.isArray(rawMemberIds)
    ? rawMemberIds.filter((uid) => typeof uid === "string")
    : [];
  const posts = await connection.ref.collection("posts").get();

  for (const post of posts.docs) {
    scanned += 1;
    const existing = post.get("memberIds");
    if (JSON.stringify(existing) === JSON.stringify(memberIds)) continue;
    changed += 1;
    if (!apply) continue;
    batch.update(post.ref, { memberIds });
    pendingWrites += 1;
    if (pendingWrites === 400) await flush();
  }
}

await flush();
console.log(`${apply ? "Updated" : "Would update"} ${changed} of ${scanned} posts in ${projectId}.`);
if (!apply && changed > 0) console.log("Run again with --apply after reviewing the project and count.");
