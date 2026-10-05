const fs = require("node:fs");
const assert = require("node:assert/strict");
const {initializeTestEnvironment, assertSucceeds, assertFails} = require("@firebase/rules-unit-testing");
const {doc, setDoc} = require("firebase/firestore");
const {ref, uploadBytes, getBytes, deleteObject} = require("firebase/storage");

async function main() {
  const projectId = "demo-memento-schema";
  const testEnv = await initializeTestEnvironment({
    projectId,
    firestore: {host: "127.0.0.1", port: 8080, rules: fs.readFileSync("../firestore.rules", "utf8")},
    storage: {host: "127.0.0.1", port: 9199, rules: fs.readFileSync("../storage.rules", "utf8")},
  });
  try {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      await setDoc(doc(context.firestore(), "connections/c-photo"), {
        status: "ACTIVE", memberIds: ["member", "member2"],
      });
      await setDoc(doc(context.firestore(), "connections/c-photo/members/member"), {status: "ACTIVE"});
      await setDoc(doc(context.firestore(), "connections/c-photo/members/member2"), {status: "ACTIVE"});
    });
    const memberStorage = testEnv.authenticatedContext("member").storage();
    const outsiderStorage = testEnv.authenticatedContext("outsider").storage();
    const otherMemberStorage = testEnv.authenticatedContext("member2").storage();
    const validPath = "connections/c-photo/posts/post-1/media-1.jpg";
    await assertSucceeds(uploadBytes(ref(memberStorage, validPath), new Uint8Array([1, 2, 3]), {
      contentType: "image/jpeg", customMetadata: {authorId: "member"},
    }));
    await assertFails(uploadBytes(
      ref(outsiderStorage, "connections/c-photo/posts/post-2/media-2.jpg"),
      new Uint8Array([1]), {contentType: "image/jpeg", customMetadata: {authorId: "outsider"}},
    ));
    await assertFails(uploadBytes(
      ref(memberStorage, "connections/c-photo/posts/post-3/media-3.png"),
      new Uint8Array([1]), {contentType: "image/jpeg", customMetadata: {authorId: "member"}},
    ));
    await assertFails(uploadBytes(
      ref(memberStorage, "connections/c-photo/posts/post-4/media-4.jpg"),
      new Uint8Array([1]), {contentType: "image/png", customMetadata: {authorId: "member"}},
    ));
    await assertFails(uploadBytes(
      ref(memberStorage, "connections/c-photo/posts/post-5/media-5.jpg"),
      new Uint8Array(5 * 1024 * 1024 + 1), {contentType: "image/jpeg", customMetadata: {authorId: "member"}},
    ));
    await assertFails(uploadBytes(ref(otherMemberStorage, validPath), new Uint8Array([4]), {
      contentType: "image/jpeg", customMetadata: {authorId: "member2"},
    }));
    const downloaded = await assertSucceeds(getBytes(ref(memberStorage, validPath)));
    assert.equal(downloaded.byteLength, 3);
    await assertFails(getBytes(ref(outsiderStorage, validPath)));
    await assertFails(deleteObject(ref(memberStorage, validPath)));
    await testEnv.withSecurityRulesDisabled(async (context) => {
      await setDoc(doc(context.firestore(), "connections/c-photo"), {
        status: "CLOSED", memberIds: [],
      });
      await setDoc(doc(context.firestore(), "connections/c-photo/members/member"), {status: "LEFT"});
      await setDoc(doc(context.firestore(), "connections/c-photo/members/member2"), {status: "LEFT"});
    });
    await assertFails(getBytes(ref(memberStorage, validPath)));
    await assertFails(uploadBytes(
      ref(memberStorage, "connections/c-photo/posts/post-6/media-6.jpg"),
      new Uint8Array([1]), {contentType: "image/jpeg", customMetadata: {authorId: "member"}},
    ));
    console.log("Storage rules: path, membership, author, MIME, size, read, delete and revoked-access checks passed.");
  } finally {
    await testEnv.cleanup();
  }
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
