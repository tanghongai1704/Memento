import { applicationDefault, initializeApp } from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";
import { FieldValue, getFirestore } from "firebase-admin/firestore";

const projectId = process.env.FIREBASE_PROJECT_ID;

if (!projectId) {
  throw new Error("Missing FIREBASE_PROJECT_ID environment variable.");
}

initializeApp({
  credential: applicationDefault(),
  projectId,
});

const auth = getAuth();
const db = getFirestore();
const password = "123456789a";

const users = [
  { email: "oliver17@example.com", username: "oliver17", displayName: "Oliver 17" },
  { email: "amelia24@example.com", username: "amelia24", displayName: "Amelia 24" },
  { email: "henry31@example.com", username: "henry31", displayName: "Henry 31" },
  { email: "charlotte42@example.com", username: "charlotte42", displayName: "Charlotte 42" },
  { email: "james58@example.com", username: "james58", displayName: "James 58" },
  { email: "sophia63@example.com", username: "sophia63", displayName: "Sophia 63" },
  { email: "william76@example.com", username: "william76", displayName: "William 76" },
  { email: "isabella81@example.com", username: "isabella81", displayName: "Isabella 81" },
  { email: "lucas94@example.com", username: "lucas94", displayName: "Lucas 94" },
  { email: "evelyn105@example.com", username: "evelyn105", displayName: "Evelyn 105" },
];

async function getOrCreateAuthUser(user) {
  try {
    const existing = await auth.getUserByEmail(user.email);
    await auth.updateUser(existing.uid, {
      password,
      displayName: user.displayName,
    });
    console.log(`Auth already exists: ${user.email} (${existing.uid})`);
    return existing;
  } catch (error) {
    if (error?.code !== "auth/user-not-found") throw error;
  }

  const created = await auth.createUser({
    email: user.email,
    password,
    displayName: user.displayName,
    emailVerified: false,
  });
  console.log(`Created Auth user: ${user.email} (${created.uid})`);
  return created;
}

for (const user of users) {
  const account = await getOrCreateAuthUser(user);
  const profileRef = db.collection("users").doc(account.uid);
  const existingProfile = await profileRef.get();

  await profileRef.set({
    displayName: user.displayName,
    username: user.username,
    usernameNormalized: user.username.toLowerCase(),
    avatarPath: null,
    bio: null,
    createdAt: existingProfile.get("createdAt") ?? FieldValue.serverTimestamp(),
    updatedAt: FieldValue.serverTimestamp(),
    schemaVersion: 1,
  });

  console.log(`Created profile: users/${account.uid} (${user.username})`);
}

console.log(`Done. ${users.length} test accounts are ready.`);
